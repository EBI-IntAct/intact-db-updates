/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.referencefilter.IntactCrossReferenceFilter;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.*;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceException;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.CvHelper;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.intact.util.protein.utils.*;

import java.util.*;

/**
 * Updates the current protein in the database, using information from uniprot.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UniprotProteinUpdater {

    private static final Log log = LogFactory.getLog( UniprotProteinUpdater.class );
    private static final String FEATURE_CHAIN_UNKNOWN_POSITION = "?";

    private final int MAX_RETRY_ATTEMPTS = 100;
    private int retryAttempt = 0;

    /**
     * Mapping allowing to specify which database shortlabel correspond to which MI reference.
     */
    private Map<String, String> databaseName2mi = new HashMap<String, String>();
    /**
     * The results
     */
    protected UniprotServiceResult uniprotServiceResult;

    private ProteinUpdateProcessor processor;
    private RangeFixer rangeFixer;

    /**
     * BioSource service allowing to create new BioSource in the database.
     */
    private BioSourceService bioSourceService;
    private OutOfDateParticipantFixer participantFixer;

    public UniprotProteinUpdater(TaxonomyService taxonomyService) {
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService(taxonomyService));
        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
        participantFixer = new OutOfDateParticipantFixer();
        rangeFixer = new RangeFixer();
    }

    public UniprotProteinUpdater() {
        // Build default taxonomy service
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService());
        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
        participantFixer = new OutOfDateParticipantFixer();
        rangeFixer = new RangeFixer();
    }

    /**
     * Create or update a protein.
     *
     *
     * @return an up-to-date IntAct protein.
     *
     * @throws ProteinServiceException
     */
    public void createOrUpdateProtein( UpdateCaseEvent evt) throws ProteinServiceException {
        this.processor = (ProteinUpdateProcessor) evt.getSource();
        UniprotProtein uniprotProtein = evt.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<? extends Protein> primaryProteins = evt.getPrimaryProteins();
        Collection<? extends Protein> secondaryProteins = evt.getSecondaryProteins();

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotProtein.getPrimaryAc());
        processor.fireOnUpdateCase(evt);

        // TODO returned proteins are not used here
        processProteinCase(uniprotProtein, primaryProteins, secondaryProteins, evt);
    }

    protected void processProteinCase(UniprotProtein uniprotProtein, Collection<? extends Protein> primaryProteins, Collection<? extends Protein> secondaryProteins, UpdateCaseEvent evt) throws ProteinServiceException {
        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if ( countPrimary == 0 && countSecondary == 0 ) {
            if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
            Protein protein = createMinimalisticProtein( uniprotProtein );
            updateProtein( protein, uniprotProtein, evt);

            proteinCreated(protein);
            evt.getPrimaryProteins().add(protein);

        } else {
            if (log.isDebugEnabled())
                log.debug("Found in IntAct"+countPrimary+" protein(s) with primaryAc and "+countSecondary+" protein(s) on with secondaryAc.");

            for (Protein protein : primaryProteins){
                updateProtein( protein, uniprotProtein, evt);
            }
            for (Protein protein : secondaryProteins){
                updateProtein( protein, uniprotProtein, evt);
            }
        }
    }

    /**
     * create or update a protein transcript

     * @param masterProtein : the IntAct master protein
     * @return the list of protein transcripts created or updated
     * @throws ProteinServiceException
     */
    public void createOrUpdateIsoform( UpdateCaseEvent caseEvent, Protein masterProtein) throws ProteinServiceException {
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = caseEvent.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryProteins = caseEvent.getSecondaryIsoforms();

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+caseEvent.getUniprotServiceResult().getQuerySentToService());
        processor.fireOnUpdateCase(caseEvent);

        // TODO returned proteins are not used here
        processIsoform(uniprotProtein, masterProtein, primaryProteins, secondaryProteins, caseEvent);
    }

    public void createOrUpdateFeatureChain( UpdateCaseEvent caseEvent, Protein masterProtein) throws ProteinServiceException {
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = caseEvent.getPrimaryFeatureChains();

        int countPrimary = primaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary for "+caseEvent.getUniprotServiceResult().getQuerySentToService());
        processor.fireOnUpdateCase(caseEvent);

        // TODO returned proteins are not used here
        processChain(uniprotProtein, masterProtein, primaryProteins, caseEvent);
    }

    /**
     * Process the protein transcript and update it
     * @param uniprotProtein
     * @param masterProtein
     * @param primaryProteins
     * @param secondaryProteins
     * @return
     * @throws ProteinServiceException
     */
    protected void processIsoform(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, Collection<ProteinTranscript> secondaryProteins, UpdateCaseEvent evt) throws ProteinServiceException {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        for (UniprotProteinTranscript ut : uniprotProtein.getSpliceVariants()){
            boolean hasFoundSpliceVariant = false;

            for (ProteinTranscript protTrans : primaryProteins){
                if (ut.equals(protTrans.getUniprotVariant())){
                    hasFoundSpliceVariant = true;
                    break;
                }
            }

            if (!hasFoundSpliceVariant){
                for (ProteinTranscript protTrans : secondaryProteins){
                    if (ut.equals(protTrans.getUniprotVariant())){
                        hasFoundSpliceVariant = true;
                        break;
                    }
                }
            }

            if (!hasFoundSpliceVariant){
                if (!config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions()){
                    if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), uniprotProtein );
                    primaryProteins.add(new ProteinTranscript(protein, ut));

                    proteinCreated(protein);
                }
            }
        }

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isDebugEnabled())
            log.debug("Found in IntAct"+countPrimary+" protein(s) with primaryAc and "+countSecondary+" protein(s) on with secondaryAc.");

        for (ProteinTranscript protein : primaryProteins){
            updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotVariant(), uniprotProtein, evt);
        }
        for (ProteinTranscript protein : secondaryProteins){
            updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotVariant(), uniprotProtein, evt);
        }
    }

    protected void processChain(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, UpdateCaseEvent evt) throws ProteinServiceException {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        for (UniprotProteinTranscript ut : uniprotProtein.getFeatureChains()){
            boolean hasFoundSpliceVariant = false;

            for (ProteinTranscript protTrans : primaryProteins){
                if (ut.equals(protTrans.getUniprotVariant())){
                    hasFoundSpliceVariant = true;
                    break;
                }
            }

            if (!hasFoundSpliceVariant){
                if (!config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions()){
                    if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), uniprotProtein );
                    primaryProteins.add(new ProteinTranscript(protein, ut));

                    proteinCreated(protein);
                }
            }
        }

        int countPrimary = primaryProteins.size();

        if (log.isDebugEnabled())
            log.debug("Found in IntAct"+countPrimary+" protein(s) with primaryAc");

        for (ProteinTranscript protein : primaryProteins){
            updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotVariant(), uniprotProtein, evt);
        }
    }

    /**
     * Update an existing intact protein's annotations.
     * <p/>
     * That includes, all Xrefs, Aliases, splice variants.
     *
     * @param protein        the intact protein to update.
     * @param uniprotProtein the uniprot protein used for data input.
     */
    private void updateProtein( Protein protein, UniprotProtein uniprotProtein, UpdateCaseEvent evt) throws ProteinServiceException {

        // check that both protein carry the same organism information
        if (!UpdateBioSource(protein, uniprotProtein.getOrganism())){
            return;
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
            evt.getUniprotServiceResult().addXrefUpdaterReport(report);
        }

        // Aliases
        AliasUpdaterUtils.updateAllAliases( protein, uniprotProtein );

        // Sequence
        updateProteinSequence(protein, uniprotProtein.getSequence(), uniprotProtein.getCrc64(), evt);

        // Persist changes
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.update( ( ProteinImpl ) protein );
    }

    private boolean UpdateBioSource(Protein protein, Organism organism) {
        // check that both protein carry the same organism information
        BioSource organism1 = protein.getBioSource();

        int t2 = organism.getTaxid();

        if (organism1 == null) {
            if (log.isWarnEnabled()) log.warn("Protein protein does not contain biosource. It will be assigned the Biosource from uniprot: "+organism.getName()+" ("+organism.getTaxid()+")");
            organism1 = new BioSource(protein.getOwner(), organism.getName(), String.valueOf(t2));
            protein.setBioSource(organism1);

            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(organism1);
        }

        if ( organism1 != null && !String.valueOf( t2 ).equals( organism1.getTaxId() ) ) {
            processor.fireonProcessErrorFound(new UpdateErrorEvent(processor, IntactContext.getCurrentInstance().getDataContext(), "UpdateProteins is trying to modify" +
                    " the BioSource(" + organism1.getTaxId() + "," + organism1.getShortLabel() +  ") of the following protein protein " +
                    getProteinDescription(protein) + " by BioSource( " + t2 + "," +
                    organism.getName() + " ). Changing the organism of an existing protein is a forbidden operation.", UpdateError.organism_conflict_with_uniprot_protein));
            processor.finalizeAfterCurrentPhase();

            return false;
        }
        return true;
    }

    private void updateProteinSequence(Protein protein, String uniprotSequence, String uniprotCrC64, UpdateCaseEvent evt) throws ProteinServiceException {
        boolean sequenceToBeUpdated = false;
        String oldSequence = protein.getSequence();
        String sequence = uniprotSequence;
        if ( (oldSequence == null && sequence != null)) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Sequence requires update." );
            }
            sequenceToBeUpdated = true;
        }
        else if (oldSequence != null && sequence == null){
            processor.fireonProcessErrorFound(new UpdateErrorEvent(processor, IntactContext.getCurrentInstance().getDataContext(), "The sequence of the protein " + protein.getAc() +
                    " is not null but the uniprot entry has a sequence null.", UpdateError.uniprot_sequence_null));
            processor.finalizeAfterCurrentPhase();
        }
        else if (oldSequence != null && sequence != null){
            if (!sequence.equals( oldSequence ) ){
                if ( log.isDebugEnabled() ) {
                    log.debug( "Sequence requires update." );
                }
                sequenceToBeUpdated = true;
            }
        }

        if ( sequenceToBeUpdated) {
            Collection<Component> componentsWithRangeConflicts = rangeFixer.updateRanges(protein, uniprotSequence, processor);

            if (!componentsWithRangeConflicts.isEmpty()){

                OutOfDateParticipantFoundEvent participantEvent = new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), componentsWithRangeConflicts, protein, evt.getProtein(), evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), evt.getPrimaryFeatureChains());
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvent, true);

                ProteinTools.updateProteinTranscripts(evt.getDataContext().getDaoFactory(), protein, fixedProtein.getProtein());

                evt.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());

                if (!ProteinUtils.isFromUniprot(fixedProtein.getProtein())){
                    processor.fireNonUniprotProteinFound(new ProteinEvent(evt.getSource(), evt.getDataContext(), fixedProtein.getProtein()));
                }
                else {
                    updateProteinTranscript(fixedProtein.getProtein(), protein, fixedProtein.getUniprotVariant(), evt.getProtein(), evt);
                }
            }

            protein.setSequence( sequence );

            // CRC64
            String crc64 = uniprotCrC64;
            if ( protein.getCrc64() == null || !protein.getCrc64().equals( crc64 ) ) {
                log.debug( "CRC64 requires update." );
                protein.setCrc64( crc64 );
            }

            sequenceChanged(protein, sequence, oldSequence, uniprotCrC64);
        }
    }

    /**
     * Update an existing splice variant.
     *
     * @param transcript
     * @param uniprotTranscript
     */
    private boolean updateProteinTranscript( Protein transcript, Protein master,
                                             UniprotProteinTranscript uniprotTranscript,
                                             UniprotProtein uniprotProtein,
                                             UpdateCaseEvent evt) throws ProteinServiceException {

        if (!UpdateBioSource(transcript, uniprotTranscript.getOrganism())){
            return false;
        }

        transcript.setShortLabel( uniprotTranscript.getPrimaryAc().toLowerCase() );

        // we have a feature chain
        if (uniprotTranscript.getDescription() != null){
            transcript.setFullName(uniprotTranscript.getDescription());
        }
        // we have a splice variant
        else {
            transcript.setFullName( master.getFullName() );
        }

        // update UniProt Xrefs
        XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( transcript, uniprotTranscript, uniprotProtein );

        // Update Aliases from the uniprot protein aliases
        AliasUpdaterUtils.updateAllAliases( transcript, uniprotTranscript, uniprotProtein );

        // Sequence
        updateProteinSequence(transcript, uniprotTranscript.getSequence(), Crc64.getCrc64(uniprotTranscript.getSequence()), evt);
        // Add IntAct Xref

        // Update Note
        String note = uniprotTranscript.getNote();
        if ( ( note != null ) && ( !note.trim().equals( "" ) ) ) {
            Institution owner = IntactContext.getCurrentInstance().getInstitution();
            DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            CvObjectDao<CvTopic> cvDao = daoFactory.getCvObjectDao( CvTopic.class );
            CvTopic comment = cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

            if (comment == null) {
                comment = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(comment);
            }

            Annotation annotation = new Annotation( owner, comment );
            annotation.setAnnotationText( note );
            AnnotationUpdaterUtils.addNewAnnotation( transcript, annotation );
        }

        // in case the protin transcript is a feature chain, we need to add two annotations containing the end and start positions of the feature chain
        if (CvXrefQualifier.CHAIN_PARENT_MI_REF.equalsIgnoreCase(uniprotTranscript.getParentXRefQualifier())){
            boolean hasStartPosition = false;
            boolean hasEndPosition = false;
            String startToString = Integer.toString(uniprotTranscript.getStart());
            String endToString = Integer.toString(uniprotTranscript.getEnd());

            DaoFactory factory = IntactContext.getCurrentInstance().getDaoFactory();

            // check if the annotated object already contains a start and or end position
            for (Annotation annot : transcript.getAnnotations()) {
                if (CvTopic.CHAIN_SEQ_START.equals(annot.getCvTopic().getShortLabel())) {
                    hasStartPosition = true;
                    if (uniprotTranscript.getStart() == -1){
                        annot.setAnnotationText(FEATURE_CHAIN_UNKNOWN_POSITION);
                    }
                    else {
                        annot.setAnnotationText(startToString);
                    }
                    factory.getAnnotationDao().update(annot);
                }
                else if (CvTopic.CHAIN_SEQ_END.equals(annot.getCvTopic().getShortLabel())) {
                    hasEndPosition = true;
                    if (uniprotTranscript.getEnd() == -1){
                        annot.setAnnotationText(FEATURE_CHAIN_UNKNOWN_POSITION);
                    }
                    else {
                        annot.setAnnotationText(endToString);
                    }
                    factory.getAnnotationDao().update(annot);
                }
            }

            CvObjectDao<CvTopic> cvTopicDao = factory.getCvObjectDao(CvTopic.class);

            if (!hasStartPosition){
                CvTopic startPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_START);

                if (startPosition == null){
                    startPosition = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.CHAIN_SEQ_START);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(startPosition);
                }
                Annotation start = new Annotation(startPosition, startToString);
                factory.getAnnotationDao().persist(start);

                transcript.addAnnotation(start);
            }
            if (!hasEndPosition){
                CvTopic endPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_END);

                if (endPosition == null){
                    endPosition = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.CHAIN_SEQ_END);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(endPosition);
                }
                Annotation end = new Annotation(endPosition, endToString);
                factory.getAnnotationDao().persist(end);

                transcript.addAnnotation(end);
            }
        }

        // Persist changes
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.update( ( ProteinImpl ) transcript );

        return true;
    }

    protected void sequenceChanged(Protein protein, String newSequence, String oldSequence, String crc64) {
        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, IntactContext.getCurrentInstance().getDataContext(), protein, oldSequence, newSequence, crc64));
    }

    protected void proteinCreated(Protein protein) {
        processor.fireOnProteinCreated(new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), protein));
    }

    public BioSourceService getBioSourceService() {
        return bioSourceService;
    }

    public void setBioSourceService(BioSourceService bioSourceService) {
        if ( bioSourceService == null ) {
            throw new IllegalArgumentException( "bioSourceService must not be null." );
        }
        this.bioSourceService = bioSourceService;
    }

    public UniprotServiceResult getUniprotServiceResult() {
        return uniprotServiceResult;
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
            protein.setSequence(uniprotProtein.getSequence());
            protein.setCrc64(uniprotProtein.getCrc64());

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

    private String generateProteinShortlabel( UniprotProtein uniprotProtein ) {

        String name = null;

        if ( uniprotProtein == null ) {
            throw new NullPointerException( "uniprotProtein must not be null." );
        }

        name = uniprotProtein.getId();

        return name.toLowerCase();
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
                                                         String masterAc,
                                                         BioSource masterBiosource,
                                                         UniprotProtein uniprotProtein
    ) {

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();

        Protein variant = new ProteinImpl( CvHelper.getInstitution(),
                masterBiosource,
                uniprotProteinTranscript.getPrimaryAc().toLowerCase(),
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
        InteractorXref xref = new InteractorXref( CvHelper.getInstitution(), intact, masterAc, isoformParent );
        variant.addXref( xref );
        XrefDao xdao = daoFactory.getXrefDao();
        xdao.persist( xref );

        // Create UniProt Xrefs
        XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( variant, uniprotProteinTranscript, uniprotProtein );

        pdao.update( ( ProteinImpl ) variant );

        return variant;
    }

    private String getProteinDescription(Protein protein){
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        return "[" + protein.getAc() + ","+ protein.getShortLabel() + "," + uniprotXref.getPrimaryId() + "]";
    }
}
