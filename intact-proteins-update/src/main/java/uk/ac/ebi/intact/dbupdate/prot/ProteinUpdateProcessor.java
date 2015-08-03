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
package uk.ac.ebi.intact.dbupdate.prot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DbInfoDao;
import uk.ac.ebi.intact.dbupdate.prot.actions.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.*;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.LoggingProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.ReportWriterListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.SequenceChangedListener;
import uk.ac.ebi.intact.dbupdate.prot.referencefilter.IntactCrossReferenceFilter;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.meta.DbInfo;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.Set;

/**
 * Updates the database proteins using the latest information from UniProt.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessor extends ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    /**
     * The filter of no-uniprot-update and multi uniprot identities
     */
    protected ProteinUpdateFilter updateFilter;
    /**
     * The updater for the uniprot identity cross references
     */
    protected UniprotIdentityUpdater uniprotIdentityUpdater;
    /**
     * The uniprot protein retriever
     */
    protected UniprotProteinRetriever uniprotRetriever;

    /**
     * The duplicate fixer
     */
    protected DuplicatesFixer duplicateFixer;
    /**
     * The protein deleter
     */
    protected ProteinDeleter proteinDeleter;
    /**
     * The deleter of proteins without interactions
     */
    protected ProtWithoutInteractionDeleter protWithoutInteractionDeleter;

    protected OutOfDateParticipantFixer participantFixer;
    protected UniprotProteinUpdater updater;
    protected IntactTranscriptParentUpdater parentUpdater;
    protected UniprotProteinMapper proteinMappingManager;
    protected DeadUniprotProteinFixer deadUniprotProteinFixer;
    protected DuplicatesFinder duplicatesFinder;
    protected RangeFixer rangeFixer;

    public ProteinUpdateProcessor(){
        super();
        initDefaultActionsAndListeners();
    }

    public void initDefaultActionsAndListeners(){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        // uses a filter in cross references
        config.getUniprotService().setCrossReferenceSelector(new IntactCrossReferenceFilter());

        this.proteinDeleter = new ProteinDeleterImpl();
        this.proteinMappingManager = new UniprotProteinMapperImpl(config.getUniprotService());
        this.deadUniprotProteinFixer = new DeadUniprotProteinFixerImpl();
        this.uniprotIdentityUpdater = new UniprotIdentityUpdaterImpl();
        this.duplicatesFinder = new DuplicatesFinderImpl();
        this.rangeFixer = new RangeFixerImpl();
        this.protWithoutInteractionDeleter = new ProtWithoutInteractionDeleterImpl();
        this.parentUpdater = new IntactTranscriptParentUpdaterImpl();

        this.participantFixer = new OutOfDateParticipantFixerImpl(this.rangeFixer);
        this.updateFilter = new ProteinUpdateFilterImpl(proteinMappingManager);
        this.uniprotRetriever = new UniprotProteinRetrieverImpl(config.getUniprotService(), this.proteinMappingManager, this.deadUniprotProteinFixer);
        this.duplicateFixer = new DuplicatesFixerImpl(this.proteinDeleter, this.participantFixer, this.duplicatesFinder);
        this.updater = new UniprotProteinUpdaterImpl(config.getTaxonomyService(), this.participantFixer);

        registerListeners();
    }

    public ProteinUpdateProcessor(ProteinUpdateProcessorConfig configUpdate){
        this();
        ProteinUpdateContext.getInstance().setConfig( configUpdate );
        initDefaultActionsAndListeners();
    }

    @Override
    public void updateAll() throws ProcessorException {

        boolean global = ProteinUpdateContext.getInstance().getConfig().isGlobalProteinUpdate();
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( true );

        super.updateAll();

        // close the cache
        this.uniprotRetriever.getUniprotService().close();

        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( global );

        // update db info accordingly
        String lastProtUpdate = new SimpleDateFormat("dd-MMM-yy").format(new Date());

        saveOrUpdateDbInfo("last_protein_update", lastProtUpdate);
        saveOrUpdateDbInfo("uniprotkb.version", UniProtJAPI.factory.getVersion());

        List<ReportWriterListener> writers = getListeners(ReportWriterListener.class);

        for (ReportWriterListener listener : writers){
            UpdateReportHandler handler = listener.getReportHandler();

            if (handler != null){
                try {
                    handler.close();
                } catch (IOException e) {
                    throw new ProcessorException("Impossible to close one of the log files." ,e);
                }
            }
        }
    }

    private void saveOrUpdateDbInfo(String key, String value) {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final TransactionStatus transactionStatus = dataContext.beginTransaction();

        DbInfoDao dbInfoDao = dataContext.getDaoFactory().getDbInfoDao();
        DbInfo dbInfo = dbInfoDao.get(key);

        if (dbInfo == null) {
            dbInfo = new DbInfo(key, value);
            dbInfoDao.persist(dbInfo);
        } else {
            dbInfo.setValue(value);
            dataContext.getDaoFactory().getEntityManager().merge(dbInfo);
        }

        dataContext.commitTransaction(transactionStatus);
    }

    protected void registerListeners() {
        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        addListener(new LoggingProcessorListener());

        if (config.getReportHandler() != null) {
            addListener(new ReportWriterListener(config.getReportHandler()));
        }

        addListener(new SequenceChangedListener());
    }

    public void fireOnDelete(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDelete(evt);
        }
    }

    public void fireOnProteinDuplicationFound(DuplicatesFoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinDuplicationFound(evt);
        }
    }

    public void fireOnProteinSequenceChanged(ProteinSequenceChangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinSequenceChanged(evt);
        }
    }

    public void fireOnProteinCreated(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinCreated(evt);
        }
    }

    public void fireNonUniprotProteinFound(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onNonUniprotProteinFound(evt);
        }
    }

    public void fireOnUpdateCase(UpdateCaseEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onUpdateCase(evt);
        }
    }

    public void fireOnRangeChange(RangeChangedEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onRangeChanged(evt);
        }
    }

    public void fireOnInvalidRange(InvalidRangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onInvalidRange(evt);
        }
    }

    public void fireOnOutOfDateRange(InvalidRangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onOutOfDateRange(evt);
        }
    }

    public void fireOnUniprotDeadEntry(DeadUniprotEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDeadProteinFound(evt);
        }
    }

    public void fireOnOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onOutOfDateParticipantFound(evt);
        }
    }

    public void fireOnSecondaryAcsFound(UpdateCaseEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onSecondaryAcsFound(evt);
        }
    }

    public void fireOnProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinTranscriptWithSameSequence(evt);
        }
    }

    public void fireOnInvalidIntactParentFound(InvalidIntactParentFoundEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onInvalidIntactParent(evt);
        }
    }

    public void fireOnProteinToBeRemapped(ProteinRemappingEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinRemapping(evt);
        }
    }

    public void fireOnProteinSequenceCaution(ProteinSequenceChangeEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinSequenceCaution(evt);
        }
    }

    public void fireOnDeletedComponent(DeletedComponentEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDeletedComponent(evt);
        }
    }

    @Override
    public List<Protein> retrieveAndUpdateProteinFromUniprot(String uniprotAc) throws ProcessorException{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setBlastEnabled(false);

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        List<Protein> intactProteins = super.retrieveAndUpdateProteinFromUniprot(uniprotAc);

        // register the listeners
        if (getListeners(SequenceChangedListener.class) == null){
            addListener(new SequenceChangedListener());
        }

        if (uniprotAc == null){
            throw new ProcessorException("The uniprot ac should not be null");
        }

        // get the uniprot protein
        UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(uniprotAc);

        // if the uniprot protein exists, start to update
        if (uniprotProtein != null){
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                if (log.isTraceEnabled()) log.trace("Retrieving all intact proteins matcing the uniprot entry : "+uniprotAc);

                ProteinEvent processEvent = new ProteinEvent(this, context, null);
                processEvent.setUniprotIdentity(uniprotAc);

                UpdateCaseEvent caseEvent = runProteinUpdate(uniprotProtein, processEvent, null);

                if (caseEvent != null){
                    intactProteins.addAll(caseEvent.getPrimaryProteins());
                    intactProteins.addAll(caseEvent.getSecondaryProteins());

                    for (ProteinTranscript pt : caseEvent.getPrimaryIsoforms()){
                        intactProteins.add(pt.getProtein());
                    }
                    for (ProteinTranscript pt : caseEvent.getSecondaryIsoforms()){
                        intactProteins.add(pt.getProtein());
                    }
                    for (ProteinTranscript pt : caseEvent.getPrimaryFeatureChains()){
                        intactProteins.add(pt.getProtein());
                    }
                }

                context.commitTransaction(status);
            }  catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(null, uniprotAc, e);
                fireOnProcessErrorFound(new UpdateErrorEvent(this, context, fatalError, uniprotAc));
                if (!status.isCompleted()){
                    context.rollbackTransaction(status);
                }
            }
        }
        else {
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                List<ProteinImpl> deadProteins = IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByUniprotId(uniprotAc);

                if (!deadProteins.isEmpty()){
                    for (ProteinImpl prot : deadProteins){
                        ProteinEvent evt = new ProteinEvent(this, context, prot);

                        uniprotRetriever.retrieveUniprotEntry(evt);
                    }
                }

                context.commitTransaction(status);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(null, uniprotAc, e);
                fireOnProcessErrorFound(new UpdateErrorEvent(this, context, fatalError, uniprotAc));
                if (!status.isCompleted()){
                    context.rollbackTransaction(status);
                }
            }
        }

        List<ReportWriterListener> writers = getListeners(ReportWriterListener.class);

        for (ReportWriterListener listener : writers){
            UpdateReportHandler handler = listener.getReportHandler();

            if (handler != null){
                try {
                    handler.close();
                } catch (IOException e) {
                    throw new ProcessorException("Impossible to close one of the log files." ,e);
                }
            }
        }

        config.getUniprotService().close();

        return intactProteins;
    }

    @Override
    public Set<String> update(Protein protToUpdate, DataContext dataContext){
        // register the listeners
        registerListenersIfNotDoneYet();

        // the proteins processed during this update
        Set<String> processedProteins = super.update(protToUpdate, dataContext);
        // the current config
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        String uniprotIdentity = null;

        try{
            // the protein to update
            this.currentProtein = protToUpdate;

            // add the protein to the list of processed proteins
            processedProteins.add(protToUpdate.getAc());

            // create the event for this protein
            ProteinEvent processEvent = new ProteinEvent(this, dataContext, protToUpdate);

            // to know if the protein should be deleted
            boolean toDelete = false;

            InteractorXref uniprotIdentityXref = ProteinUtils.getUniprotXref(protToUpdate);
            String uniprot = uniprotIdentityXref != null ? uniprotIdentityXref.getPrimaryId() : null;

            processEvent.setUniprotIdentity(uniprot);

            // if we delete proteins without interactions
            if (config.isDeleteProtsWithoutInteractions()){
                if (log.isTraceEnabled()) log.trace("Checking for protein interactions : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

                // true if the protein is not involved in any interactions
                toDelete = protWithoutInteractionDeleter.hasToBeDeleted(processEvent);
            }

            // if the protein must be deleted, delete it
            if (toDelete){
                boolean isDeletedFromDatabase = proteinDeleter.delete(processEvent);

                if (!isDeletedFromDatabase){

                    ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleToDeleteError(protToUpdate.getShortLabel(), "The protein " + protToUpdate.getShortLabel() + " cannot be deleted because doesn't have any intact ac.");

                    fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, protToUpdate, uniprot));
                }
            }
            // the protein must not be deleted, update it
            else {
                if (log.isTraceEnabled()) log.trace("Filtering protein : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+") for uniprot update");

                boolean canBeUpdated = true;

                // get the list of protein isoforms or feature chains in intact matching this uniprot entry but without any parents attached to it
                // remove from the list of transcripts to update :
                // - transcripts with dead parent impossible to remap
                // - transcript with several parents
                // - transcripts with both feature chain and isoform parents
                List<Protein> transcriptsWithoutParents = new ArrayList<Protein>();

                // case of splice variant or feature chain, check if the parent cross references are consistent, otherwise don't update
                // - cannot be updated if is attached to several parents
                if (ProteinUtils.isFeatureChain(protToUpdate) || ProteinUtils.isSpliceVariant(protToUpdate)){
                    canBeUpdated = parentUpdater.checkConsistencyProteinTranscript(processEvent, transcriptsWithoutParents);
                }

                if (canBeUpdated){
                    // get the uniprot identity of this protein
                    uniprotIdentity = updateFilter.filterOnUniprotIdentity(processEvent);

                    // if the protein has a uniprot identity and is not 'no-uniprot-update'
                    if (uniprotIdentity != null){
                        if (log.isTraceEnabled()) log.trace("Retrieving uniprot entry matching the protein : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+"), "+uniprotIdentity+"");
                        processEvent.setUniprotIdentity(uniprotIdentity);

                        // get the uniprot protein
                        UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(processEvent);

                        // if the uniprot protein exists, start to update
                        if (uniprotProtein != null){

                            if (log.isTraceEnabled()) log.trace("Retrieving all intact proteins matcing the uniprot entry : "+uniprotIdentity);

                            UpdateCaseEvent caseEvent = runProteinUpdate(uniprotProtein, processEvent, transcriptsWithoutParents);

                            if (caseEvent != null){
                                // add each protein to the list of processed proteins
                                processedProteins.addAll(caseEvent.getProteins());
                            }
                        }
                        else if (!transcriptsWithoutParents.isEmpty()){
                            ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleParentTranscriptToReviewError(protToUpdate.getAc(), "The protein transcript cannot be reviewed because we cannot retrieve a single uniprot entry matching " + uniprotIdentity);
                            fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, protToUpdate, uniprotIdentity));
                        }
                    }
                    else if (!transcriptsWithoutParents.isEmpty()){
                        ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleParentTranscriptToReviewError(protToUpdate.getAc(), "The protein transcript cannot be reviewed because it does not have a valid uniprot identity.");
                        fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, protToUpdate));
                    }
                }
            }

        }
        catch (Exception e) {
            log.fatal("We failed to update the protein " + protToUpdate.getAc(), e);

            ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(protToUpdate.getAc(), uniprotIdentity, e);
            fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, fatalError, protToUpdate, uniprotIdentity));
        }

        return processedProteins;
    }

    private UpdateCaseEvent runProteinUpdate(UniprotProtein uniprotProtein, ProteinEvent processEvent, List<Protein> transcriptToReview){
        // the current config
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // if the uniprot protein exists, start to update
        if (uniprotProtein != null){
            // set the uniprot protein of the event
            processEvent.setUniprotProtein(uniprotProtein);

            // if the uniprot identity is null, set the uniprot identity to the primary ac of the uniprot entry.
            // it is possible that the uniprot identity is different that th uniprot primary ac, it depends which protein we were updating first (
            // could be secondary ac, isoform ac, feature chain id, etc.)
            if (processEvent.getUniprotIdentity() == null){
                processEvent.setUniprotIdentity(uniprotProtein.getPrimaryAc());
            }

            if (log.isTraceEnabled()) log.trace("Retrieving all intact proteins matcing the uniprot entry : "+processEvent.getUniprotIdentity());

            // get all the proteins in intact attached to this uniprot entry without non uniprot :
            // - all intact proteins with uniprot identity = uniprot primary ac => primary proteins
            // - all intact proteins with uniprot identity = one of the uniprot secondary acs => secondary proteins
            // - For each primary and secondary proteins, collect all splice variants and feature chains attached to it
            //     - primary isoforms : each primary isoform matches a primary ac of a uniprot splice variant
            //     - secondary isoforms : each secondary isoform matches a secondary ac of a uniprot splice variant
            //     - primary feature chains : each primary feature chain matches a primary ac of a uniprot feature chain
            UpdateCaseEvent caseEvent = uniprotIdentityUpdater.collectPrimaryAndSecondaryProteins(processEvent);

            // if we can delete proteins without interactions, delete all of the proteins attached to this uniprot entry without interactions
            if (config.isDeleteProtsWithoutInteractions()){
                if (log.isTraceEnabled()) log.trace("Checking for all protein interactions");

                // get the list of proteins to delete. All these proteins have been removed from the UpdateCaseEvent and will not be updated
                Set<Protein> protToDelete = protWithoutInteractionDeleter.collectAndRemoveProteinsWithoutInteractions(caseEvent);

                // delete these proteins
                for (Protein p : protToDelete){
                    ProteinEvent protEvent = new ProteinEvent(this, caseEvent.getDataContext(), p, uniprotProtein, "Protein without interactions");

                    boolean isDeletedFromDatabase = proteinDeleter.delete(protEvent);

                    if (!isDeletedFromDatabase){

                        ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleToDeleteError(p.getShortLabel(), "The protein " + p.getShortLabel() + " cannot be deleted because doesn't have any intact ac.");
                        fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleToDeleteEvent, p, processEvent.getUniprotIdentity()));

                    }
                }
            }

            // get the list of protein isoforms or feature chains in intact matching this uniprot entry but without any parents attached to it
            // remove from the list of transcripts to update :
            // - transcripts with dead parent impossible to remap
            // - transcript with several parents
            // - transcripts with both feature chain and isoform parents
            if (transcriptToReview != null){
                transcriptToReview.addAll(parentUpdater.checkConsistencyOfAllTranscripts(caseEvent));
            }
            else {
                transcriptToReview = parentUpdater.checkConsistencyOfAllTranscripts(caseEvent);
            }

            if (log.isTraceEnabled()) log.trace("Filtering " + caseEvent.getPrimaryProteins().size() + " primary proteins and " + caseEvent.getSecondaryProteins().size() + "secondary proteins for uniprot update." );

            // filter on 'no-uniprot-update' and multi identities, remove them from the list of proteins to update (can happen if proteins with uniprot identity and 'no-uniprot-update')
            updateFilter.filterNonUniprotAndMultipleUniprot(caseEvent);

            if (log.isTraceEnabled()) log.trace("Checking that it is possible to update existing secondary proteins for " + uniprotProtein.getPrimaryAc() );

            // filter on the proteins matching a single uniprot protein :
            // - all secondary proteins must match a single uniprot entry otherwise are removed from the list of proteins to update
            // - all isoforms/feature chains which doesn't have a 'no-uniprot-uptate' and doesn't match any uniprot transcript of this uniprot entry
            // are dead proteins removed from the list of proteins to update
            // - all isoform/feature chains which can be attached to several uniprot entries and which do not have a main entry
            uniprotRetriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(caseEvent);

            // secondary acs to update : after updating uniprot identity all secondary proteins are moved to primary proteins.
            // in case of organism conflicts, the secondary protein is not updated
            uniprotIdentityUpdater.updateAllSecondaryProteins(caseEvent);

            // the master protein in IntAct
            Protein masterProtein = null;

            // if there are some duplicates and we can fix them, merge them
            // if fixing protein duplicate is enabled, fix them
            if (config.isFixDuplicates()){
                if (log.isTraceEnabled()) log.trace("Check for possible duplicates." );

                // return the master protein which is the result of the merge if there is one. Returns null if there were no duplicated proteins
                // or if it was impossible to have an original protein after the merge
                masterProtein = duplicateFixer.fixAllProteinDuplicates(caseEvent);
            }

            // update master proteins first
            try{
                updater.createOrUpdateProtein(caseEvent);
            }
            catch (ProteinServiceException e){
                ProteinUpdateError impossibleUpdate = errorFactory.createImpossibleUpdateMasterError("The master proteins for the uniprot entry " + caseEvent.getProtein().getPrimaryAc() + " couldn't be updated because of a biosource service problem when creating a new protein", caseEvent.getProtein().getPrimaryAc());
                fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleUpdate, caseEvent.getProtein().getPrimaryAc()));
            }


            // it is possible to update protein transcripts only if a master protein is available :
            // - a master protein not nul which is the result of a merge
            // - a master protein which is the unique protein in primary proteins
            // if master protein is null and the list of primary proteins contains more than one protein, it is impossible to update the transcripts because no parent available
            boolean canUpdateProteinTranscript = false;

            // master protein null because no merge done before and number of primary proteins = 1 : the master protein exists
            if (masterProtein == null && caseEvent.getPrimaryProteins().size() == 1){
                masterProtein = caseEvent.getPrimaryProteins().iterator().next();
                canUpdateProteinTranscript = true;
            }
            // master protein null because no merge done before and number of primary proteins != 1 : the master protein is impossible to decide
            else if (masterProtein == null && caseEvent.getPrimaryProteins().size() != 1){
                canUpdateProteinTranscript = false;
            }
            // a merge has been done, the master protein is the result of the merge
            else {
                canUpdateProteinTranscript = true;
            }

            boolean needTranscriptUpdate = (caseEvent.getPrimaryIsoforms().size() >= 1 ||
                    caseEvent.getSecondaryIsoforms().size() >= 1 || caseEvent.getPrimaryFeatureChains().size() >= 1) ||
                    (caseEvent.getPrimaryIsoforms().isEmpty() && caseEvent.getSecondaryIsoforms().isEmpty() &&
                            caseEvent.getPrimaryFeatureChains().isEmpty() && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions());

            // if a single master protein has been found and transcript update is needed
            if (needTranscriptUpdate){
                if (canUpdateProteinTranscript){
                    // add first a parent xref for all the protein transcripts without parent xref if it is necessary
                    if (!transcriptToReview.isEmpty()){
                        parentUpdater.createParentXRefs(transcriptToReview, masterProtein, uniprotProtein.getPrimaryAc(), caseEvent.getDataContext(), this);
                    }

                    //protein transcript duplicates to merge
                    // fixing duplicates is enabled
                    if (config.isFixDuplicates()){
                        if (log.isTraceEnabled()) log.trace("Check for possible transcript duplicates." );

                        duplicateFixer.fixAllProteinTranscriptDuplicates(caseEvent, masterProtein);
                    }

                    // update isoforms if necessary
                    if (!caseEvent.getPrimaryIsoforms().isEmpty() || (caseEvent.getPrimaryIsoforms().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())){
                        updater.createOrUpdateIsoform(caseEvent, masterProtein);
                    }

                    // update chains if necessary
                    if (!caseEvent.getPrimaryFeatureChains().isEmpty() || (caseEvent.getPrimaryFeatureChains().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())){
                        updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                    }
                }
                else {
                    ProteinUpdateError impossibleUpdate = errorFactory.createImpossibleTranscriptUpdateError("No master protein or several master proteins exist and it is impossible to update the protein transcripts", uniprotProtein.getPrimaryAc());
                    fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleUpdate, uniprotProtein.getPrimaryAc()));
                }
            }

            // log in updated.csv
            boolean hasBeenUpdated = false;
            if (!caseEvent.getNameUpdaterReports().isEmpty()){
                hasBeenUpdated = true;
            }
            else if (!caseEvent.getAliasUpdaterReports().isEmpty()){
                hasBeenUpdated = true;
            }
            else if (!caseEvent.getXrefUpdaterReports().isEmpty()){
                hasBeenUpdated = true;
            }
            else if (!caseEvent.getNewAnnotations().isEmpty()){
                hasBeenUpdated = true;
            }
            else if (!caseEvent.getUpdatedRanges().isEmpty()){
                hasBeenUpdated = true;
            }

            if (hasBeenUpdated){
                fireOnUpdateCase(caseEvent);
            }

            return caseEvent;
        }

        return null;
    }

    public ProteinUpdateFilter getUpdateFilter() {
        return updateFilter;
    }

    public void setUpdateFilter(ProteinUpdateFilter updateFilter) {
        this.updateFilter = updateFilter;
    }

    public UniprotIdentityUpdater getUniprotIdentityUpdater() {
        return uniprotIdentityUpdater;
    }

    public void setUniprotIdentityUpdater(UniprotIdentityUpdater uniprotIdentityUpdater) {
        this.uniprotIdentityUpdater = uniprotIdentityUpdater;
    }

    public UniprotProteinRetriever getUniprotRetriever() {
        return uniprotRetriever;
    }

    public void setUniprotRetriever(UniprotProteinRetriever uniprotRetriever) {
        this.uniprotRetriever = uniprotRetriever;
    }

    public DuplicatesFixer getDuplicateFixer() {
        return duplicateFixer;
    }

    public void setDuplicateFixer(DuplicatesFixer duplicateFixer) {
        this.duplicateFixer = duplicateFixer;
    }

    public ProteinDeleter getProteinDeleter() {
        return proteinDeleter;
    }

    public void setProteinDeleter(ProteinDeleter proteinDeleter) {
        this.proteinDeleter = proteinDeleter;
    }

    public ProtWithoutInteractionDeleter getProtWithoutInteractionDeleter() {
        return protWithoutInteractionDeleter;
    }

    public void setProtWithoutInteractionDeleter(ProtWithoutInteractionDeleter protWithoutInteractionDeleter) {
        this.protWithoutInteractionDeleter = protWithoutInteractionDeleter;
    }

    public OutOfDateParticipantFixer getParticipantFixer() {
        return participantFixer;
    }

    public void setParticipantFixer(OutOfDateParticipantFixer participantFixer) {
        this.participantFixer = participantFixer;
    }

    public UniprotProteinUpdater getUpdater() {
        return updater;
    }

    public void setUpdater(UniprotProteinUpdater updater) {
        this.updater = updater;
    }

    public IntactTranscriptParentUpdater getParentUpdater() {
        return parentUpdater;
    }

    public void setParentUpdater(IntactTranscriptParentUpdater parentUpdater) {
        this.parentUpdater = parentUpdater;
    }

    public UniprotProteinMapper getProteinMappingManager() {
        return proteinMappingManager;
    }

    public void setProteinMappingManager(UniprotProteinMapper proteinMappingManager) {
        this.proteinMappingManager = proteinMappingManager;
    }

    public DeadUniprotProteinFixer getDeadUniprotProteinFixer() {
        return deadUniprotProteinFixer;
    }

    public void setDeadUniprotProteinFixer(DeadUniprotProteinFixer deadUniprotProteinFixer) {
        this.deadUniprotProteinFixer = deadUniprotProteinFixer;
    }

    public DuplicatesFinder getDuplicatesFinder() {
        return duplicatesFinder;
    }

    public void setDuplicatesFinder(DuplicatesFinder duplicatesFinder) {
        this.duplicatesFinder = duplicatesFinder;
    }

    public RangeFixer getRangeFixer() {
        return rangeFixer;
    }

    public void setRangeFixer(RangeFixer rangeFixer) {
        this.rangeFixer = rangeFixer;
    }
}