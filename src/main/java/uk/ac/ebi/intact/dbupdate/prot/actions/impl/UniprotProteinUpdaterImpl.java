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
package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.OutOfDateParticipantFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.referencefilter.IntactCrossReferenceFilter;
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
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.intact.util.protein.utils.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * Updates the current protein in the database, using information from uniprot.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UniprotProteinUpdaterImpl implements UniprotProteinUpdater{

    private static final Log log = LogFactory.getLog( UniprotProteinUpdaterImpl.class );
    public static final String FEATURE_CHAIN_UNKNOWN_POSITION = "?";

    private final int MAX_RETRY_ATTEMPTS = 100;
    private int retryAttempt = 0;

    /**
     * Mapping allowing to specify which database shortlabel correspond to which MI reference.
     */
    private Map<String, String> databaseName2mi = new HashMap<String, String>();

    private ProteinUpdateProcessor processor;
    private RangeFixer rangeFixer;

    /**
     * BioSource service allowing to create new BioSource in the database.
     */
    private BioSourceService bioSourceService;
    private OutOfDateParticipantFixer participantFixer;

    public UniprotProteinUpdaterImpl(TaxonomyService taxonomyService) {
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService(taxonomyService));
        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
        participantFixer = new OutOfDateParticipantFixerImpl();
        rangeFixer = new RangeFixerImpl();
    }

    public UniprotProteinUpdaterImpl() {
        // Build default taxonomy service
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService());
        IntactCrossReferenceFilter intactCrossReferenceFilter = new IntactCrossReferenceFilter();
        databaseName2mi = intactCrossReferenceFilter.getDb2Mi();
        participantFixer = new OutOfDateParticipantFixerImpl();
        rangeFixer = new RangeFixerImpl();
    }

    /**
     * Create or update a protein.
     *
     *
     * @return an up-to-date IntAct protein.
     *
     * @throws ProteinServiceException
     */
    public void createOrUpdateProtein( UpdateCaseEvent evt) throws ProteinServiceException{
        this.processor = (ProteinUpdateProcessor) evt.getSource();
        UniprotProtein uniprotProtein = evt.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<Protein> primaryProteins = new ArrayList<Protein>(evt.getPrimaryProteins());
        Collection<Protein> secondaryProteins = new ArrayList<Protein>(evt.getSecondaryProteins());

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotProtein.getPrimaryAc());

        // TODO returned proteins are not used here
        processProteinCase(uniprotProtein, primaryProteins, secondaryProteins, evt);
    }

    private void processProteinCase(UniprotProtein uniprotProtein, Collection<Protein> primaryProteins, Collection<Protein> secondaryProteins, UpdateCaseEvent evt) throws ProteinServiceException{
        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if ( countPrimary == 0 && countSecondary == 0 ) {
            if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
            Protein protein = createMinimalisticProtein( uniprotProtein, evt.getDataContext() );
            updateProtein( protein, uniprotProtein, evt);

            proteinCreated(protein, evt.getDataContext(), "No Intact master protein existed (or was valid) for the uniprot ac " + uniprotProtein.getPrimaryAc());
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
     * create or update a protein transcript

     * @param masterProtein : the IntAct master protein
     * @return the list of protein transcripts created or updated
     * @throws ProteinServiceException
     */
    public void createOrUpdateIsoform( UpdateCaseEvent caseEvent, Protein masterProtein){
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = new ArrayList<ProteinTranscript>(caseEvent.getPrimaryIsoforms());
        Collection<ProteinTranscript> secondaryProteins = new ArrayList<ProteinTranscript>(caseEvent.getSecondaryIsoforms());

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+caseEvent.getQuerySentToService());

        // TODO returned proteins are not used here
        processIsoform(uniprotProtein, masterProtein, primaryProteins, secondaryProteins, caseEvent);
    }

    public void createOrUpdateFeatureChain( UpdateCaseEvent caseEvent, Protein masterProtein){
        this.processor = (ProteinUpdateProcessor) caseEvent.getSource();

        UniprotProtein uniprotProtein = caseEvent.getProtein();

        // we will assign the proteins to two collections - primary / secondary
        Collection<ProteinTranscript> primaryProteins = new ArrayList<ProteinTranscript>(caseEvent.getPrimaryFeatureChains());

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
     * @throws ProteinServiceException
     */
    private void processIsoform(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, Collection<ProteinTranscript> secondaryProteins, UpdateCaseEvent evt){
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
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), uniprotProtein, evt.getDataContext() );
                    evt.getPrimaryIsoforms().add(new ProteinTranscript(protein, ut));

                    updateProteinTranscript(protein, masterProtein, ut, uniprotProtein, evt);
                    proteinCreated(protein, evt.getDataContext(), "No IntAct splice variant existed (or was valid) for the uniprot ac " + ut.getPrimaryAc());
                    evt.getProteins().add(protein.getAc());
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

    private void processChain(UniprotProtein uniprotProtein, Protein masterProtein, Collection<ProteinTranscript> primaryProteins, UpdateCaseEvent evt){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        for (UniprotProteinTranscript ut : uniprotProtein.getFeatureChains()){
            boolean hasFoundFeatureChain = false;

            for (ProteinTranscript protTrans : primaryProteins){
                if (ut.equals(protTrans.getUniprotVariant())){
                    hasFoundFeatureChain = true;
                    break;
                }
            }

            if (!hasFoundFeatureChain){
                if (!config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions()){
                    if (log.isDebugEnabled()) log.debug( "Could not find IntAct protein by UniProt primary or secondary AC." );
                    Protein protein = createMinimalisticProteinTranscript( ut, masterProtein.getAc(), masterProtein.getBioSource(), uniprotProtein, evt.getDataContext() );
                    evt.getPrimaryFeatureChains().add(new ProteinTranscript(protein, ut));

                    updateProteinTranscript(protein, masterProtein, ut, uniprotProtein, evt);
                    proteinCreated(protein, evt.getDataContext(), "No IntAct feature chain existed (or was valid) for the uniprot ac " + ut.getPrimaryAc());
                    evt.getProteins().add(protein.getAc());
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
    private void updateProtein( Protein protein, UniprotProtein uniprotProtein, UpdateCaseEvent evt){

        // check that both protein carry the same organism information
        if (!UpdateBioSource(protein, uniprotProtein.getOrganism(), evt.getDataContext())){
            evt.getPrimaryProteins().remove(protein);
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

        if (fullname != null && !fullname.equals(protein.getFullName())){
            protein.setFullName( fullname );
        }
        else if (protein.getFullName() != null && !protein.getFullName().equals(fullname)){
            protein.setFullName( fullname );
        }
        // no update done
        else {
            fullname = null;
        }

        String shortLabel = generateProteinShortlabel( uniprotProtein );

        if (shortLabel != null && !shortLabel.equals(protein.getShortLabel())){
            protein.setShortLabel(shortLabel);
        }
        else if (protein.getShortLabel() != null && !protein.getShortLabel().equals(shortLabel)){
            protein.setShortLabel(shortLabel);
        }
        // no update done
        else {
            shortLabel = null;
        }

        if (shortLabel != null || fullname != null){
            ProteinNameUpdateReport nameReport = new ProteinNameUpdateReport(protein.getAc(), shortLabel, fullname);
            evt.addNameUpdaterReport(nameReport);
        }

        // Xrefs -- but UniProt's as they are supposed to be up-to-date at this stage.
        XrefUpdaterReport reports = XrefUpdaterUtils.updateAllXrefs( protein, uniprotProtein, databaseName2mi, evt.getDataContext(), processor );
        evt.addXrefUpdaterReport(reports);

        // Aliases
        AliasUpdateReport aliasReport = AliasUpdaterUtils.updateAllAliases(protein, uniprotProtein, evt.getDataContext(), processor);
        evt.addAliasUpdaterReport(aliasReport);

        // Sequence
        updateProteinSequence(protein, uniprotProtein.getSequence(), uniprotProtein.getCrc64(), evt, protein.getAc());

        // Persist changes
        DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.update( ( ProteinImpl ) protein );
    }

    private boolean UpdateBioSource(Protein protein, Organism organism, DataContext context) {
        // check that both protein carry the same organism information
        BioSource organism1 = protein.getBioSource();

        int t2 = organism.getTaxid();

        if (organism1 == null) {
            if (log.isWarnEnabled()) log.warn("Protein protein does not contain biosource. It will be assigned the Biosource from uniprot: "+organism.getName()+" ("+organism.getTaxid()+")");
            organism1 = new BioSource(protein.getOwner(), organism.getName(), String.valueOf(t2));
            protein.setBioSource(organism1);

            context.getDaoFactory().getBioSourceDao().saveOrUpdate(organism1);
        }

        if ( organism1 != null && !String.valueOf( t2 ).equals( organism1.getTaxId() ) ) {
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, context, "UpdateProteins is trying to modify" +
                    " the BioSource(" + organism1.getTaxId() + "," + organism1.getShortLabel() +  ") of the following protein protein " +
                    getProteinDescription(protein) + " by BioSource( " + t2 + "," +
                    organism.getName() + " ). Changing the organism of an existing protein is a forbidden operation.", UpdateError.organism_conflict_with_uniprot_protein, protein));

            return false;
        }
        return true;
    }

    private void updateProteinSequence(Protein protein, String uniprotSequence, String uniprotCrC64, UpdateCaseEvent evt, String masterProteinAc) {
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
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The sequence of the protein " + protein.getAc() +
                    " is not null but the uniprot entry has a sequence null.", UpdateError.uniprot_sequence_null, protein, uniprotAc));
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

            UniprotProteinTranscript transcriptsWithSameSequence = null;

            if (oldSequence != null){
                transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(protein.getSequence(), evt.getProtein());
            }

            if (transcriptsWithSameSequence != null){
                processor.fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(processor, evt.getDataContext(), protein, evt.getProtein(), transcriptsWithSameSequence.getPrimaryAc()));
            }

            RangeUpdateReport report =  rangeFixer.updateRanges(protein, uniprotSequence, processor, evt.getDataContext());
            Collection<Component> componentsWithRangeConflicts = report.getInvalidComponents().keySet();

            if (!componentsWithRangeConflicts.isEmpty()){

                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                        "The protein " + protein.getAc() + " contains " +
                                componentsWithRangeConflicts.size() + " components with range conflicts.", UpdateError.feature_conflicts, protein, uniprotAc));

                OutOfDateParticipantFoundEvent participantEvent = new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), componentsWithRangeConflicts, protein, evt.getProtein(), evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), evt.getPrimaryFeatureChains(), masterProteinAc);
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvent, false);

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
                                if (fixedProtein.getUniprotVariant() instanceof UniprotSpliceVariant){
                                    evt.getPrimaryIsoforms().add(fixedProtein);
                                }
                                else {
                                    evt.getPrimaryFeatureChains().add(fixedProtein);
                                }
                            }
                        }
                    }

                    updateProteinTranscript(fixedProtein.getProtein(), protein, fixedProtein.getUniprotVariant(), evt.getProtein(), evt);
                }
                // no unisave sequence for splice variants and feature chains so if we have conflicts, it is better to create a no-uniprot protein with the sequence of the moment
                else if (ProteinUtils.isSpliceVariant(protein) || ProteinUtils.isFeatureChain(protein)){
                    fixedProtein = participantFixer.createDeprecatedProtein(participantEvent, true);
                    rangeFixer.processInvalidRanges(protein, evt, uniprotAc, oldSequence, report, fixedProtein, processor, false);
                }
                else{
                    rangeFixer.processInvalidRanges(protein, evt, uniprotAc, oldSequence, report, fixedProtein, processor, true);
                }
            }

            protein.setSequence( sequence );

            // CRC64
            String crc64 = uniprotCrC64;
            if ( protein.getCrc64() == null || !protein.getCrc64().equals( crc64 ) ) {
                log.debug( "CRC64 requires update." );
                protein.setCrc64( crc64 );
            }

            sequenceChanged(protein, sequence, oldSequence, uniprotCrC64, evt.getDataContext());
        }
        else{

            RangeUpdateReport report =  rangeFixer.updateOnlyInvalidRanges(protein, processor, evt.getDataContext());
            Collection<Component> componentsWithRangeConflicts = report.getInvalidComponents().keySet();

            if (!componentsWithRangeConflicts.isEmpty()){

                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                        "The protein " + protein.getAc() + " contains " +
                                componentsWithRangeConflicts.size() + " components with invalid ranges.", UpdateError.feature_conflicts, protein, uniprotAc));

                OutOfDateParticipantFoundEvent participantEvent = new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), componentsWithRangeConflicts, protein, evt.getProtein(), evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), evt.getPrimaryFeatureChains(), masterProteinAc);
                processor.fireOnOutOfDateParticipantFound(participantEvent);

                rangeFixer.processInvalidRanges(protein, evt, uniprotAc, oldSequence, report, null, processor, true);
            }
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
                                             UpdateCaseEvent evt) {

        if (!UpdateBioSource(transcript, uniprotTranscript.getOrganism(), evt.getDataContext())){
            ProteinTranscript transcriptToRemove = null;
            for (ProteinTranscript t : evt.getPrimaryIsoforms()){
                if (t.getProtein().getAc().equals(transcript.getAc())){
                    transcriptToRemove = t;
                }
            }
            if (transcriptToRemove == null){
                for (ProteinTranscript t : evt.getSecondaryIsoforms()){
                    if (t.getProtein().getAc().equals(transcript.getAc())){
                        transcriptToRemove = t;
                    }
                }

                if (transcriptToRemove == null){
                    for (ProteinTranscript t : evt.getPrimaryFeatureChains()){
                        if (t.getProtein().getAc().equals(transcript.getAc())){
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

            return false;
        }

        String shortLabel = uniprotTranscript.getPrimaryAc().toLowerCase();

        if (shortLabel != null && !shortLabel.equals(transcript.getShortLabel())){
            transcript.setShortLabel(shortLabel);
        }
        else if (transcript.getShortLabel() != null && !transcript.getShortLabel().equals(shortLabel)){
            transcript.setShortLabel(shortLabel);
        }
        // no update done
        else {
            shortLabel = null;
        }

        String fullName;
        // we have a feature chain
        if (uniprotTranscript.getDescription() != null){
            fullName = uniprotTranscript.getDescription();
        }
        // we have a splice variant
        else {
            fullName = master.getFullName();
        }

        if (fullName != null && !fullName.equals(transcript.getFullName())){
            transcript.setFullName( fullName );
        }
        else if (transcript.getFullName() != null && !transcript.getFullName().equals(fullName)){
            transcript.setFullName( fullName );
        }
        // no update done
        else {
            fullName = null;
        }

        // add a report if updated shortlabel or fullname
        if (shortLabel != null || fullName != null){
            ProteinNameUpdateReport nameReport = new ProteinNameUpdateReport(transcript.getAc(), shortLabel, fullName);
            evt.addNameUpdaterReport(nameReport);
        }

        // update all Xrefs
        XrefUpdaterReport reports = XrefUpdaterUtils.updateAllProteinTranscriptXrefs( transcript, uniprotTranscript, uniprotProtein, evt.getDataContext(), processor );
        evt.addXrefUpdaterReport(reports);

        // Update Aliases from the uniprot protein aliases
        AliasUpdateReport aliasReport = AliasUpdaterUtils.updateAllAliases( transcript, uniprotTranscript, uniprotProtein, evt.getDataContext(), processor);
        evt.addAliasUpdaterReport(aliasReport);

        // Sequence
        if (uniprotTranscript.getSequence() != null){
            updateProteinSequence(transcript, uniprotTranscript.getSequence(), Crc64.getCrc64(uniprotTranscript.getSequence()), evt, master.getAc());
        }
        else if (uniprotTranscript.isNullSequenceAllowed() && transcript.getSequence() != null){
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), "The feature chain " + transcript.getAc() + " has a sequence not null in IntAct but the sequence in uniprot is null because" +
                    " one of the positions is unknown.", UpdateError.uniprot_sequence_null_intact_sequence_not_null, transcript, uniprotTranscript.getPrimaryAc()));
        }

        // Add IntAct Xref

        // Update Note
        String note = uniprotTranscript.getNote();
        if ( ( note != null ) && ( !note.trim().equals( "" ) ) ) {
            Institution owner = transcript.getOwner();
            DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
            CvObjectDao<CvTopic> cvDao = daoFactory.getCvObjectDao( CvTopic.class );
            CvTopic comment = cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

            if (comment == null) {
                comment = CvObjectUtils.createCvObject(owner, CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
                daoFactory.getCvObjectDao(CvTopic.class).persist(comment);
            }

            Annotation annotation = new Annotation( owner, comment );
            annotation.setAnnotationText( note );
            AnnotationUpdaterUtils.addNewAnnotation( transcript, annotation, evt.getDataContext() );
        }

        // in case the protin transcript is a feature chain, we need to add two annotations containing the end and start positions of the feature chain
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
            for (Annotation annot : transcript.getAnnotations()) {
                if (CvTopic.CHAIN_SEQ_START.equals(annot.getCvTopic().getShortLabel())) {
                    hasStartPosition = true;
                    annot.setAnnotationText(startToString);
                    factory.getAnnotationDao().update(annot);
                }
                else if (CvTopic.CHAIN_SEQ_END.equals(annot.getCvTopic().getShortLabel())) {
                    hasEndPosition = true;
                    annot.setAnnotationText(endToString);
                    factory.getAnnotationDao().update(annot);
                }
            }

            CvObjectDao<CvTopic> cvTopicDao = factory.getCvObjectDao(CvTopic.class);

            if (!hasStartPosition){
                CvTopic startPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_START);

                if (startPosition == null){
                    startPosition = CvObjectUtils.createCvObject(transcript.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_START);
                    evt.getDataContext().getDaoFactory().getCvObjectDao(CvTopic.class).persist(startPosition);
                }
                Annotation start = new Annotation(startPosition, startToString);
                factory.getAnnotationDao().persist(start);

                transcript.addAnnotation(start);
            }
            if (!hasEndPosition){
                CvTopic endPosition = cvTopicDao.getByShortLabel(CvTopic.CHAIN_SEQ_END);

                if (endPosition == null){
                    endPosition = CvObjectUtils.createCvObject(transcript.getOwner(), CvTopic.class, null, CvTopic.CHAIN_SEQ_END);
                    evt.getDataContext().getDaoFactory().getCvObjectDao(CvTopic.class).persist(endPosition);
                }
                Annotation end = new Annotation(endPosition, endToString);
                factory.getAnnotationDao().persist(end);

                transcript.addAnnotation(end);
            }
        }

        // Persist changes
        DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
        ProteinDao pdao = daoFactory.getProteinDao();
        pdao.update( ( ProteinImpl ) transcript );

        return true;
    }

    private void sequenceChanged(Protein protein, String newSequence, String oldSequence, String crc64, DataContext context) {
        double relativeConservation = 0;

        if (oldSequence != null && newSequence != null){
            relativeConservation = ProteinTools.calculateSequenceConservation(oldSequence, newSequence);
        }
        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, context, protein, oldSequence, newSequence, crc64, relativeConservation));
    }

    private void proteinCreated(Protein protein, DataContext context, String message) {
        processor.fireOnProteinCreated(new ProteinEvent(processor, context, protein, message));
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
    private Protein createMinimalisticProtein( UniprotProtein uniprotProtein, DataContext context ) throws ProteinServiceException {
        try{
            if (uniprotProtein == null) {
                throw new NullPointerException("Passed a null UniprotProtein");
            }

            if (bioSourceService == null) {
                throw new IllegalStateException("BioSourceService should not be null");
            }

            DaoFactory daoFactory = context.getDaoFactory();
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

            Protein protein = new ProteinImpl( CvHelper.getInstitution(),
                    biosource,
                    generateProteinShortlabel( uniprotProtein ),
                    CvHelper.getProteinType() );
            protein.setSequence(uniprotProtein.getSequence());
            protein.setCrc64(uniprotProtein.getCrc64());

            pdao.persist( ( ProteinImpl ) protein );

            // Create UniProt Xrefs
            XrefUpdaterUtils.updateUniprotXrefs( protein, uniprotProtein, context, processor);

            pdao.update( ( ProteinImpl ) protein );
            return protein;

        }catch( IntactTransactionException e){
            throw new ProteinServiceException(e);
        }

    }

    public static String generateProteinShortlabel( UniprotProtein uniprotProtein ) {

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
                                                         UniprotProtein uniprotProtein,
                                                         DataContext context
    ) {

        DaoFactory daoFactory = context.getDaoFactory();
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
        XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs( variant, uniprotProteinTranscript, uniprotProtein, context, processor);

        pdao.update( ( ProteinImpl ) variant );

        return variant;
    }

    private String getProteinDescription(Protein protein){
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        return "[" + protein.getAc() + ","+ protein.getShortLabel() + "," + uniprotXref.getPrimaryId() + "]";
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
