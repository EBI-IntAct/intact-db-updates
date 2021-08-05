package uk.ac.ebi.intact.dbupdate.prot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DbInfoDao;
import uk.ac.ebi.intact.dbupdate.prot.actions.deleters.ProtWithoutInteractionDeleter;
import uk.ac.ebi.intact.dbupdate.prot.actions.deleters.ProteinDeleter;
import uk.ac.ebi.intact.dbupdate.prot.actions.filters.ProteinUpdateFilter;
import uk.ac.ebi.intact.dbupdate.prot.actions.finders.DuplicatesFinder;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.DeadUniprotProteinFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.DuplicatesFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.OutOfDateParticipantFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.mappers.UniprotProteinMapper;
import uk.ac.ebi.intact.dbupdate.prot.actions.retrievers.UniprotProteinRetriever;
import uk.ac.ebi.intact.dbupdate.prot.actions.updaters.IntactTranscriptParentUpdater;
import uk.ac.ebi.intact.dbupdate.prot.actions.updaters.UniprotIdentityUpdater;
import uk.ac.ebi.intact.dbupdate.prot.actions.updaters.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.LoggingProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.ReportWriterListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.SequenceChangedListener;
import uk.ac.ebi.intact.dbupdate.prot.model.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.meta.DbInfo;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.DefaultCrossReferenceFilter;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceException;
import uk.ac.ebi.uniprot.dataservice.client.Client;
import uk.ac.ebi.uniprot.dataservice.client.exception.ServiceException;
import uk.ac.ebi.uniprot.dataservice.client.uniprot.UniProtService;

import javax.swing.event.EventListenerList;
import java.io.IOException;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Updates the database proteins using the latest information from UniProt.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessor {

    private static final Log log = LogFactory.getLog(ProteinUpdateProcessor.class);

    protected int COMMIT_INTERVAL = 50;

    // to allow listener
    private final EventListenerList listenerList = new EventListenerList();

    /**
     * The filter of no-uniprot-update and multi uniprot identities
     */
    private ProteinUpdateFilter updateFilter;

    /**
     * The updater for the uniprot identity cross references
     */
    private UniprotIdentityUpdater uniprotIdentityUpdater;

    /**
     * The uniprot protein retriever
     */
    private UniprotProteinRetriever uniprotRetriever;

    /**
     * The duplicate fixer
     */
    private DuplicatesFixer duplicateFixer;

    /**
     * The protein deleter
     */
    private ProteinDeleter proteinDeleter;

    /**
     * The deleter of proteins without interactions
     */
    private ProtWithoutInteractionDeleter protWithoutInteractionDeleter;

    private UniprotProteinUpdater updater;
    private IntactTranscriptParentUpdater parentUpdater;

    private ProteinUpdateProcessorConfig config;

    public ProteinUpdateProcessor() {
        initDefaultActionsAndListeners();
    }

    public ProteinUpdateProcessor(ProteinUpdateProcessorConfig configUpdate) {
        ProteinUpdateContext.getInstance().setConfig(configUpdate);
        initDefaultActionsAndListeners();
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

    public void fireOnOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onOutOfDateParticipantFound(evt);
        }
    }

    public void fireOnSecondaryAcsFound(UpdateCaseEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onSecondaryAcsFound(evt);
        }
    }

    public void fireOnProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinTranscriptWithSameSequence(evt);
        }
    }

    public void fireOnProcessErrorFound(UpdateErrorEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProcessErrorFound(evt);
        }
    }

    public void fireOnInvalidIntactParentFound(InvalidIntactParentFoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onInvalidIntactParent(evt);
        }
    }

    public void fireOnProteinToBeRemapped(ProteinRemappingEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinRemapping(evt);
        }
    }

    public void fireOnProteinSequenceCaution(ProteinSequenceChangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinSequenceCaution(evt);
        }
    }

    public void fireOnDeletedComponent(DeletedComponentEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDeletedComponent(evt);
        }
    }

    // listener methods
    public void addListener(ProteinUpdateProcessorListener listener) {
        listenerList.add(ProteinUpdateProcessorListener.class, listener);
    }

    public void removeListener(ProteinUpdateProcessorListener listener) {
        listenerList.remove(ProteinUpdateProcessorListener.class, listener);
    }

    /**
     * Updates all the proteins in the database.
     * It calls updateByAc with the list of every Protein ac in IntAct
     *
     * @throws ProcessorException
     */
    public void updateAll() throws ProcessorException {

        config.setGlobalProteinUpdate(true);

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        TransactionStatus transactionStatus = dataContext.beginTransaction();
        List<String> acs = dataContext.getDaoFactory().getEntityManager()
                .createQuery("select p.ac from ProteinImpl p order by p.created").getResultList();
        dataContext.commitTransaction(transactionStatus);

        updateByACs(acs);

        // close the cache
        this.uniprotRetriever.getUniprotService().close();

        // update db info accordingly
        String lastProtUpdate = new SimpleDateFormat("dd-MMM-yy").format(new Date());

        saveOrUpdateDbInfo("last_protein_update", lastProtUpdate);
        UniProtService uniprotService = Client.getServiceFactoryInstance().getUniProtQueryService();
        try {
            uniprotService.start();
            saveOrUpdateDbInfo("uniprotkb.version", uniprotService.getServiceInfo().getReleaseNumber());
            uniprotService.stop();
        } catch (ServiceException e) {
            uniprotService.stop();
            e.printStackTrace();
        }
    }

    /**
     * Handles the commit interval of the transaction. Currently every 50 proteins updated.
     * It calls internally the update() method.
     * <p>
     * Keeps track of the list of proteins already updated.
     *
     * @param protACsToUpdate list of accession of the proteins to update
     * @throws ProcessorException
     */
    public void updateByACs(List<String> protACsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        Set<String> processedIntactProteins = new HashSet<>();
        Set<String> chunkIntactProteins = new HashSet<>(COMMIT_INTERVAL);
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        Iterator<String> protAcsIterator = protACsToUpdate.iterator();
        int currentIndex = 0;
        ProteinImpl intactProteinToUpdate;
        String protAc;

        while (protAcsIterator.hasNext()) {
            TransactionStatus transactionStatus = dataContext.beginTransaction();
            chunkIntactProteins.clear();

            try {

                while (currentIndex < COMMIT_INTERVAL && protAcsIterator.hasNext()) {
                    protAc = protAcsIterator.next();

                    if (!processedIntactProteins.contains(protAc)) {
                        chunkIntactProteins.add(protAc);

                        intactProteinToUpdate = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

                        if (intactProteinToUpdate != null) {
                            // update returns all the proteins affected during the update process e.g isoforms and post process chains
                            processedIntactProteins.addAll(update(intactProteinToUpdate, dataContext));
                        } else {
                            if (log.isWarnEnabled())
                                log.warn("Protein was not found in the database. Probably it was deleted already? " + protAc);
                            processedIntactProteins.add(protAc);
                        }
                    }
                    currentIndex++;
                }

                dataContext.commitTransaction(transactionStatus);
            } catch (Exception e) {
                if (!transactionStatus.isCompleted()) {
                    for (String ac : chunkIntactProteins) {
                        log.fatal("FATAL: We failed to update the protein " + ac, e);
                        ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(ac, null, e);
                        fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, fatalError, null, ac));
                    }
                    dataContext.rollbackTransaction(transactionStatus);
                }
            }

            currentIndex = 0;
        }

        List<ReportWriterListener> writers = getListeners(ReportWriterListener.class);

        for (ReportWriterListener listener : writers) {
            UpdateReportHandler handler = listener.getReportHandler();

            if (handler != null) {
                try {
                    handler.close();
                } catch (IOException e) {
                    throw new ProcessorException("Impossible to close one of the log files.", e);
                }
            }
        }
    }

    /**
     * Has the logic of the protein update. Internally calls the private method runProteinUpdate
     *
     * @throws ProcessorException
     */
    public Set<String> update(Protein intactProteinToUpdate, DataContext dataContext) {

        // the proteins processed during this update
        Set<String> processedProteins = new HashSet<>();

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        String uniprotIdentity = null;

        try {
            // add the protein to the list of processed proteins
            processedProteins.add(intactProteinToUpdate.getAc());

            // create the event for this protein
            ProteinEvent processEvent = new ProteinEvent(this, dataContext, intactProteinToUpdate);

            // to know if the protein should be deleted
            boolean toDelete = false;

            InteractorXref uniprotIdentityXref = ProteinUtils.getUniprotXref(intactProteinToUpdate);
            String uniprot = uniprotIdentityXref != null ? uniprotIdentityXref.getPrimaryId() : null;

            processEvent.setUniprotIdentity(uniprot);

            // if we delete proteins without interactions
            if (config.isDeleteProtsWithoutInteractions()) {
                if (log.isTraceEnabled())
                    log.trace("Checking for protein interactions : " + intactProteinToUpdate.getShortLabel() + " (" + intactProteinToUpdate.getAc() + ")");

                // true if the protein is not involved in any interactions,
                // TODO: it should take care of the case of parent proteins from (isoforms, post process chains) without interactions
                // for now it is not working
                toDelete = protWithoutInteractionDeleter.hasToBeDeleted(processEvent);
            }

            // if the protein must be deleted, delete it
            if (toDelete) {
                boolean isDeletedFromDatabase = proteinDeleter.delete(processEvent);

                if (!isDeletedFromDatabase) {
                    ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleToDeleteError(intactProteinToUpdate.getShortLabel(),
                            "The protein " + intactProteinToUpdate.getShortLabel() + " cannot be deleted because doesn't have any intact ac.");
                    fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, intactProteinToUpdate, uniprot));
                }
            }
            // the protein must not be deleted, update it
            else {
                if (log.isTraceEnabled())
                    log.trace("Filtering protein : " + intactProteinToUpdate.getShortLabel() + " (" + intactProteinToUpdate.getAc() + ") for uniprot update");

                boolean canBeUpdated = true;

                // get the list of protein isoforms or feature chains in intact matching this uniprot entry but without any parents attached to it
                // remove from the list of transcripts to update :
                // - transcripts with dead parent impossible to remap
                // - transcript with several parents
                // - transcripts with both feature chain and isoform parents
                List<Protein> transcriptsWithoutParents = new ArrayList<>();

                // case of splice variant or feature chain, check if the parent cross references are consistent, otherwise don't update
                // - cannot be updated if is attached to several parents
                if (ProteinUtils.isFeatureChain(intactProteinToUpdate) || ProteinUtils.isSpliceVariant(intactProteinToUpdate)) {
                    canBeUpdated = parentUpdater.checkConsistencyProteinTranscript(processEvent, transcriptsWithoutParents);
                }

                if (canBeUpdated) {
                    // get the uniprot identity of this protein
                    uniprotIdentity = updateFilter.filterOnUniprotIdentity(processEvent);

                    // if the protein has a uniprot identity and is not 'no-uniprot-update'
                    if (uniprotIdentity != null) {
                        if (log.isTraceEnabled())
                            log.trace("Retrieving uniprot entry matching the protein : " + intactProteinToUpdate.getShortLabel() + " (" + intactProteinToUpdate.getAc() + "), " + uniprotIdentity + "");
                        processEvent.setUniprotIdentity(uniprotIdentity);

                        // get the uniprot protein
                        UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(processEvent);

                        // if the uniprot protein exists, start to update
                        if (uniprotProtein != null) {

                            if (log.isTraceEnabled())
                                log.trace("Retrieving all intact proteins matcing the uniprot entry : " + uniprotIdentity);

                            // starts updating; the protein is inside processEvent.getProtein
                            UpdateCaseEvent caseEvent = runProteinUpdate(uniprotProtein, processEvent, transcriptsWithoutParents);

                            if (caseEvent != null) {
                                // add each protein to the list of processed proteins
                                processedProteins.addAll(caseEvent.getProteins());
                            }
                        } else if (!transcriptsWithoutParents.isEmpty()) {
                            ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleParentTranscriptToReviewError(intactProteinToUpdate.getAc(), "The protein transcript cannot be reviewed because we cannot retrieve a single uniprot entry matching " + uniprotIdentity);
                            fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, intactProteinToUpdate, uniprotIdentity));
                        }
                    } else if (!transcriptsWithoutParents.isEmpty()) {
                        ProteinUpdateError impossibleToDeleteEvent = errorFactory.createImpossibleParentTranscriptToReviewError(intactProteinToUpdate.getAc(), "The protein transcript cannot be reviewed because it does not have a valid uniprot identity.");
                        fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, impossibleToDeleteEvent, intactProteinToUpdate));
                    }
                }
            }

        } catch (Exception e) {
            log.fatal("We failed to update the protein " + intactProteinToUpdate.getAc(), e);

            ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(intactProteinToUpdate.getAc(), uniprotIdentity, e);
            fireOnProcessErrorFound(new UpdateErrorEvent(this, dataContext, fatalError, intactProteinToUpdate, uniprotIdentity));
        }

        return processedProteins;
    }

    // Noe: 8-07-2021 This method is only used in testing the ProteinProcessor. For my taste I think it should get moved from here to a tests class
    // (or mock the update behaviour) - only for testing purposes
    protected List<Protein> retrieveAndUpdateProteinFromUniprot(String uniprotAc) throws ProcessorException {

        config.setBlastEnabled(false);

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        List<Protein> intactProteins = new ArrayList<>();

        // register the listeners
        if (getListeners(SequenceChangedListener.class) == null) {
            addListener(new SequenceChangedListener());
        }

        if (uniprotAc == null) {
            throw new ProcessorException("The uniprot ac should not be null");
        }

        // get the uniprot protein (intact-protein module in bridges)
        UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(uniprotAc);

        // if the uniprot protein exists, start to update
        if (uniprotProtein != null) {
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                if (log.isTraceEnabled())
                    log.trace("Retrieving all intact proteins matcing the uniprot entry : " + uniprotAc);

                ProteinEvent processEvent = new ProteinEvent(this, context, null);
                processEvent.setUniprotIdentity(uniprotAc);

                UpdateCaseEvent caseEvent = runProteinUpdate(uniprotProtein, processEvent, null);

                if (caseEvent != null) {
                    intactProteins.addAll(caseEvent.getPrimaryProteins());
                    intactProteins.addAll(caseEvent.getSecondaryProteins());

                    for (ProteinTranscript pt : caseEvent.getPrimaryIsoforms()) {
                        intactProteins.add(pt.getProtein());
                    }
                    for (ProteinTranscript pt : caseEvent.getSecondaryIsoforms()) {
                        intactProteins.add(pt.getProtein());
                    }
                    for (ProteinTranscript pt : caseEvent.getPrimaryFeatureChains()) {
                        intactProteins.add(pt.getProtein());
                    }
                }

                context.commitTransaction(status);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(null, uniprotAc, e);
                fireOnProcessErrorFound(new UpdateErrorEvent(this, context, fatalError, uniprotAc));
                if (!status.isCompleted()) {
                    context.rollbackTransaction(status);
                }
            }
        } else {
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                List<ProteinImpl> deadProteins = IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByUniprotId(uniprotAc);

                if (!deadProteins.isEmpty()) {
                    for (ProteinImpl prot : deadProteins) {
                        ProteinEvent evt = new ProteinEvent(this, context, prot);

                        //TODO Noe: why is not storing the retrieved entry?
                        uniprotRetriever.retrieveUniprotEntry(evt);
                    }
                }

                context.commitTransaction(status);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                ProteinUpdateError fatalError = errorFactory.createFatalUpdateError(null, uniprotAc, e);
                fireOnProcessErrorFound(new UpdateErrorEvent(this, context, fatalError, uniprotAc));
                if (!status.isCompleted()) {
                    context.rollbackTransaction(status);
                }
            }
        }

        List<ReportWriterListener> writers = getListeners(ReportWriterListener.class);

        for (ReportWriterListener listener : writers) {
            UpdateReportHandler handler = listener.getReportHandler();

            if (handler != null) {
                try {
                    handler.close();
                } catch (IOException e) {
                    throw new ProcessorException("Impossible to close one of the log files.", e);
                }
            }
        }

        config.getUniprotService().close();

        return intactProteins;
    }

    protected <T> List<T> getListeners(Class<T> listenerClass) {
        List list = new ArrayList();

        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinUpdateProcessorListener.class) {
                if (listenerClass.isAssignableFrom(listeners[i + 1].getClass())) {
                    list.add(listeners[i + 1]);
                }
            }
        }
        return list;
    }

    protected void registerListenersIfNotDoneYet() {
        if (listenerList.getListenerCount() == 0) {
            registerListeners();
        }

        if (listenerList.getListenerCount() == 0) {
            throw new IllegalStateException("No listener registered for ProteinProcessor");
        }
    }

    protected void registerListeners() {

        addListener(new LoggingProcessorListener());

        if (config.getReportHandler() != null) {
            addListener(new ReportWriterListener(config.getReportHandler()));
        }

        addListener(new SequenceChangedListener());
    }

    private void initDefaultActionsAndListeners() {
        this.config = ProteinUpdateContext.getInstance().getConfig();

        // uses a filter in cross references
        config.getUniprotService().setCrossReferenceSelector(new DefaultCrossReferenceFilter());

        this.proteinDeleter = new ProteinDeleter();
        this.uniprotIdentityUpdater = new UniprotIdentityUpdater();
        this.protWithoutInteractionDeleter = new ProtWithoutInteractionDeleter();
        this.parentUpdater = new IntactTranscriptParentUpdater();

        UniprotProteinMapper proteinMappingManager = new UniprotProteinMapper(config.getUniprotService());
        this.updateFilter = new ProteinUpdateFilter(proteinMappingManager);
        this.uniprotRetriever = new UniprotProteinRetriever(config.getUniprotService(), proteinMappingManager, new DeadUniprotProteinFixer());

        OutOfDateParticipantFixer outOfDateParticipantFixer = new OutOfDateParticipantFixer(new RangeFixer());
        this.duplicateFixer = new DuplicatesFixer(this.proteinDeleter, outOfDateParticipantFixer, new DuplicatesFinder());
        this.updater = new UniprotProteinUpdater(config.getTaxonomyService(), outOfDateParticipantFixer);

        registerListeners();
    }

    private UpdateCaseEvent runProteinUpdate(UniprotProtein uniprotProtein, ProteinEvent processEvent, List<Protein> transcriptToReview) {

        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // if the uniprot protein exists, start to update
        if (uniprotProtein != null) {
            // set the uniprot protein of the event
            processEvent.setUniprotProtein(uniprotProtein);

            // if the uniprot identity is null, set the uniprot identity to the primary ac of the uniprot entry.
            // it is possible that the uniprot identity is different that th uniprot primary ac, it depends which protein we were updating first (
            // could be secondary ac, isoform ac, feature chain id, etc.)
            if (processEvent.getUniprotIdentity() == null) {
                processEvent.setUniprotIdentity(uniprotProtein.getPrimaryAc());
            }

            if (log.isTraceEnabled())
                log.trace("Retrieving all intact proteins matcing the uniprot entry : " + processEvent.getUniprotIdentity());

            // get all the proteins in intact attached to this uniprot entry without non uniprot :
            // - all intact proteins with uniprot identity = uniprot primary ac => primary proteins
            // - all intact proteins with uniprot identity = one of the uniprot secondary acs => secondary proteins
            // - For each primary and secondary proteins, collect all splice variants and feature chains attached to it
            //     - primary isoforms : each primary isoform matches a primary ac of a uniprot splice variant
            //     - secondary isoforms : each secondary isoform matches a secondary ac of a uniprot splice variant
            //     - primary feature chains : each primary feature chain matches a primary ac of a uniprot feature chain
            UpdateCaseEvent caseEvent = uniprotIdentityUpdater.collectPrimaryAndSecondaryProteins(processEvent);

            // if we can delete proteins without interactions, delete all of the proteins attached to this uniprot entry without interactions
            if (config.isDeleteProtsWithoutInteractions()) {
                if (log.isTraceEnabled()) log.trace("Checking for all protein interactions");

                // get the list of proteins to delete. All these proteins have been removed from the UpdateCaseEvent and will not be updated
                Set<Protein> protToDelete = protWithoutInteractionDeleter.collectAndRemoveProteinsWithoutInteractions(caseEvent);

                // delete these proteins
                for (Protein p : protToDelete) {
                    ProteinEvent protEvent = new ProteinEvent(this, caseEvent.getDataContext(), p, uniprotProtein, "Protein without interactions");

                    boolean isDeletedFromDatabase = proteinDeleter.delete(protEvent);

                    if (!isDeletedFromDatabase) {

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
            if (transcriptToReview != null) {
                transcriptToReview.addAll(parentUpdater.checkConsistencyOfAllTranscripts(caseEvent));
            } else {
                transcriptToReview = parentUpdater.checkConsistencyOfAllTranscripts(caseEvent);
            }

            if (log.isTraceEnabled())
                log.trace("Filtering " + caseEvent.getPrimaryProteins().size() + " primary proteins and " + caseEvent.getSecondaryProteins().size() + "secondary proteins for uniprot update.");

            // filter on 'no-uniprot-update' and multi identities, remove them from the list of proteins to update (can happen if proteins with uniprot identity and 'no-uniprot-update')
            updateFilter.filterNonUniprotAndMultipleUniprot(caseEvent);

            if (log.isTraceEnabled())
                log.trace("Checking that it is possible to update existing secondary proteins for " + uniprotProtein.getPrimaryAc());

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
            if (config.isFixDuplicates()) {
                if (log.isTraceEnabled()) log.trace("Check for possible duplicates.");

                // return the master protein which is the result of the merge if there is one. Returns null if there were no duplicated proteins
                // or if it was impossible to have an original protein after the merge
                masterProtein = duplicateFixer.fixAllProteinDuplicates(caseEvent);
            }

            // update master proteins first
            try {
                updater.createOrUpdateProtein(caseEvent);
            } catch (BioSourceServiceException e) {
                ProteinUpdateError impossibleUpdate = errorFactory.createImpossibleUpdateMasterError("The master proteins for the uniprot entry " + caseEvent.getProtein().getPrimaryAc() + " couldn't be updated because of a biosource service problem when creating a new protein\n" + e.getMessage(), caseEvent.getProtein().getPrimaryAc());
                fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleUpdate, caseEvent.getProtein().getPrimaryAc()));
            } catch (IntactTransactionException e) {
                ProteinUpdateError impossibleUpdate = errorFactory.createImpossibleUpdateMasterError("The master proteins for the uniprot entry " + caseEvent.getProtein().getPrimaryAc() + " couldn't be updated because of a IntAct transaction problem when creating a new protein\n" + e.getMessage(), caseEvent.getProtein().getPrimaryAc());
                fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleUpdate, caseEvent.getProtein().getPrimaryAc()));
            }


            // it is possible to update protein transcripts only if a master protein is available :
            // - a master protein not nul which is the result of a merge
            // - a master protein which is the unique protein in primary proteins
            // if master protein is null and the list of primary proteins contains more than one protein, it is impossible to update the transcripts because no parent available
            boolean canUpdateProteinTranscript = false;

            // master protein null because no merge done before and number of primary proteins = 1 : the master protein exists
            // a merge has been done, the master protein is the result of the merge
            if (masterProtein == null && caseEvent.getPrimaryProteins().size() == 1) {
                masterProtein = caseEvent.getPrimaryProteins().iterator().next();
                canUpdateProteinTranscript = true;
            }
            // master protein null because no merge done before and number of primary proteins != 1 : the master protein is impossible to decide
            else canUpdateProteinTranscript = masterProtein != null || caseEvent.getPrimaryProteins().size() == 1;

            boolean needTranscriptUpdate = (caseEvent.getPrimaryIsoforms().size() >= 1 ||
                    caseEvent.getSecondaryIsoforms().size() >= 1 || caseEvent.getPrimaryFeatureChains().size() >= 1) ||
                    (caseEvent.getPrimaryIsoforms().isEmpty() && caseEvent.getSecondaryIsoforms().isEmpty() &&
                            caseEvent.getPrimaryFeatureChains().isEmpty() && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions());

            // if a single master protein has been found and transcript update is needed
            if (needTranscriptUpdate) {
                if (canUpdateProteinTranscript) {
                    // add first a parent xref for all the protein transcripts without parent xref if it is necessary
                    if (!transcriptToReview.isEmpty()) {
                        parentUpdater.createParentXRefs(transcriptToReview, masterProtein, uniprotProtein.getPrimaryAc(), caseEvent.getDataContext(), this);
                    }

                    //protein transcript duplicates to merge
                    // fixing duplicates is enabled
                    if (config.isFixDuplicates()) {
                        if (log.isTraceEnabled()) log.trace("Check for possible transcript duplicates.");

                        duplicateFixer.fixAllProteinTranscriptDuplicates(caseEvent, masterProtein);
                    }

                    // update isoforms if necessary
                    if (!caseEvent.getPrimaryIsoforms().isEmpty() || (caseEvent.getPrimaryIsoforms().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) {
                        updater.createOrUpdateIsoform(caseEvent, masterProtein);
                    }

                    // update chains if necessary
                    if (!caseEvent.getPrimaryFeatureChains().isEmpty() || (caseEvent.getPrimaryFeatureChains().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) {
                        updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                    }
                } else {
                    ProteinUpdateError impossibleUpdate = errorFactory.createImpossibleTranscriptUpdateError("No master protein or several master proteins exist and it is impossible to update the protein transcripts", uniprotProtein.getPrimaryAc());
                    fireOnProcessErrorFound(new UpdateErrorEvent(this, caseEvent.getDataContext(), impossibleUpdate, uniprotProtein.getPrimaryAc()));
                }
            }

            // log in updated.csv
            boolean hasBeenUpdated = false;
            if (!caseEvent.getNameUpdaterReports().isEmpty()) {
                hasBeenUpdated = true;
            } else if (!caseEvent.getAliasUpdaterReports().isEmpty()) {
                hasBeenUpdated = true;
            } else if (!caseEvent.getXrefUpdaterReports().isEmpty()) {
                hasBeenUpdated = true;
            } else if (!caseEvent.getNewAnnotations().isEmpty()) {
                hasBeenUpdated = true;
            } else if (!caseEvent.getUpdatedRanges().isEmpty()) {
                hasBeenUpdated = true;
            }

            if (hasBeenUpdated) {
                fireOnUpdateCase(caseEvent);
            }

            return caseEvent;
        }

        return null;
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
}