package uk.ac.ebi.intact.util;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.taxonomy.UniprotTaxonomyService;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.prot.referencefilter.IntactCrossReferenceFilter;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinLike;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotXref;
import uk.ac.ebi.intact.uniprot.service.SimpleUniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceException;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceImpl;
import uk.ac.ebi.intact.util.protein.CvHelper;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.intact.util.protein.utils.AliasUpdaterUtils;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Default implementation of proteinService to create an intact protein from uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10/06/11</pre>
 */

public class ProteinServiceImpl implements ProteinService{

    /**
     * Sets up a logger for that class.
     */
    public static final Log log = LogFactory.getLog( ProteinServiceImpl.class );
    private UniprotService uniprotService;
    private BioSourceService biosourceService;

    /**
     * Mapping allowing to specify which database shortlabel correspond to which MI reference.
     */
    private Map<String, String> databaseName2mi = new HashMap<String, String>();

    public ProteinServiceImpl(){
        this.uniprotService = new SimpleUniprotRemoteService();
        this.biosourceService = new BioSourceServiceImpl(new UniprotTaxonomyService());

        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
    }

    public ProteinServiceImpl(UniprotService service){
        this.biosourceService = new BioSourceServiceImpl(new UniprotTaxonomyService());

        if (service != null){
            this.uniprotService = service;
        }
        else {
            log.warn("No uniprot service is given, a default remote service will be used");
            this.uniprotService = new SimpleUniprotRemoteService();
        }

        if (biosourceService != null){
            this.biosourceService = biosourceService;
        }
        else {
            log.warn("No biosource service is given, a default uniprot taxonomy service will be used");
            this.biosourceService = new BioSourceServiceImpl(new UniprotTaxonomyService());
        }

        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
    }

    public UniprotService getUniprotService() {
        return uniprotService;
    }

    @Override
    public Collection<Protein> getMasterProteinsByUniprotAc(String uniprotAc) throws ProteinServiceException{

        if (uniprotAc == null){
            throw new ProteinServiceException("Impossible to create an Intact protein if the uniprot ac is null");
        }

        Collection<UniprotProtein> uniprotProteins = uniprotService.retrieve(uniprotAc);

        Collection<Protein> proteins = new ArrayList<Protein>(uniprotProteins.size());
        for (UniprotProtein uniprot : uniprotProteins){
            proteins.add(getMasterProteinFromUniprotEntry(uniprot));
        }

        return proteins;
    }

    @Override
    public Protein getUniqueMasterProteinForUniprotAc(String uniprotAc) throws ProteinServiceException{

        if (uniprotAc == null){
            throw new ProteinServiceException("Impossible to create an Intact protein if the uniprot ac is null");
        }

        Collection<UniprotProtein> uniprotProteins = uniprotService.retrieve(uniprotAc);

        Protein protein = null;

        if (uniprotProteins.size() == 1){
            protein = getMasterProteinFromUniprotEntry(uniprotProteins.iterator().next());
        }
        else if (uniprotProteins.size() > 1){
            log.error("The uniprot ac " + uniprotAc + " returns " + uniprotProteins.size() + " different uniprot entries and it is not possible to create a single protein.");
        }

        return protein;
    }

    @Override
    public Collection<Protein> getProteinTranscriptsByUniprotAc(String uniprotAc, String intactParentAc) throws ProteinServiceException {
        if (uniprotAc == null){
            throw new ProteinServiceException("Impossible to create an Intact protein if the uniprot ac is null");
        }

        Collection<UniprotProteinTranscript> uniprotProteins = uniprotService.retrieveProteinTranscripts(uniprotAc);

        Collection<Protein> proteins = new ArrayList<Protein>(uniprotProteins.size());
        for (UniprotProteinTranscript uniprot : uniprotProteins){
            proteins.add(getProteinTranscriptFromUniprotEntry(uniprot, intactParentAc));
        }

        return proteins;
    }

    @Override
    public Protein getProteinTranscriptFromUniprotEntry(UniprotProteinTranscript uniprot, String intactParentAc) throws ProteinServiceException {
        if (uniprot == null){
            throw new ProteinServiceException("Impossible to create an IntAct protein if the uniprot entry is null");
        }

        if (uniprot.getOrganism() == null) {
            throw new ProteinServiceException("Uniprot protein without organism: "+uniprot);
        }

        BioSource biosource = null;
        try {
            biosource = biosourceService.getBiosourceByTaxid( String.valueOf( uniprot.getOrganism().getTaxid() ) );
        } catch ( BioSourceServiceException e ) {
            throw new ProteinServiceException(e);
        }

        // create minimalistic protein transcript
        Protein variant = new ProteinImpl( CvHelper.getInstitution(),
                biosource,
                uniprot.getPrimaryAc().toLowerCase(),
                CvHelper.getProteinType() );

        // set the fullName
        // we have a feature chain
        variant.setFullName(uniprot.getDescription());

        // set the sequence
        if (uniprot.getSequence() != null) {
            variant.setSequence(uniprot.getSequence());
            variant.setCrc64(Crc64.getCrc64(variant.getSequence()));
        } else if (!uniprot.isNullSequenceAllowed()){
            log.warn("Uniprot splice variant without sequence: "+variant);
        }

        // Create isoform-parent or chain-parent Xref
        if (intactParentAc != null){
            CvXrefQualifier isoformParent = CvHelper.getQualifierByMi( uniprot.getParentXRefQualifier() );
            CvDatabase intact = CvHelper.getDatabaseByMi( CvDatabase.INTACT_MI_REF );
            InteractorXref xref = new InteractorXref( CvHelper.getInstitution(), intact, intactParentAc, isoformParent );
            variant.addXref( xref );
        }

        // create uniprot cross references
        createUniprotXRefs(uniprot, variant, null);

        // create the aliases for protein transcripts
        createAliasesForProteinTranscripts(uniprot, variant);

        // Create note for splice variants
        createNote(uniprot, variant);

        // in case the protin transcript is a feature chain, we need to add two annotations containing the end and start positions of the feature chain
        createStartAndEndForFeatureChain(uniprot, variant);
        return variant;
    }

    @Override
    public Protein getUniqueProteinTranscriptForUniprotAc(String uniprotAc, String intactParentAc) throws ProteinServiceException {
        if (uniprotAc == null){
            throw new ProteinServiceException("Impossible to create an Intact protein if the uniprot ac is null");
        }

        Collection<UniprotProteinTranscript> uniprotProteins = uniprotService.retrieveProteinTranscripts(uniprotAc);

        Protein protein = null;

        if (uniprotProteins.size() == 1){
            protein = getProteinTranscriptFromUniprotEntry(uniprotProteins.iterator().next(), intactParentAc);
        }
        else if (uniprotProteins.size() > 1){
            log.error("The uniprot ac " + uniprotAc + " returns " + uniprotProteins.size() + " different uniprot entries and it is not possible to create a single protein.");
        }

        return protein;
    }

    @Override
    public Protein getMasterProteinFromUniprotEntry(UniprotProtein uniprot) throws ProteinServiceException {

        if (uniprot == null){
            throw new ProteinServiceException("Impossible to create an IntAct protein if the uniprot entry is null");
        }

        if (uniprot.getOrganism() == null) {
            throw new ProteinServiceException("Uniprot protein without organism: "+uniprot);
        }

        BioSource biosource = null;
        try {
            biosource = biosourceService.getBiosourceByTaxid( String.valueOf( uniprot.getOrganism().getTaxid() ) );
        } catch ( BioSourceServiceException e ) {
            throw new ProteinServiceException(e);
        }

        // create minimalistic protein
        Protein protein = new ProteinImpl( CvHelper.getInstitution(),
                biosource,
                UniprotProteinUpdaterImpl.generateProteinShortlabel(uniprot),
                CvHelper.getProteinType() );

        // Fullname
        protein.setFullName(uniprot.getDescription());

        // set sequences
        protein.setSequence(uniprot.getSequence());
        protein.setCrc64(uniprot.getCrc64());

        // Create UniProt Xrefs
        createUniprotXRefs(uniprot, protein, uniprot.getReleaseVersion());

        // Create other XRefs taken from uniprot and based on the same filter as the protein update
        createComplementaryXrefs(uniprot, protein);

        // Create the aliases based on the same filter as the uniprot update
        createAliases(uniprot, protein);

        return protein;
    }

    private void createUniprotXRefs(UniprotProteinLike uniprotProtein, Protein protein, String releaseVersion){
        CvDatabase uniprot = CvHelper.getDatabaseByMi( CvDatabase.UNIPROT_MI_REF );
        CvXrefQualifier identity = CvHelper.getQualifierByMi( CvXrefQualifier.IDENTITY_MI_REF );
        CvXrefQualifier secondaryAcQual = CvHelper.getQualifierByMi( CvXrefQualifier.SECONDARY_AC_MI_REF );
        Institution owner = CvHelper.getInstitution();

        protein.addXref(new InteractorXref(owner, uniprot, uniprotProtein.getPrimaryAc(), null, releaseVersion, identity));

        log.debug( "Found " + uniprotProtein.getSecondaryAcs().size() + " secondary ACs" );
        for ( String secondaryAc : uniprotProtein.getSecondaryAcs() ) {
            InteractorXref interactorXref =   new InteractorXref( owner, uniprot, secondaryAc, null, releaseVersion, secondaryAcQual );
            protein.addXref(interactorXref);
        }
    }

    private void createNote(UniprotProteinTranscript uniprot, Protein variant){
        String note = uniprot.getNote();
        if ( ( note != null ) && ( !note.trim().equals( "" ) ) ) {
            Institution owner = variant.getOwner();
            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDaoFactory();
            CvObjectDao<CvTopic> cvDao = daoFactory.getCvObjectDao( CvTopic.class );
            CvTopic comment = cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

            if (comment == null) {
                comment = CvObjectUtils.createCvObject(owner, CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
            }

            Annotation annotation = new Annotation( owner, comment );
            annotation.setAnnotationText( note );

            variant.addAnnotation(annotation);
        }
    }

    private void createStartAndEndForFeatureChain(UniprotProteinTranscript uniprot, Protein variant){
        // in case the protin transcript is a feature chain, we need to add two annotations containing the end and start positions of the feature chain
        if (CvXrefQualifier.CHAIN_PARENT_MI_REF.equalsIgnoreCase(uniprot.getParentXRefQualifier())){

            String startToString;
            String endToString;

            if (uniprot.getStart() == null || uniprot.getStart() == -1){
                startToString = UniprotProteinUpdaterImpl.FEATURE_CHAIN_UNKNOWN_POSITION;
            }
            else {
                startToString = Integer.toString(uniprot.getStart());
            }

            if (uniprot.getEnd() == null || uniprot.getEnd() == -1){
                endToString = UniprotProteinUpdaterImpl.FEATURE_CHAIN_UNKNOWN_POSITION;
            }
            else {
                endToString = Integer.toString(uniprot.getEnd());
            }

            DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

            CvObjectDao<CvTopic> cvTopicDao = factory.getCvObjectDao(CvTopic.class);

            CvTopic startPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_START);

            if (startPosition == null){
                startPosition = CvObjectUtils.createCvObject(variant.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_START);
            }
            Annotation start = new Annotation(startPosition, startToString);

            variant.addAnnotation(start);

            CvTopic endPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_END);

            if (endPosition == null){
                endPosition = CvObjectUtils.createCvObject(variant.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_END);
            }
            Annotation end = new Annotation(endPosition, endToString);

            variant.addAnnotation(end);
        }
    }

    private void createComplementaryXrefs(UniprotProtein uniprotProtein, Protein protein){

        Map<String, Collection<UniprotXref>> xrefCluster = XrefUpdaterUtils.clusterByDatabaseName( uniprotProtein.getCrossReferences() );

        for ( Map.Entry<String, Collection<UniprotXref>> entry : xrefCluster.entrySet() ) {

            String db = entry.getKey();
            Collection<UniprotXref> uniprotXrefs = entry.getValue();

            CvObjectDao<CvDatabase> dbDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class);

            // search by shortlabel is dodgy ! Try mapping to MI:xxxx first.
            String mi = databaseName2mi.get( db.toLowerCase() );
            CvDatabase cvDatabase = null;
            if ( mi != null ) {
                cvDatabase = dbDao.getByPsiMiRef( mi );

                if( cvDatabase == null ) {
                    log.error( "Could not find CvDatabase by label: " + db );
                }
            }

            if(cvDatabase != null){
                // Convert collection into Xref
                Collection<Xref> xrefs = XrefUpdaterUtils.convert( uniprotXrefs, cvDatabase );

                for (Xref xref : xrefs){
                    protein.addXref((InteractorXref) xref);
                }
            }else{
                log.debug("We are not copying across xref to " + db);
            }
        }

    }

    private void createAliases(UniprotProtein uniprotProtein, Protein protein){
        Collection<Alias> aliases = AliasUpdaterUtils.buildAliases(uniprotProtein, protein);

        for (Alias alias : aliases){
            protein.addAlias((InteractorAlias) alias);
        }
    }

    private void createAliasesForProteinTranscripts(UniprotProteinTranscript transcript, Protein protein){
        Collection<Alias> aliases = AliasUpdaterUtils.buildAliases(transcript.getMasterProtein(), transcript, protein );

        for (Alias alias : aliases){
            protein.addAlias((InteractorAlias) alias);
        }
    }

    public void setUniprotService(UniprotService uniprotService) {
        this.uniprotService = uniprotService;
    }

    public BioSourceService getBiosourceService() {
        return biosourceService;
    }

    public void setBiosourceService(BioSourceService biosourceService) {
        this.biosourceService = biosourceService;
    }
}
