/*
 * Copyright (c) 2002 The European Bioinformatics Institute, and others.
 * All rights reserved. Please see the file LICENSE
 * in the root directory of this distribution.
 */
package uk.ac.ebi.intact.util.protein;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.referencefilter.IntactCrossReferenceFilter;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceException;
import uk.ac.ebi.intact.util.protein.utils.*;

import java.util.*;

/**
 * TODO comment this
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08-Feb-2007</pre>
 */
public class ProteinServiceImpl implements ProteinService {

    private UniprotServiceResult uniprotServiceResult;

    public static final Log log = LogFactory.getLog( ProteinServiceImpl.class );

    // TODO Factory could coordinate a shared cache between multiple instances of the service (eg. multiple services running in threads)

    // TODO when running tests using this service, the implementation of UniprotBridgeAdapter could be DummyUniprotBridgeAdapter
    // TODO that creates proteins without relying on the network. Spring configuration might come in handy to configure the tests.

    /**
     * UniProt Data Source.
     */
    private UniprotService uniprotService;

    /**
     * BioSource service allowing to create new BioSource in the database.
     */
    private BioSourceService bioSourceService;

    /**
     * Mapping allowing to specify which database shortlabel correspond to which MI reference.
     */
    private Map<String, String> databaseName2mi = new HashMap<String, String>();

    private boolean isGlobalProteinUpdate = false;

    //////////////////////////
    // Constructor

    protected ProteinServiceImpl( UniprotService uniprotService ) {
        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
        //if ( uniprotService == null ) {
        //    throw new IllegalArgumentException( "You must give a non null implementation of a UniProt Service." );
        //}
        this.uniprotService = uniprotService;
    }

    /////////////////////////
    // Getters and Setters

    public BioSourceService getBioSourceService() {
        return bioSourceService;
    }

    public void setBioSourceService( BioSourceService bioSourceService ) {
        if ( bioSourceService == null ) {
            throw new IllegalArgumentException( "bioSourceService must not be null." );
        }
        this.bioSourceService = bioSourceService;
    }

    public UniprotService getUniprotService() {
        return uniprotService;
    }

    public void setUniprotService( UniprotService uniprotService ) {
        if ( uniprotService == null ) {
            throw new NullPointerException( "uniprotService must not be null." );
        }
        this.uniprotService = uniprotService;
    }

    public boolean isGlobalProteinUpdate() {
        return isGlobalProteinUpdate;
    }

    public void setGlobalProteinUpdate( boolean globalProteinUpdate ) {
        isGlobalProteinUpdate = globalProteinUpdate;
    }

    //////////////////////////
    // ProteinLoaderService

    public UniprotServiceResult retrieve( String uniprotAc ) {
        if ( uniprotAc == null ) {
            throw new IllegalArgumentException( "You must give a non null UniProt AC" );
        }

        uniprotAc = uniprotAc.trim();

        if ( uniprotAc.length() == 0 ) {
            throw new IllegalArgumentException( "You must give a non empty UniProt AC" );
        }
        // Instanciate the uniprotServiceResult that is going to hold the proteins collection, the information messages
        // and the error message.
        uniprotServiceResult = new UniprotServiceResult(uniprotAc);

        Collection<UniprotProtein> uniprotProteins = retrieveFromUniprot( uniprotAc );

        try{
            if(uniprotProteins.size() == 0){
                ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
                List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotAc);
                if(proteinsInIntact.size() != 0){
                    uniprotServiceResult.addError("Couldn't update protein with uniprot id = " + uniprotAc + ". It was found" +
                            " in IntAct but was not found in Uniprot.", UniprotServiceResult.PROTEIN_FOUND_IN_INTACT_BUT_NOT_IN_UNIPROT_ERROR_TYPE);
                    return uniprotServiceResult;
                }else{
                    uniprotServiceResult.addError("Could not udpate protein with uniprot id = " + uniprotAc + ". No " +
                            "corresponding entry found in uniprot.", UniprotServiceResult.PROTEIN_NOT_IN_INTACT_NOT_IN_UNIPROT_ERROR_TYPE);
                }
            }else if ( uniprotProteins.size() > 1 ) {
                if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                    // If a uniprot ac we have in Intact as identity xref in IntAct, now corresponds to 2 or more proteins
                    // in uniprot we should not update it automatically but send a message to the curators so that they
                    // choose manually which of the new uniprot ac is relevant.
                    uniprotServiceResult.addError("Trying to update " + uniprotServiceResult.getQuerySentToService()
                            + " returned a set of proteins belonging to the same organism.",UniprotServiceResult.SEVERAL_PROT_BELONGING_TO_SAME_ORGA_ERROR_TYPE);
                } else {
                    // Send an error message because this should just not happen anymore in IntAct at all. In IntAct, all
                    // the demerged has taken care of the demerged proteins have been dealt with and replaced manually by
                    // the correct uniprot protein.
                    // Ex of demerged protein :P00001 was standing for the Cytochrome c of the human and the chimpanzee.
                    // It has now been demerged in one entry for the human P99998 and one for the chimpanzee P99999.
                    uniprotServiceResult.addError("Trying to update " + uniprotServiceResult.getQuerySentToService()
                            + " returned a set of proteins belonging to different organisms.", UniprotServiceResult.SEVERAL_PROT_BELONGING_TO_DIFFERENT_ORGA_ERROR_TYPE);
                }
            } else {
                createOrUpdate( uniprotProteins );
            }
        } catch (ProteinServiceException e){

            uniprotServiceResult.addException(e);
            uniprotServiceResult.addError(e.getMessage(), UniprotServiceResult.UNEXPECTED_EXCEPTION_ERROR_TYPE);
        }

        return uniprotServiceResult;
    }

    /**
     * Count the number of differents species the proteins are spread on and return the count.
     * @param proteins a Collection of Uniprot Proteins
     * @return an int representing the number of different species found.
     */
    private int getSpeciesCount( Collection<UniprotProtein> proteins ) {

        if(proteins == null){
            throw new IllegalArgumentException("The proteins collection should not be null");
        }
        if(proteins.size() == 0){
            throw new IllegalArgumentException("The proteins collection should not be empty");
        }

        Collection<Integer> species = new ArrayList<Integer>( proteins.size() );
        for ( UniprotProtein protein : proteins ) {
            int taxid = protein.getOrganism().getTaxid();
            if(!species.contains(taxid)){
                species.add( taxid );
            }
        }

        return species.size();
    }

    ///////////////////////////
    // Private methods

    /**
     * Create or update a protein.
     *
     * @param uniprotProtein the UniProt protein we want to create in IntAct.
     *
     * @return an up-to-date IntAct protein.
     *
     * @throws ProteinServiceException
     */
    protected Collection<Protein> createOrUpdate( UniprotProtein uniprotProtein ) throws ProteinServiceException {
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

        Collection<Protein> nonUniprotProteins = new ArrayList<Protein>();

        final String uniprotAc = uniprotProtein.getPrimaryAc();
        String taxid = String.valueOf( uniprotProtein.getOrganism().getTaxid() );

        if (log.isDebugEnabled()) log.debug("Searching IntAct for Uniprot protein: "+ uniprotAc + ", "
                + uniprotProtein.getOrganism().getName() +" ("+uniprotProtein.getOrganism().getTaxid()+")");

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinImpl> primaryProteins = proteinDao.getByUniprotId(uniprotAc);
        Collection<ProteinImpl> secondaryProteins = new ArrayList<ProteinImpl>();

        for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
            secondaryProteins.addAll(proteinDao.getByUniprotId(secondaryAc));
        }

        // filter and remove non-uniprot prots from the list, and assign to the primary or secondary collections

        if (taxid != null) {
            filterNonUniprot(nonUniprotProteins, primaryProteins);
            filterNonUniprot(nonUniprotProteins, secondaryProteins);
        }

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotAc);
        // TODO returned proteins are not used here
        processCase(uniprotProtein, primaryProteins, secondaryProteins);
        uniprotServiceResult.addAllToProteins(nonUniprotProteins);

        return uniprotServiceResult.getProteins();
    }

    protected Collection<Protein> processCase(UniprotProtein uniprotProtein, Collection<ProteinImpl> primaryProteins, Collection<ProteinImpl> secondaryProteins) throws ProteinServiceException {
        Collection<Protein> proteins = new ArrayList<Protein>();
        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if ( countPrimary == 0 && countSecondary == 0 ) {
            if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
            Protein protein = createMinimalisticProtein( uniprotProtein );
            proteins.add( protein );
            proteins.addAll(updateProtein( protein, uniprotProtein ));

            proteinCreated(protein);

        } else if ( countPrimary == 0 && countSecondary == 1 ) {
            if (log.isDebugEnabled())
                log.debug( "Found a single IntAct protein by UniProt secondary AC (hint: could be a TrEMBL moved to SP)." );

            Protein protein = secondaryProteins.iterator().next();
            proteins.add( protein );

            // update UniProt Xrefs
            XrefUpdaterReport xrefReport = XrefUpdaterUtils.updateUniprotXrefs( protein, uniprotProtein );

            if (xrefReport.isUpdated()) {
                uniprotServiceResult.addXrefUpdaterReport(xrefReport);
            }

            // Update protein
            proteins.addAll(updateProtein( protein, uniprotProtein ));

        } else if ( countPrimary == 1 && countSecondary == 0 ) {
            if (log.isDebugEnabled())
                log.debug( "Found in Intact one protein with primaryAc and 0 with secondaryAc." );

            Protein protein = primaryProteins.iterator().next();
            proteins.add(protein);

            proteins.addAll(updateProtein( protein, uniprotProtein ));

        }else if ( countPrimary == 1 && countSecondary >= 1){
            if (log.isDebugEnabled())
                log.debug("Found in IntAct 1 protein with primaryAc and 1 or more protein on with secondaryAc.");

            processDuplication(uniprotProtein, primaryProteins, secondaryProteins);

        }else {

            // Error cases

            String pCount = "Count of protein in Intact for the Uniprot entry primary ac(" + countPrimary + ") for the Uniprot entry secondary ac(s)(" + countSecondary + ")";
            log.error( "Could not update that protein, number of protein found in IntAct: " + pCount );

            if ( countPrimary > 1 && countSecondary == 0 ) {
                //corresponding test : testRetrieve_primaryCount2_secondaryCount1()
                uniprotServiceResult.addError("Duplication", UniprotServiceResult.MORE_THAN_1_PROT_MATCHING_UNIPROT_PRIMARY_AC_ERROR_TYPE);

                Protein updatedProt = processDuplication(uniprotProtein, primaryProteins, Collections.EMPTY_LIST);
                proteins.add(updatedProt);


            } else if ( countPrimary == 0 && countSecondary > 1 ) {
                // corresponding test ProteinServiceImplTest.testRetrieve_primaryCount0_secondaryCount2()
                uniprotServiceResult.addError("Possible duplication", UniprotServiceResult.MORE_THAN_1_PROT_MATCHING_UNIPROT_SECONDARY_AC_ERROR_TYPE);
            } else {

                // corresponding test ProteinServiceImplTest.testRetrieve_primaryCount1_secondaryCount1()
                uniprotServiceResult.addError( "Unexpected number of proteins found in IntAct for UniprotEntry("+ uniprotProtein.getPrimaryAc() + ") " + pCount + ", " +
                        "Please fix this problem manually.", UniprotServiceResult.UNEXPECTED_NUMBER_OF_INTACT_PROT_FOUND_ERROR_TYPE);

            }
        }

        uniprotServiceResult.addAllToProteins(proteins);

        return proteins;
    }

    /**
     * Note that the subclass is used in the global protein update and this one only in the editor.
     *
     */
    protected Protein processDuplication(UniprotProtein uniprotProtein, Collection<ProteinImpl> primaryProteins, Collection<ProteinImpl> secondaryProteins) throws ProteinServiceException {
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

        Protein primaryProt = primaryProteins.iterator().next();

        Protein protToBeKept = getProtWithMaxInteraction(primaryProteins.iterator().next(),secondaryProteins);

        List<Protein> proteinsToDelete = new ArrayList<Protein>();
        proteinsToDelete.addAll(secondaryProteins);
        proteinsToDelete.add(primaryProt);
        proteinsToDelete.remove(protToBeKept);

        ProteinTools.moveInteractionsBetweenProteins(protToBeKept, proteinsToDelete);

        CvXrefQualifier intactSecondary = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao().getByShortLabel(CvXrefQualifier.class,"intact-secondary");

        //            CvXrefQualifier intactSecondary = IntactContext.getCurrentInstance().getCvContext().getByLabel(CvXrefQualifier.class,"intact-secondary");
        Institution owner = IntactContext.getCurrentInstance().getInstitution();
        // TODO make sure we are using the owner of the protein, not directly IntAct
        CvDatabase intact = IntactContext.getCurrentInstance().getDataContext().getDaoFactory()
                .getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);

        for(Protein protToDelete : proteinsToDelete ){

            // On the protein that we are going to keep add the protein ac of the one we are going to delete as
            // intact-secondary xref. That way, a user can still search for the protein ac he used to search with
            // even if it has been deleted.
            InteractorXref xref = new InteractorXref(owner,intact, protToDelete.getAc(), intactSecondary);
            xref.setParent(protToBeKept);
            protToBeKept.addXref(xref);
            protToDelete.getActiveInstances().clear();

            // TODO harmonise with DuplicateFixer
            deleteProtein(protToDelete);
        }

        proteinDao.saveOrUpdate((ProteinImpl) protToBeKept);
        updateProtein( protToBeKept, uniprotProtein );
        // Message being added.

        uniprotServiceResult.addMessage("Duplication found");

        return protToBeKept;
    }

    private void filterNonUniprot(Collection<Protein> nonUniprotProteins, Collection<ProteinImpl> primaryProteins) {
        for (Iterator<ProteinImpl> proteinIterator = primaryProteins.iterator(); proteinIterator.hasNext();) {
            ProteinImpl protein = proteinIterator.next();

            if (!ProteinUtils.isFromUniprot(protein)) {
                proteinIterator.remove();
                nonUniprotProteins.add(protein);
            }
        }
    }

    /**
     * Given the protein "protein" it will add to it an annotation with cvTopic to-delete.
     * This is done in case the protein update would fail before the having deleted the protein it need to delete.
     * In this case we can still delete the protein by had afterwards.
     * @param protein
     */
    public void addToDeleteAnnotation(Protein protein) {
        Institution institution = IntactContext.getCurrentInstance().getInstitution();

        CvTopic toDelete = IntactContext.getCurrentInstance().getDataContext().getDaoFactory()
                .getCvObjectDao(CvTopic.class).getByShortLabel("to-delete", false);
        if(toDelete == null){
            toDelete = CvObjectUtils.createCvObject(institution, CvTopic.class, null, "to-delete");
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(toDelete);
        }

        Annotation annot = new Annotation(institution, toDelete, "ProteinUpdateMessage : this protein should be deleted " +
                " as it is not reflecting what is not in UniprotKB and is not involved in any interactions.");
        //AnnotationDao annotationDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getAnnotationDao();
        //annotationDao.saveOrUpdate(annot);
        protein.addAnnotation(annot);
    }

    /**
     * This will search all the active instances of the proteins contained in the proteins collection and set their
     * interactor to the replacer. It will save each active instance, each proteins of the proteins collection and
     * the replacer to the database.
     * In other words if the collection of proteins contains (EBI-1,EBI-2,EBI-3) and the replacer is EBI-4, this will
     * remove EBI-1, EBI-2, EBI-3 of all the interactions they might partipate in and will replace them by EBI-4. 
     * @param proteins
     * @param replacer
     */
    public void replaceInActiveInstances(List<Protein> proteins, Protein replacer){
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

        // Put the list in the vector to avoid ConcurrentModificationException
        Protein[] proteinArray = new Protein[proteins.size()];
        int c = 0;
        for(Protein protein : proteins){
            proteinArray[c] = protein;
            c++;
        }

        for (int i=0; i<proteinArray.length; i++){
            Protein protein =  proteinArray[i];
            Collection<Component> components = protein.getActiveInstances();
            Component[] componentArray = new Component[components.size()];
            int c2 = 0;
            for(Component component : components){
                componentArray[c2] = component;
                c2++;
            }

            for (int j=0; j < componentArray.length; j++) {
                Component component =  componentArray[j];
                replacer.addActiveInstance(component);
                //This will set the interactor of the component to null so you have after to set the interactor again.
                protein.removeActiveInstance(component);
                component.setInteractor(replacer);
                //componentDao.saveOrUpdate(component);
            }
            proteinDao.saveOrUpdate((ProteinImpl) protein);
        }
        proteinDao.saveOrUpdate((ProteinImpl) replacer);
    }

    /**
     * Given a protein and a collection of proteins it will return the protein that appear in the higher number of
     * interactions.
     * @param protein (if null return an IllegalArgumentException)
     * @param proteins (if null return an IllegalArgumentException)
     * @return a protein
     */
    private Protein getProtWithMaxInteraction(Protein protein, Collection<? extends Protein> proteins){
        if(protein == null){
            throw new IllegalArgumentException("The protein argument shouldn't be null.");
        }
        if(proteins == null){
            throw new IllegalArgumentException("The proteins collection argument shouldn't be null.");
        }
        Protein protWithMaxInteraction = protein;
        int numberOfInteraction = protein.getActiveInstances().size();
        for(Protein prot : proteins){
            if(prot.getActiveInstances().size() > numberOfInteraction){
                numberOfInteraction = prot.getActiveInstances().size();
                protWithMaxInteraction = prot;
            }
        }
        return protWithMaxInteraction;
    }

    /**
     * Create or update a collection of proteins.
     *
     * @param uniprotProtein the collection of UniProt protein we want to create in IntAct.
     *
     * @return a Collection of up-to-date IntAct proteins.
     *
     * @throws ProteinServiceException
     */
    private Collection<Protein> createOrUpdate( Collection<UniprotProtein> uniprotProtein ) throws ProteinServiceException {
        // TODO Collection or Set ???
        Collection<Protein> proteins = new ArrayList<Protein>( uniprotProtein.size() );

        for ( UniprotProtein protein : uniprotProtein ) {
            proteins.addAll( createOrUpdate( protein ) );
        }

        return proteins;
    }

    /**
     * Update an existing intact protein's annotations.
     * <p/>
     * That includes, all Xrefs, Aliases, splice variants.
     *
     * @param protein        the intact protein to update.
     * @param uniprotProtein the uniprot protein used for data input.
     */
    private Collection<Protein> updateProtein( Protein protein, UniprotProtein uniprotProtein ) throws ProteinServiceException {
        List<Protein> proteins = new ArrayList<Protein>();

        // check that both protein carry the same organism information
        BioSource organism1 = protein.getBioSource();

        Organism organism2 = uniprotProtein.getOrganism();
        int t2 = organism2.getTaxid();

        if (organism1 == null) {
            if (log.isWarnEnabled()) log.warn("Protein does not contain biosource. It will be assigned the Biosource from uniprot: "+organism2.getName()+" ("+organism2.getTaxid()+")");
            organism1 = new BioSource(protein.getOwner(), organism2.getName(), String.valueOf(t2));
            protein.setBioSource(organism1);

            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(organism1);
        }

        if ( organism1 != null && !String.valueOf( t2 ).equals( organism1.getTaxId() ) ) {
            uniprotServiceResult.addError(UniprotServiceResult.BIOSOURCE_MISMATCH, "UpdateProteins is trying to modify" +
                    " the BioSource(" + organism1.getTaxId() + "," + organism1.getShortLabel() +  ") of the following protein " +
                    getProteinDescription(protein) + " by BioSource( " + t2 + "," +
                    organism2.getName() + " ). Changing the organism of an existing protein is a forbidden operation.");

            return proteins;
        }

        // Fullname
        String fullname = uniprotProtein.getDescription();
        if ( fullname != null && fullname.length() > 250 ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Truncating fullname to the first 250 first chars." );
            }
            fullname = fullname.substring( 0, 250 );
        }
        protein.setFullName( fullname );

        // Shortlabel
        protein.setShortLabel( generateProteinShortlabel( uniprotProtein ) );

        // Xrefs -- but UniProt's as they are supposed to be up-to-date at this stage.
        XrefUpdaterReport report = XrefUpdaterUtils.updateAllXrefs( protein, uniprotProtein, databaseName2mi );

        if (report.isUpdated()) {
            uniprotServiceResult.addXrefUpdaterReport(report);
        }

        // Aliases
        AliasUpdaterUtils.updateAllAliases( protein, uniprotProtein );

        // Sequence
        boolean sequenceUpdated = false;
        String oldSequence = protein.getSequence();
        String sequence = uniprotProtein.getSequence();
        if ( sequence == null || !sequence.equals( oldSequence ) ) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Sequence requires update." );
            }
            protein.setSequence( sequence );
            sequenceUpdated = true;
        }

        // CRC64
        String crc64 = uniprotProtein.getCrc64();
        if ( protein.getCrc64() == null || !protein.getCrc64().equals( crc64 ) ) {
            log.debug( "CRC64 requires update." );
            protein.setCrc64( crc64 );
        }

        if ( sequenceUpdated && !protein.getActiveInstances().isEmpty() ) {
            sequenceChanged(protein, oldSequence);
        }

        // Persist changes
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.update( ( ProteinImpl ) protein );

        ///////////////////////////////
        // Update Splice Variants and feature chains

        // search intact
        // splice variants
        Collection<ProteinImpl> variants = pdao.getSpliceVariants( protein );
        // feature chains
        variants.addAll(pdao.getProteinChains( protein ));

        // We create a copy of the collection that hold the protein transcripts as the findMatches remove the protein transcripts
        // from the collection when a match is found. Therefore the first time it runs, it finds the match, protein transcripts
        //  are correctly created, the protein transcripts are deleted from the collection so that the second
        // you run it, the splice variant are not linked anymore to the uniprotProtein and therefore they are not correctly
        // updated.
        Collection<UniprotProteinTranscript> variantsClone = new ArrayList<UniprotProteinTranscript>();

        variantsClone.addAll(uniprotProtein.getSpliceVariants());
        variantsClone.addAll(uniprotProtein.getFeatureChains());

//        Collection<ProteinTranscriptMatch> matches = findMatches( variants, variantsClone) );
        Collection<ProteinTranscriptMatch> matches = findMatches( variants, variantsClone );
        for ( ProteinTranscriptMatch match : matches ) {

            if ( match.isSuccessful() ) {
                // update
                final UniprotProteinTranscript variant = match.getUniprotTranscript();
                final Protein intactProtein = match.getIntactProtein();
                updateProteinTranscript(intactProtein, protein, variant, uniprotProtein );

                if (variant.getSequence() != null || (variant.getSequence() == null && variant.isNullSequenceAllowed())) {
                    proteins.add(intactProtein);
                }

            } else if ( match.hasNoIntact() ) {

                // TODO in the case of a global update, and the user requested splice variants without interactions to be deleted,
                // TODO we don't create splice variants when they are missing as they wouldn't have interactions anyways.
                // NOTE: this does not apply say in our curation environment as the users want to see imported SV so they can choose them
                // TODO test this
                final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
                final boolean globalProteinUpdate = config.isGlobalProteinUpdate();
                final boolean deleteProteinTranscript = config.isDeleteProteinTranscriptWithoutInteractions();

                if( ! globalProteinUpdate && !deleteProteinTranscript) {
                    // create shallow
                    Protein intactTranscript = createMinimalisticProteinTranscript( match.getUniprotTranscript(),
                            protein,
                            uniprotProtein );
                    // update
                    final UniprotProteinTranscript uniprotTranscript = match.getUniprotTranscript();
                    updateProteinTranscript( intactTranscript, protein, uniprotTranscript, uniprotProtein);

                    proteinCreated(intactTranscript);

                    if (uniprotTranscript.getSequence() != null || (uniprotTranscript.getSequence() == null && uniprotTranscript.isNullSequenceAllowed())) {
                        proteins.add(intactTranscript);
                    }
                }

            } else {
                Protein intactProteinTranscript = match.getIntactProtein();

                if(intactProteinTranscript.getActiveInstances().size() == 0){
                    deleteProtein(intactProteinTranscript);

                    uniprotServiceResult.addMessage("The protein " + getProteinDescription(intactProteinTranscript) +
                            " is a protein transcript of " + getProteinDescription(protein) + " in IntAct but not in Uniprot." +
                            " As it is not part of any interactions in IntAct we have deleted it."  );

                }else{
                    uniprotServiceResult.addError(UniprotServiceResult.SPLICE_VARIANT_IN_INTACT_BUT_NOT_IN_UNIPROT,
                            "In Intact the protein "+ getProteinDescription(intactProteinTranscript) +
                                    " is a protein transcript of protein "+ getProteinDescription(protein)+
                                    " but in Uniprot it is not the case. As it is part of interactions in IntAct we couldn't " +
                                    "delete it.");
                }
            }
        }
        return proteins;
    }

    protected void proteinCreated(Protein protein) {
        // nothing
    }

    protected void sequenceChanged(Protein protein, String oldSequence) {
        if ( log.isDebugEnabled() ) {
            log.debug( "Request a Range check on Protein " + protein.getShortLabel() + " " + protein.getAc() );
        }
    }

    protected void deleteProtein(Protein protein) {
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        addToDeleteAnnotation(protein);
        proteinDao.saveOrUpdate((ProteinImpl) protein);

        ProteinToDeleteManager.addProteinAc(protein.getAc());
    }

    private String getProteinDescription(Protein protein){
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        return "[" + protein.getAc() + ","+ protein.getShortLabel() + "," + uniprotXref.getPrimaryId() + "]";
    }

    /**
     * Compares the two given collections and try to associate intact proteins to uniprot splice variants based on their AC.
     *
     * @param intactProteins        collection of intact protein (splice variant).
     * @param uniprotProteinTranscripts collection of uniprot splice variants and feature chains.
     *
     * @return a non null collection of matches.
     *
     * @throws ProteinServiceException when an intact protein doesn't have an identity.
     */
    private Collection<ProteinTranscriptMatch> findMatches( Collection<ProteinImpl> intactProteins,
                                                            Collection<UniprotProteinTranscript> uniprotProteinTranscripts
    ) throws ProteinServiceException {

        int max = intactProteins.size() + uniprotProteinTranscripts.size();
        Collection<ProteinTranscriptMatch> matches = new ArrayList<ProteinTranscriptMatch>( max );

        // copy the intact collection
        Collection<Protein> proteins = new ArrayList<Protein>( intactProteins );

        for ( Iterator<UniprotProteinTranscript> itsv = uniprotProteinTranscripts.iterator(); itsv.hasNext(); ) {
            UniprotProteinTranscript usv = itsv.next();

            boolean found = false;
            for ( Iterator<Protein> itp = proteins.iterator(); itp.hasNext() && false == found; ) {
                Protein protein = itp.next();
                Collection<InteractorXref> xrefs = AnnotatedObjectUtils.searchXrefs( protein, CvDatabase.UNIPROT_MI_REF, CvXrefQualifier.IDENTITY_MI_REF );
                String upac = null;
                if ( xrefs.size() == 1 ) {
                    upac = xrefs.iterator().next().getPrimaryId();
                } else {
                    if(protein.getActiveInstances().size() == 0){
                        deleteProtein(protein);

                        uniprotServiceResult.addMessage("The protein " + getProteinDescription(protein) +
                                " is a protein transcript which had multiple or no identity, as it is not involved in any interaction" +
                                " we deleted it.");
                    } else {
                        if (xrefs.size() > 1){
                            uniprotServiceResult.addError(UniprotServiceResult.SPLICE_VARIANT_WITH_MULTIPLE_IDENTITY,
                                    "Found " + xrefs.size() + "identities to UniProt for protein transcript: "
                                            + protein.getAc() + ". Couldn't be deleted as it is involved in interaction(s)" );
                        } else if (xrefs.size() == 0){
                            uniprotServiceResult.addError(UniprotServiceResult.SPLICE_VARIANT_WITH_NO_IDENTITY,
                                    "Could not find a UniProt identity for protein transcript: " + protein.getAc() +
                                            ". Couldn't be deleted as it is involved in interaction(s)" );
                        }
                    }
                    continue;
                }

                if ( usv.getPrimaryAc().equals( upac ) ) {
                    // found it
                    matches.add( new ProteinTranscriptMatch( protein, usv ) );
                    itp.remove(); // that protein was matched !
                    itsv.remove(); // that splice variant was matched !
                    found = true;
                }

                if ( !found ) {
                    // Search by secondary AC
                    for ( Iterator<String> iterator = usv.getSecondaryAcs().iterator(); iterator.hasNext() && false == found; )
                    {
                        String ac = iterator.next();

                        if ( ac.equals( upac ) ) {
                            // found it
                            matches.add( new ProteinTranscriptMatch( protein, usv ) );
                            itp.remove(); // that protein was matched !
                            itsv.remove(); // that splice variant was matched !
                            found = true;
                        }
                    }
                }
            } // for - intact proteins

            if ( !found ) {
                matches.add( new ProteinTranscriptMatch( null, usv ) ); // no mapping found
            }

        } // for - uniprot transcripts

        for ( Protein protein : proteins ) {
            matches.add( new ProteinTranscriptMatch( protein, null ) ); // no mapping found
        }

        return matches;
    }

    /**
     * Update an existing splice variant.
     *
     * @param transcript
     * @param uniprotTranscript
     */
    private void updateProteinTranscript( Protein transcript, Protein master,
                                          UniprotProteinTranscript uniprotTranscript,
                                          UniprotProtein uniprotProtein ) throws ProteinServiceException {

        transcript.setShortLabel( uniprotTranscript.getPrimaryAc() );

        // we have a feature chain
        if (uniprotTranscript.getDescription() != null){
            transcript.setFullName(uniprotTranscript.getDescription());
        }
        // we have a splice variant
        else {
            transcript.setFullName( master.getFullName() );
        }

        boolean sequenceUpdated = false;
        final String oldSequence = transcript.getSequence();

        if ( uniprotTranscript.getSequence() == null && !uniprotTranscript.isNullSequenceAllowed()) {
            if ( log.isDebugEnabled() ) {
                log.error( "Splice variant " + uniprotTranscript.getPrimaryAc() + " has no sequence" );
            }
        } else {
            if ( !uniprotTranscript.getSequence().equals(oldSequence) ) {
                transcript.setSequence( uniprotTranscript.getSequence() );
                sequenceUpdated = true;
            }

            transcript.setCrc64( Crc64.getCrc64(uniprotTranscript.getSequence()) );
        }

        // Add IntAct Xref

        // update UniProt Xrefs
        XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( transcript, uniprotTranscript, uniprotProtein );

        // Update Aliases from the uniprot protein aliases
        AliasUpdaterUtils.updateAllAliases( transcript, uniprotTranscript, uniprotProtein );

        if (sequenceUpdated) {
            sequenceChanged(transcript, oldSequence);
        }

        // Update Note
        String note = uniprotTranscript.getNote();
        if ( ( note != null ) && ( !note.trim().equals( "" ) ) ) {
            Institution owner = IntactContext.getCurrentInstance().getInstitution();
            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            CvObjectDao<CvTopic> cvDao = daoFactory.getCvObjectDao( CvTopic.class );
            CvTopic comment = cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

            if (comment == null) {
                throw new IllegalStateException("No CvTopic found with shortlabel: "+ CvTopic.ISOFORM_COMMENT);
            }

            Annotation annotation = new Annotation( owner, comment );
            annotation.setAnnotationText( note );
            AnnotationUpdaterUtils.addNewAnnotation( transcript, annotation );
        }
    }

    /**
     * Create a simple splice variant or feature chain in view of updating it.
     * <p/>
     * It should contain the following elements: Shorltabel, Biosource and UniProt Xrefs.
     *
     * @param uniprotProteinTranscript the Uniprot transcript we are going to build the intact on from.
     *
     * @return a non null, persisted intact protein.
     */
    private Protein createMinimalisticProteinTranscript( UniprotProteinTranscript uniprotProteinTranscript,
                                                         Protein master,
                                                         UniprotProtein uniprotProtein
    ) {

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();

        Protein variant = new ProteinImpl( CvHelper.getInstitution(),
                master.getBioSource(),
                uniprotProteinTranscript.getPrimaryAc(),
                CvHelper.getProteinType() );

        if (uniprotProteinTranscript.getSequence() != null) {
            variant.setSequence(uniprotProteinTranscript.getSequence());
            variant.setCrc64(Crc64.getCrc64(variant.getSequence()));
        } else if (!uniprotProteinTranscript.isNullSequenceAllowed()){
            log.warn("Uniprot splice variant without sequence: "+variant);
        }

        pdao.persist( ( ProteinImpl ) variant );

        // Create isoform-parent or chain-parent Xref
        CvXrefQualifier isoformParent = CvHelper.getQualifierByMi( uniprotProteinTranscript.getParentXRefQualifier() );
        CvDatabase intact = CvHelper.getDatabaseByMi( CvDatabase.INTACT_MI_REF );
        InteractorXref xref = new InteractorXref( CvHelper.getInstitution(), intact, master.getAc(), isoformParent );
        variant.addXref( xref );
        XrefDao xdao = daoFactory.getXrefDao();
        xdao.persist( xref );

        // Create UniProt Xrefs
        XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( variant, uniprotProteinTranscript, uniprotProtein );

        pdao.update( ( ProteinImpl ) variant );

        return variant;
    }

    /**
     * Create a simple protein in view of updating it.
     * <p/>
     * It should contain the following elements: Shortlabel, Biosource and UniProt Xrefs.
     *
     * @param uniprotProtein the Uniprot protein we are going to build the intact on from.
     *
     * @return a non null, persisted intact protein.
     */
    private Protein createMinimalisticProtein( UniprotProtein uniprotProtein ) throws ProteinServiceException {
        try{
            if (uniprotProtein == null) {
                throw new NullPointerException("Passed a null UniprotProtein");
            }

            if (bioSourceService == null) {
                throw new IllegalStateException("BioSourceService should not be null");
            }

            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            ProteinDao pdao = daoFactory.getProteinDao();

            if (uniprotProtein.getOrganism() == null) {
                throw new IllegalStateException("Uniprot protein without organism: "+uniprotProtein);
            }

            BioSource biosource = null;
            try {
                biosource = bioSourceService.getBiosourceByTaxid( String.valueOf( uniprotProtein.getOrganism().getTaxid() ) );
            } catch ( BioSourceServiceException e ) {
                throw new ProteinServiceException(e);
            }

            TransactionStatus transactionStatus = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

            Protein protein = new ProteinImpl( CvHelper.getInstitution(),
                    biosource,
                    generateProteinShortlabel( uniprotProtein ),
                    CvHelper.getProteinType() );

            pdao.persist( ( ProteinImpl ) protein );

            // Create UniProt Xrefs
            XrefUpdaterUtils.updateUniprotXrefs( protein, uniprotProtein );

            pdao.update( ( ProteinImpl ) protein );
            IntactContext.getCurrentInstance().getDataContext().commitTransaction(transactionStatus);
            return protein;

        }catch( IntactTransactionException e){
            throw new ProteinServiceException(e);
        }

    }

    private Collection<UniprotProtein> retrieveFromUniprot( String uniprotId ) {
        return uniprotService.retrieve( uniprotId );
    }

    private String generateProteinShortlabel( UniprotProtein uniprotProtein ) {

        String name = null;

        if ( uniprotProtein == null ) {
            throw new NullPointerException( "uniprotProtein must not be null." );
        }

        name = uniprotProtein.getId();

        return name.toLowerCase();
    }

    protected UniprotServiceResult getUniprotServiceResult() {
        return uniprotServiceResult;
    }
}