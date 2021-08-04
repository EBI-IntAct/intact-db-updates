package uk.ac.ebi.intact.dbupdate.prot.actions.updaters;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.bridges.fetcher.OrganismFetcher;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.OutOfDateParticipantFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.model.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.report.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceException;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.CvHelper;
import uk.ac.ebi.intact.util.protein.utils.*;
import uk.ac.ebi.intact.util.protein.utils.comparator.InteractorAliasComparator;

import java.util.*;

/**
 * Updates the current protein in the database, using information from uniprot.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UniprotProteinUpdater {

    private static final Log log = LogFactory.getLog( UniprotProteinUpdater.class );
    public static final String FEATURE_CHAIN_UNKNOWN_POSITION = "?";

    private ProteinUpdateProcessor processor;
    private RangeFixer rangeFixer;

    /**
     * BioSource service allowing to create new BioSource in the database.
     */
    private BioSourceService bioSourceService;
    private OutOfDateParticipantFixer participantFixer;

    private final TreeSet<InteractorAlias> sortedInteractorAliases;

    public UniprotProteinUpdater(OrganismFetcher taxonomyService, OutOfDateParticipantFixer outOfDateParticipantFixer) {
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService(taxonomyService));
        this.participantFixer = outOfDateParticipantFixer;
        this.rangeFixer = this.participantFixer.getRangeFixer();
        sortedInteractorAliases = new TreeSet<>(new InteractorAliasComparator());
    }

    public UniprotProteinUpdater(OutOfDateParticipantFixer outOfDateParticipantFixer) {
        // Build default taxonomy service
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService());

        if (outOfDateParticipantFixer != null) {
            this.participantFixer = outOfDateParticipantFixer;

            if (participantFixer.getRangeFixer() == null){
                this.rangeFixer = new RangeFixer();
                participantFixer.setRangeFixer(this.rangeFixer);
            }
            else{
                this.rangeFixer = participantFixer.getRangeFixer();
            }
        }
        else {
            if (this.rangeFixer == null){
                this.rangeFixer = new RangeFixer();
            }

            this.participantFixer = new OutOfDateParticipantFixer(this.rangeFixer);
        }

        sortedInteractorAliases = new TreeSet<>(new InteractorAliasComparator());
    }

    /**
     * Create or update the master proteins
     * @param evt : contains the uniprot entry and the list of proteins to update
     */
    public void createOrUpdateProtein( UpdateCaseEvent evt) throws BioSourceServiceException, IntactTransactionException{
        this.processor = (ProteinUpdateProcessor) evt.getSource();
        UniprotProtein uniprotProtein = evt.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<Protein> primaryProteins = new ArrayList<>(evt.getPrimaryProteins());
        Collection<Protein> secondaryProteins = new ArrayList<>(evt.getSecondaryProteins());

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotProtein.getPrimaryAc());

        processProteinCase(uniprotProtein, primaryProteins, secondaryProteins, evt);
    }

    private void processProteinCase(UniprotProtein uniprotProtein, Collection<Protein> primaryProteins, Collection<Protein> secondaryProteins, UpdateCaseEvent evt) throws BioSourceServiceException, IntactTransactionException {
        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if ( countPrimary == 0 && countSecondary == 0 ) {
            if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
            Protein protein = createMinimalisticProtein( uniprotProtein, evt.getDataContext() );
            updateProtein( protein, uniprotProtein, evt);

            proteinCreated(protein, evt.getDataContext(), "No Intact master protein existed (or was valid) for the uniprot ac " + uniprotProtein.getPrimaryAc(), uniprotProtein.getPrimaryAc());
            evt.getPrimaryProteins().add(protein);
            evt.getProteins().add(protein.getAc());

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
     * Create of update the isoforms
     * @param caseEvent : contains the uniprot entry and the list of proteins to update
     * @param masterProtein : the master protein which is the parent of the isoforms
     */
    public void createOrUpdateIsoform( UpdateCaseEvent caseEvent, Protein masterProtein){
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = new ArrayList<>(caseEvent.getPrimaryIsoforms());
        Collection<ProteinTranscript> secondaryProteins = new ArrayList<>(caseEvent.getSecondaryIsoforms());

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+caseEvent.getQuerySentToService());

        // TODO returned proteins are not used here
        processIsoform(uniprotProtein, masterProtein, primaryProteins, secondaryProteins, caseEvent);
    }

    /**
     * Create of update the feature chains
     * @param caseEvent : contains the uniprot entry and the list of proteins to update
     * @param masterProtein : the master protein which is the parent of the isoforms
     */
    public void createOrUpdateFeatureChain( UpdateCaseEvent caseEvent, Protein masterProtein){
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = new ArrayList<>(caseEvent.getPrimaryFeatureChains());

        int countPrimary = primaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary for "+caseEvent.getQuerySentToService());

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
     */
    private void processIsoform(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, Collection<ProteinTranscript> secondaryProteins, UpdateCaseEvent evt){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        for (UniprotProteinTranscript ut : uniprotProtein.getSpliceVariants()){
            boolean hasFoundSpliceVariant = false;

            for (ProteinTranscript protTrans : primaryProteins){
                if (ut.equals(protTrans.getUniprotProteinTranscript())){
                    hasFoundSpliceVariant = true;
                    break;
                }
            }

            if (!hasFoundSpliceVariant){
                for (ProteinTranscript protTrans : secondaryProteins){
                    if (ut.equals(protTrans.getUniprotProteinTranscript())){
                        hasFoundSpliceVariant = true;
                        break;
                    }
                }
            }

            if (!hasFoundSpliceVariant){
                if (!config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions()){
                    if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), evt.getDataContext() );
                    evt.getPrimaryIsoforms().add(new ProteinTranscript(protein, ut));

                    updateProteinTranscript(protein, masterProtein, ut, uniprotProtein, evt);
                    proteinCreated(protein, evt.getDataContext(), "No IntAct splice variant existed (or was valid) for the uniprot ac " + ut.getPrimaryAc(), ut.getPrimaryAc());
                    evt.getProteins().add(protein.getAc());
                }
            }
        }

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isDebugEnabled())
            log.debug("Found in IntAct"+countPrimary+" protein(s) with primaryAc and "+countSecondary+" protein(s) on with secondaryAc.");

        for (ProteinTranscript protein : primaryProteins){
            if (protein.getUniprotProteinTranscript() != null){
                updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotProteinTranscript(), uniprotProtein, evt);
            }
        }
        for (ProteinTranscript protein : secondaryProteins){
            if (protein.getUniprotProteinTranscript() != null){
                updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotProteinTranscript(), uniprotProtein, evt);
            }
        }
    }

    private void processChain(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, UpdateCaseEvent evt){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        for (UniprotProteinTranscript ut : uniprotProtein.getFeatureChains()){
            boolean hasFoundFeatureChain = false;

            for (ProteinTranscript protTrans : primaryProteins){
                if (ut.equals(protTrans.getUniprotProteinTranscript())){
                    hasFoundFeatureChain = true;
                    break;
                }
            }

            if (!hasFoundFeatureChain){
                if (!config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions()){
                    if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), evt.getDataContext() );
                    evt.getPrimaryFeatureChains().add(new ProteinTranscript(protein, ut));

                    updateProteinTranscript(protein, masterProtein, ut, uniprotProtein, evt);
                    proteinCreated(protein, evt.getDataContext(), "No IntAct feature chain existed (or was valid) for the uniprot ac " + ut.getPrimaryAc(), ut.getPrimaryAc());
                    evt.getProteins().add(protein.getAc());
                }
            }
        }

        int countPrimary = primaryProteins.size();

        if (log.isDebugEnabled())
            log.debug("Found in IntAct"+countPrimary+" protein(s) with primaryAc");

        for (ProteinTranscript protein : primaryProteins){
            if (protein.getUniprotProteinTranscript() != null){
                updateProteinTranscript( protein.getProtein(), masterProtein, protein.getUniprotProteinTranscript(), uniprotProtein, evt);
            }
        }
    }

    /**
     * Update an existing intact protein
     */
    private void updateProtein(Protein intactProtein, UniprotProtein uniprotProtein, UpdateCaseEvent evt) {

        // check that both protein carry the same organism information
        if (!updateBioSource(intactProtein, uniprotProtein.getOrganism(), uniprotProtein.getPrimaryAc(), evt.getDataContext())) {
            evt.getPrimaryProteins().remove(intactProtein);
            return;
        }

        boolean hasBeenUpdated = false;

        // update fullName and shortLabel
        // Noe: 24/06/2021 The interactor.fullname in the database is a varchar2 of 1000 so I think truncating the fullname can be update to 1000
        String fullName = uniprotProtein.getDescription();
        if (fullName != null && fullName.length() > 250) {
            if (log.isDebugEnabled()) {
                log.debug("Truncating fullname to the first 250 first chars.");
            }
            fullName = fullName.substring(0, 250);
        }

        if (fullName != null && !fullName.equals(intactProtein.getFullName())) {
            intactProtein.setFullName(fullName);
        } else if (intactProtein.getFullName() != null && !intactProtein.getFullName().equals(fullName)) {
            intactProtein.setFullName(fullName);
        }
        // no update done
        else {
            fullName = null;
        }

        String shortLabel = generateProteinShortlabel(uniprotProtein);

        if (shortLabel != null && !shortLabel.equals(intactProtein.getShortLabel())) {
            intactProtein.setShortLabel(shortLabel);
        } else if (intactProtein.getShortLabel() != null && !intactProtein.getShortLabel().equals(shortLabel)) {
            intactProtein.setShortLabel(shortLabel);
        }
        // no update done
        else {
            shortLabel = null;
        }

        // add a report if updated shortlabel or fullname
        if (shortLabel != null || fullName != null) {
            hasBeenUpdated = true;

            ProteinNameUpdateReport nameReport = new ProteinNameUpdateReport(intactProtein.getAc(), shortLabel, fullName);
            evt.addNameUpdaterReport(nameReport);
        }

        // update all Xrefs UniProt Xrefs included
        XrefUpdaterReport reports = XrefUpdaterUtils.updateAllProteinXrefs(intactProtein, uniprotProtein, evt.getDataContext());
        if (reports != null) {
            hasBeenUpdated = true;

            evt.addXrefUpdaterReport(reports);
        }

        // update aliases from the uniprot protein aliases
        AliasUpdateReport aliasReport = AliasUpdaterUtils.updateAliases(uniprotProtein, intactProtein, evt.getDataContext().getDaoFactory().getAliasDao(InteractorAlias.class), this.sortedInteractorAliases);
        if (!aliasReport.getAddedAliases().isEmpty() || !aliasReport.getRemovedAliases().isEmpty()) {
            hasBeenUpdated = true;

            evt.addAliasUpdaterReport(aliasReport);
        }

        // update sequence
        if (updateProteinSequence(intactProtein, uniprotProtein.getSequence(), uniprotProtein.getCrc64(), evt, intactProtein.getAc())) {
            hasBeenUpdated = true;
        }

        // Persist changes
        if (hasBeenUpdated) {
            DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
            ProteinDao pdao = daoFactory.getProteinDao();
            pdao.update((ProteinImpl) intactProtein);
        }
    }

    private boolean updateBioSource(Protein protein, Organism organism, String uniprotAc, DataContext context) {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // check that both protein carry the same organism information
        BioSource organism1 = protein.getBioSource();

        int t2 = organism.getTaxid();

        if (organism1 == null) {
            if (log.isWarnEnabled()) log.warn("Protein protein does not contain biosource. It will be assigned the Biosource from uniprot: "+organism.getName()+" ("+organism.getTaxid()+")");
            try {
                organism1 = bioSourceService.getBiosourceByTaxid(String.valueOf(t2));
                protein.setBioSource(organism1);

                if (organism1.getAc() == null){
                    context.getDaoFactory().getBioSourceDao().persist(organism1);
                }
            } catch (BioSourceServiceException e) {
                ProteinUpdateError organismConflict = errorFactory.createFatalUpdateError(protein.getAc(), uniprotAc, e);
                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, context, organismConflict, protein, uniprotAc));

                return false;
            }
        }

        if ( organism1 != null && !String.valueOf( t2 ).equals( organism1.getTaxId() ) ) {
            ProteinUpdateError organismConflict = errorFactory.createOrganismConflictError(protein.getAc(), organism1.getTaxId(), String.valueOf(t2), uniprotAc);
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, context, organismConflict, protein, uniprotAc));

            return false;
        }
        return true;
    }

    private boolean updateProteinSequence(Protein protein, String uniprotSequence, String uniprotCrC64, UpdateCaseEvent evt, String masterProteinAc) {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        InteractorXref ref = ProteinUtils.getUniprotXref(protein);
        String uniprotAc = ref.getPrimaryId();

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
            ProteinUpdateError sequenceNull = errorFactory.createUniprotSequenceNullError(protein.getAc(), uniprotAc, oldSequence);
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), sequenceNull, protein, uniprotAc));
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

            UniprotProteinTranscript transcriptsWithSameSequence = null;

            if (oldSequence != null){
                transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(protein.getSequence(), evt.getProtein());
            }

            if (transcriptsWithSameSequence != null){
                processor.fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(processor, evt.getDataContext(), protein, uniprotAc, transcriptsWithSameSequence.getPrimaryAc()));
            }

            RangeUpdateReport report =  rangeFixer.updateRanges(protein, uniprotSequence, processor, evt.getDataContext());

            if (!report.getShiftedRanges().isEmpty() || (report.getInvalidComponents().isEmpty() && !report.getUpdatedFeatureAnnotations().isEmpty())){
                evt.getUpdatedRanges().put(protein.getAc(), report);
            }

            if (!report.getInvalidComponents().isEmpty()){
                boolean createDeprecatedProtein = ProteinUtils.isSpliceVariant(protein) || ProteinUtils.isFeatureChain(protein);

                // no unisave sequence for splice variants and feature chains so if we have conflicts, it is better to create a no-uniprot protein with the sequence of the moment

                OutOfDateParticipantFoundEvent participantEvent = new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), protein, evt.getProtein(), report, evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), evt.getPrimaryFeatureChains(), masterProteinAc);
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvent, createDeprecatedProtein, true);

                if (fixedProtein != null){
                    evt.getProteins().add(fixedProtein.getProtein().getAc());

                    boolean hasToBeAdded = true;

                    for (ProteinTranscript t : evt.getPrimaryIsoforms()){
                        if (t.getProtein().getAc().equals(fixedProtein.getProtein().getAc())){
                            hasToBeAdded = false;
                            break;
                        }
                    }

                    if (hasToBeAdded){
                        for (ProteinTranscript t : evt.getSecondaryIsoforms()){
                            if (t.getProtein().getAc().equals(fixedProtein.getProtein().getAc())){
                                hasToBeAdded = false;
                                break;
                            }
                        }

                        if (hasToBeAdded){
                            for (ProteinTranscript t : evt.getPrimaryFeatureChains()){
                                if (t.getProtein().getAc().equals(fixedProtein.getProtein().getAc())){
                                    hasToBeAdded = false;
                                    break;
                                }
                            }

                            if (hasToBeAdded){
                                if (fixedProtein.getUniprotProteinTranscript() instanceof UniprotSpliceVariant){
                                    evt.getPrimaryIsoforms().add(fixedProtein);
                                }
                                else {
                                    evt.getPrimaryFeatureChains().add(fixedProtein);
                                }
                            }
                        }
                    }
                }
            }

            protein.setSequence( sequence );

            // CRC64
            String crc64 = uniprotCrC64;
            if ( protein.getCrc64() == null || !protein.getCrc64().equals( crc64 ) ) {
                log.debug( "CRC64 requires update." );
                protein.setCrc64( crc64 );
            }

            sequenceChanged(protein, uniprotAc, sequence, oldSequence, uniprotCrC64, evt.getDataContext());
        }
        else{

            RangeUpdateReport report =  rangeFixer.updateOnlyInvalidRanges(protein, processor, evt.getDataContext());

            if (!report.getInvalidComponents().isEmpty()){

                OutOfDateParticipantFoundEvent participantEvent = new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), protein, evt.getProtein(), report, evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), evt.getPrimaryFeatureChains(), masterProteinAc);
                participantFixer.fixParticipantWithRangeConflicts(participantEvent, false, true);
            }
        }

        return sequenceToBeUpdated;
    }

    /**
     * Update an existing splice variant.
     */
    private void updateProteinTranscript(Protein intactTranscript,
                                         Protein master,
                                         UniprotProteinTranscript uniprotTranscript,
                                         UniprotProtein uniprotProtein,
                                         UpdateCaseEvent evt) {

        // check that both protein carry the same organism information
        if (!updateBioSource(intactTranscript, uniprotTranscript.getOrganism(), uniprotTranscript.getPrimaryAc(), evt.getDataContext())){
            ProteinTranscript transcriptToRemove = null;
            for (ProteinTranscript t : evt.getPrimaryIsoforms()){
                if (t.getProtein().getAc().equals(intactTranscript.getAc())){
                    transcriptToRemove = t;
                }
            }
            if (transcriptToRemove == null){
                for (ProteinTranscript t : evt.getSecondaryIsoforms()){
                    if (t.getProtein().getAc().equals(intactTranscript.getAc())){
                        transcriptToRemove = t;
                    }
                }

                if (transcriptToRemove == null){
                    for (ProteinTranscript t : evt.getPrimaryFeatureChains()){
                        if (t.getProtein().getAc().equals(intactTranscript.getAc())){
                            transcriptToRemove = t;
                        }
                    }

                    if (transcriptToRemove != null){
                        evt.getPrimaryFeatureChains().remove(transcriptToRemove);
                    }
                }
                else {
                    evt.getSecondaryIsoforms().remove(transcriptToRemove);
                }
            }
            else {
                evt.getPrimaryIsoforms().remove(transcriptToRemove);
            }
            return;
        }

        boolean hasBeenUpdated = false;

        // update fullName and shortLabel
        String fullName;
        // we have a feature chain
        if (uniprotTranscript.getDescription() != null){
            fullName = uniprotTranscript.getDescription();
        }
        // we have a splice variant
        else {
            fullName = master.getFullName();
        }

        if (fullName != null && !fullName.equals(intactTranscript.getFullName())){
            intactTranscript.setFullName( fullName );
        } else if (intactTranscript.getFullName() != null && !intactTranscript.getFullName().equals(fullName)){
            intactTranscript.setFullName( fullName );
        }
        // no update done
        else {
            fullName = null;
        }

        String shortLabel = uniprotTranscript.getPrimaryAc().toLowerCase();
        if (!shortLabel.equals(intactTranscript.getShortLabel())){
            intactTranscript.setShortLabel(shortLabel);
        } else if (intactTranscript.getShortLabel() != null && !intactTranscript.getShortLabel().equals(shortLabel)){
            intactTranscript.setShortLabel(shortLabel);
        }
        // no update done
        else {
            shortLabel = null;
        }

        // add a report if updated shortlabel or fullname
        if (shortLabel != null || fullName != null){
            hasBeenUpdated = true;

            ProteinNameUpdateReport nameReport = new ProteinNameUpdateReport(intactTranscript.getAc(), shortLabel, fullName);
            evt.addNameUpdaterReport(nameReport);
        }

        // update all Xrefs UniProt Xrefs included
        XrefUpdaterReport reports = XrefUpdaterUtils.updateAllProteinTranscriptXrefs(intactTranscript, uniprotTranscript, uniprotProtein, evt.getDataContext() );
        if (reports != null){
            hasBeenUpdated = true;

            evt.addXrefUpdaterReport(reports);
        }

        // update aliases from the uniprot protein aliases
        AliasUpdateReport aliasReport = AliasUpdaterUtils.updateIsoformAliases(uniprotProtein, uniprotTranscript, intactTranscript, evt.getDataContext().getDaoFactory().getAliasDao(InteractorAlias.class), this.sortedInteractorAliases);
        if (!aliasReport.getAddedAliases().isEmpty() || !aliasReport.getRemovedAliases().isEmpty()){
            hasBeenUpdated = true;

            evt.addAliasUpdaterReport(aliasReport);
        }

        // update sequence
        if (uniprotTranscript.getSequence() != null){
            if (updateProteinSequence(intactTranscript, uniprotTranscript.getSequence(), Crc64.getCrc64(uniprotTranscript.getSequence()), evt, master.getAc())){
                hasBeenUpdated = true;
            }
        } // uniprot transcript sequence is likely to be null
        else if (uniprotTranscript.isNullSequenceAllowed() && intactTranscript.getSequence() != null){
            ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
            ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();
            ProteinUpdateError sequenceNull = errorFactory.createUniprotSequenceNullError(intactTranscript.getAc(), uniprotTranscript.getPrimaryAc(), intactTranscript.getSequence());
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), sequenceNull, intactTranscript, uniprotTranscript.getPrimaryAc()));
        }

        // update annotations
        // If the transcript has a note, we store it as an isoform comment
        String note = uniprotTranscript.getNote();
        Collection<Annotation> newAnnotations = new ArrayList<>(3);

        if ( ( note != null ) && ( !note.trim().equals( "" ) ) ) {
            Institution owner = intactTranscript.getOwner();
            DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
            CvObjectDao<CvTopic> cvDao = daoFactory.getCvObjectDao( CvTopic.class );
            CvTopic comment = cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

            // if isoform-comment cv is not in the db we create it first.
            if (comment == null) {
                comment = CvObjectUtils.createCvObject(owner, CvTopic.class, null, CvTopic.ISOFORM_COMMENT);
                IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(comment);
            }

            boolean hasAlreadyAnIsoformComment = false;

            for (Annotation annot : intactTranscript.getAnnotations()) {
                if (CvTopic.ISOFORM_COMMENT.equals(annot.getCvTopic().getShortLabel())) {
                    hasAlreadyAnIsoformComment = true;

                    if (!note.equalsIgnoreCase(annot.getAnnotationText())){
                        annot.setAnnotationText(note);
                        evt.getDataContext().getDaoFactory().getAnnotationDao().update(annot);

                        // a new annotation has been added (updated in this case)
                        newAnnotations.add(annot);
                    }
                }
            }

            if (!hasAlreadyAnIsoformComment){

                Annotation annotation = new Annotation( owner, comment );
                annotation.setAnnotationText( note );

                newAnnotations.add(annotation);

                evt.getDataContext().getDaoFactory().getAnnotationDao().persist(annotation);
                intactTranscript.addAnnotation(annotation);
            }
        }

        // in case the protein transcript is a feature chain, we need to add two annotations containing the end and start positions of the feature chain
        if (CvXrefQualifier.CHAIN_PARENT_MI_REF.equalsIgnoreCase(uniprotTranscript.getParentXRefQualifier())){
            boolean hasStartPosition = false;
            boolean hasEndPosition = false;

            String startToString;
            String endToString;

            if (uniprotTranscript.getStart() == null || uniprotTranscript.getStart() == -1){
                startToString = FEATURE_CHAIN_UNKNOWN_POSITION;
            }
            else {
                startToString = Integer.toString(uniprotTranscript.getStart());
            }

            if (uniprotTranscript.getEnd() == null || uniprotTranscript.getEnd() == -1){
                endToString = FEATURE_CHAIN_UNKNOWN_POSITION;
            }
            else {
                endToString = Integer.toString(uniprotTranscript.getEnd());
            }

            DaoFactory factory = evt.getDataContext().getDaoFactory();

            // check if the annotated object already contains a start and or end position
            for (Annotation annot : intactTranscript.getAnnotations()) {
                if (CvTopic.CHAIN_SEQ_START.equals(annot.getCvTopic().getShortLabel())) {
                    hasStartPosition = true;

                    if (!startToString.equalsIgnoreCase(annot.getAnnotationText())){
                        annot.setAnnotationText(startToString);
                        factory.getAnnotationDao().update(annot);

                        // a new annotation has been added (updated in this case)
                        newAnnotations.add(annot);
                    }
                }
                else if (CvTopic.CHAIN_SEQ_END.equals(annot.getCvTopic().getShortLabel())) {
                    hasEndPosition = true;

                    if (!endToString.equalsIgnoreCase(annot.getAnnotationText())){
                        annot.setAnnotationText(endToString);
                        factory.getAnnotationDao().update(annot);

                        // a new annotation has been added (updated in this case)
                        newAnnotations.add(annot);
                    }
                }
            }

            CvObjectDao<CvTopic> cvTopicDao = factory.getCvObjectDao(CvTopic.class);

            if (!hasStartPosition){

                CvTopic startPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_START);

                if (startPosition == null){
                    startPosition = CvObjectUtils.createCvObject(intactTranscript.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_START);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(startPosition);
                }
                Annotation start = new Annotation(startPosition, startToString);
                factory.getAnnotationDao().persist(start);

                intactTranscript.addAnnotation(start);

                newAnnotations.add(start);
            }
            if (!hasEndPosition){

                CvTopic endPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_END);

                if (endPosition == null){
                    endPosition = CvObjectUtils.createCvObject(intactTranscript.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_END);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(endPosition);
                }
                Annotation end = new Annotation(endPosition, endToString);
                factory.getAnnotationDao().persist(end);

                intactTranscript.addAnnotation(end);

                newAnnotations.add(end);
            }
        }

        // write new annotations in the report
        if (!newAnnotations.isEmpty()){
            hasBeenUpdated = true;

            evt.addNewAnnotationReport(intactTranscript.getAc(), newAnnotations);
        }

        // Persist changes
        if (hasBeenUpdated){
            DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
            ProteinDao pdao = daoFactory.getProteinDao();
            pdao.update( ( ProteinImpl ) intactTranscript );
        }
    }

    private void sequenceChanged(Protein protein, String uniprot, String newSequence, String oldSequence, String crc64, DataContext context) {
        double relativeConservation = 0;

        if (oldSequence != null && newSequence != null){
            relativeConservation = ProteinTools.calculateSequenceConservation(oldSequence, newSequence);
        }
        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, context, protein, uniprot, oldSequence, newSequence, crc64, relativeConservation));
    }

    private void proteinCreated(Protein protein, DataContext context, String message, String uniprot) {
        ProteinEvent evt = new ProteinEvent(processor, context, protein, message);
        evt.setUniprotIdentity(uniprot);
        processor.fireOnProteinCreated(evt);
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

    /**
     * Create a simple protein in view of updating it.
     * <p/>
     * It should contain the following elements: Shortlabel, Biosource and UniProt Xrefs.
     *
     * @param uniprotProtein the Uniprot protein we are going to build the intact on from.
     *
     * @return a non null, persisted intact protein.
     */
    private Protein createMinimalisticProtein(UniprotProtein uniprotProtein, DataContext context) throws BioSourceServiceException, IntactTransactionException {

        if (uniprotProtein == null) {
            throw new NullPointerException("Passed a null UniprotProtein");
        }

        if (bioSourceService == null) {
            throw new IllegalStateException("BioSourceService should not be null");
        }

        DaoFactory daoFactory = context.getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();

        if (uniprotProtein.getOrganism() == null) {
            throw new IllegalStateException("Uniprot protein without organism: " + uniprotProtein);
        }

        BioSource biosource = bioSourceService.getBiosourceByTaxid(String.valueOf(uniprotProtein.getOrganism().getTaxid()));

        ProteinImpl protein = new ProteinImpl(CvHelper.getInstitution(),
                biosource,
                generateProteinShortlabel(uniprotProtein),
                CvHelper.getProteinType());
        protein.setSequence(uniprotProtein.getSequence());
        protein.setCrc64(uniprotProtein.getCrc64());

        pdao.persist(protein);

        // Create UniProt Xrefs
        // We delegate the creation of UniProt Xrefs to updateProtein() it is call always after createMinimalisticProtein
        // XrefUpdaterUtils.updateUniprotXrefs(protein, uniprotProtein, context);

        pdao.update(protein);
        return protein;

    }

    public static String generateProteinShortlabel( UniprotProtein uniprotProtein ) {

        String name;

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
                                                         DataContext context
    ) {

        DaoFactory daoFactory = context.getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();

        ProteinImpl variant = new ProteinImpl( CvHelper.getInstitution(),
                masterBiosource,
                uniprotProteinTranscript.getPrimaryAc().toLowerCase(),
                CvHelper.getProteinType() );

        if (uniprotProteinTranscript.getSequence() != null) {
            variant.setSequence(uniprotProteinTranscript.getSequence());
            variant.setCrc64(Crc64.getCrc64(variant.getSequence()));
        } else if (!uniprotProteinTranscript.isNullSequenceAllowed()){
            log.warn("Uniprot splice variant without sequence: "+variant);
        }

        pdao.persist(variant);

        // Create isoform-parent or chain-parent Xref
        CvXrefQualifier isoformParent = CvHelper.getQualifierByMi( uniprotProteinTranscript.getParentXRefQualifier() );
        CvDatabase intact = CvHelper.getDatabaseByMi( CvDatabase.INTACT_MI_REF );
        InteractorXref xref = new InteractorXref( CvHelper.getInstitution(), intact, masterAc, isoformParent );
        variant.addXref( xref );
        daoFactory.getXrefDao().persist(xref);

        // Create UniProt Xrefs
        // We delegate the creation of UniProt Xrefs to updateProteinTranscript() it is call always after createMinimalisticProteinTranscript
        // XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( variant, uniprotProteinTranscript, uniprotProtein, context);

        pdao.update(variant );

        return variant;
    }

    public OutOfDateParticipantFixer getParticipantFixer() {
        return participantFixer;
    }

    public void setParticipantFixer(OutOfDateParticipantFixer participantFixer) {
        this.participantFixer = participantFixer;
    }

    public RangeFixer getRangeFixer() {
        return rangeFixer;
    }

    public void setRangeFixer(RangeFixer rangeFixer) {
        this.rangeFixer = rangeFixer;
    }
}