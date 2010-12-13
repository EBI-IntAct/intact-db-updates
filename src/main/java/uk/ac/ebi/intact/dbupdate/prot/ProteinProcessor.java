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
import org.hibernate.Hibernate;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.actions.*;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.SequenceChangedListener;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;

import javax.swing.event.EventListenerList;
import java.util.*;

/**
 * Iterates through the proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    // to allow listener
    protected EventListenerList listenerList = new EventListenerList();

    private List<String> previousBatchACs;

    private boolean finalizationRequested;

    protected Protein currentProtein;

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
     * The duplicate finder
     */
    protected DuplicatesFinder duplicateFinder;
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

    protected RangeFixer rangeFixer;

    protected OutOfDateParticipantFixer participantFixer;
    protected UniprotProteinUpdater updater;
    protected IntactParentUpdater parentUpdater;

    public ProteinProcessor() {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        previousBatchACs = new ArrayList<String>();
        updateFilter = new ProteinUpdateFilter();
        this.uniprotIdentityUpdater = new UniprotIdentityUpdater();
        this.uniprotRetriever = new UniprotProteinRetriever(config.getUniprotService());
        this.duplicateFinder = new DuplicatesFinder();
        this.duplicateFixer = new DuplicatesFixer();
        this.proteinDeleter = new ProteinDeleter();
        this.protWithoutInteractionDeleter = new ProtWithoutInteractionDeleter();
        this.updater = new UniprotProteinUpdater(config.getTaxonomyService());
        this.participantFixer = new OutOfDateParticipantFixer();
        rangeFixer = new RangeFixer();
        parentUpdater = new IntactParentUpdater();
    }

    /**
     *
     * @param batchSize
     * @param stepSize
     * @deprecated please use the default constructor instead.
     */
    @Deprecated
    public ProteinProcessor(int batchSize, int stepSize){
        this();
    }

    protected abstract void registerListeners();

    /**
     * Updates all the proteins in the database
     * @throws ProcessorException
     */
    public void updateAll() throws ProcessorException {

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        TransactionStatus transactionStatus = dataContext.beginTransaction();
        List<String> acs = dataContext.getDaoFactory().getEntityManager()
                .createQuery("select p.ac from ProteinImpl p order by p.created").getResultList();
        commitTransaction(transactionStatus, dataContext);

        updateByACs(acs);
    }

    public void updateByACs(List<String> protACsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        Set<String> processedIntactProteins = new HashSet<String>();

        for (String protAc : protACsToUpdate) {
            DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus transactionStatus = dataContext.beginTransaction();

            try {
                ProteinImpl prot = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

                if (prot == null) {
                    if (log.isWarnEnabled()) log.warn("Protein was not found in the database. Probably it was deleted already? "+protAc);
                    try {
                        dataContext.commitTransaction(transactionStatus);
                    } catch (IntactTransactionException e) {
                        throw new ProcessorException(e);
                    }
                    continue;
                }

                // load annotations (to avoid lazyinitializationexceptions later)
                Hibernate.initialize(prot.getXrefs());
                Hibernate.initialize(prot.getAnnotations());
                Hibernate.initialize(prot.getAliases());
                for (Component c : prot.getActiveInstances()){
                    Hibernate.initialize(c.getXrefs());
                    Hibernate.initialize(c.getAnnotations());

                    for (Feature f : c.getBindingDomains()){
                        Hibernate.initialize(f.getAnnotations());
                        Hibernate.initialize(f.getRanges());
                        Hibernate.initialize(f.getAliases());
                        Hibernate.initialize(f.getXrefs());
                    }
                    Hibernate.initialize(c.getExperimentalRoles());
                    Hibernate.initialize(c.getAliases());
                    Hibernate.initialize(c.getExperimentalPreparations());
                    Hibernate.initialize(c.getParameters());
                    Hibernate.initialize(c.getParticipantDetectionMethods());
                }

                if (!processedIntactProteins.contains(prot.getAc())){
                    processedIntactProteins.addAll(update(prot, dataContext));
                }


                dataContext.commitTransaction(transactionStatus);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + protAc);
                if (!transactionStatus.isCompleted()){
                    dataContext.rollbackTransaction(transactionStatus);
                }
            }
        }
    }

    public void update(List<? extends Protein> protsToUpdate, DataContext dataContext) throws ProcessorException {
        registerListenersIfNotDoneYet();
        Set<String> processedIntactProteins = new HashSet<String>();

        if (log.isTraceEnabled()) log.trace("Going to process "+protsToUpdate.size()+" proteins");

        for (Protein protToUpdate : protsToUpdate) {
            if (!processedIntactProteins.contains(protToUpdate.getAc())){
                processedIntactProteins.addAll(update(protToUpdate, dataContext));
            }
            update(protToUpdate, dataContext);
        }
    }

    // do nothing, to override
    public List<Protein> retrieveAndUpdateProteinFromUniprot(String uniprotAc) throws ProcessorException{
        List<Protein> intactProteins = new ArrayList<Protein>();

        return intactProteins;
    }

    // do nothing, to override
    public Set<String> update(Protein protToUpdate, DataContext dataContext) throws ProcessorException {
        // the proteins processed during this update
        Set<String> processedProteins = new HashSet<String>();
       
        return processedProteins;
    }

    private void processDuplicatesTranscript(UpdateCaseEvent caseEvent, Collection<ProteinTranscript> mergedTranscripts, DuplicatesFoundEvent duplEvt) {
        DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplEvt);

        String originalAc = report.getOriginalProtein().getAc();

        for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
            if (originalAc.equals(p.getProtein().getAc())){
                report.setTranscript(p.getUniprotVariant());
            }
        }

        if (report.getTranscript() == null){
            for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                if (originalAc.equals(p.getProtein().getAc())){
                    report.setTranscript(p.getUniprotVariant());
                }
            }

            if (report.getTranscript() == null){
                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                    if (originalAc.equals(p.getProtein().getAc())){
                        report.setTranscript(p.getUniprotVariant());
                    }
                }
            }
        }

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                if (fixedProtein != null){

                    if (IdentifierChecker.isSpliceVariantId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                        String ac = fixedProtein.getProtein().getAc();

                        if (ac != null){
                            boolean hasFoundSpliceVariant = false;

                            for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
                                if (ac.equals(p.getProtein().getAc())){
                                    hasFoundSpliceVariant = true;
                                }
                            }

                            if (!hasFoundSpliceVariant){
                                for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundSpliceVariant = true;
                                    }
                                }

                                if (!hasFoundSpliceVariant){
                                    ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                    mergedTranscripts.add(fixedProtein);
                                    caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                                }
                            }
                        }
                    }
                    else if (IdentifierChecker.isFeatureChainId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                        String ac = fixedProtein.getProtein().getAc();

                        if (ac != null){
                            boolean hasFoundChain = false;

                            for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                                if (ac.equals(p.getProtein().getAc())){
                                    hasFoundChain = true;
                                }
                            }

                            if (!hasFoundChain){
                                ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                        processor.fireNonUniprotProteinFound(new ProteinEvent(processor, caseEvent.getDataContext(), entry.getKey()));
                    }
                }
            }
        }

        if (report.getOriginalProtein() != null){
            mergedTranscripts.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
        }
    }

    private DuplicateReport processDuplicatesProtein(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplicateEvent) {
        DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplicateEvent);

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                if (fixedProtein != null){

                    if (IdentifierChecker.isSpliceVariantId(fixedProtein.getUniprotVariant().getPrimaryAc())){

                        String ac = fixedProtein.getProtein().getAc();

                        if (ac != null){
                            boolean hasFoundSpliceVariant = false;

                            for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
                                if (ac.equals(p.getProtein().getAc())){
                                    hasFoundSpliceVariant = true;
                                }
                            }

                            if (!hasFoundSpliceVariant){
                                for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundSpliceVariant = true;
                                    }
                                }

                                if (!hasFoundSpliceVariant){
                                    ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                    caseEvent.getPrimaryIsoforms().add(fixedProtein);
                                    caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                                }
                            }
                        }
                    }
                    else if (IdentifierChecker.isFeatureChainId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                        String ac = fixedProtein.getProtein().getAc();

                        if (ac != null){
                            boolean hasFoundChain = false;

                            for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                                if (ac.equals(p.getProtein().getAc())){
                                    hasFoundChain = true;
                                }
                            }

                            if (!hasFoundChain){
                                ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                        processor.fireNonUniprotProteinFound(new ProteinEvent(processor, caseEvent.getDataContext(), entry.getKey()));
                    }
                }
            }
        }
        return report;
    }

    public void finalizeAfterCurrentPhase() {
        finalizationRequested = true;
    }

    public boolean isFinalizationRequested() {
        return finalizationRequested;
    }

    // listener methods
    public void addListener(ProteinUpdateProcessorListener listener) {
        listenerList.add(ProteinUpdateProcessorListener.class, listener);
    }

    public void removeListener(ProteinUpdateProcessorListener listener) {
        listenerList.remove(ProteinUpdateProcessorListener.class, listener);
    }

    protected <T> List<T> getListeners(Class<T> listenerClass) {
        List list = new ArrayList();

        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinUpdateProcessorListener.class) {
                if (listenerClass.isAssignableFrom(listeners[i+1].getClass())) {
                    list.add(listeners[i+1]);
                }
            }
        }
        return list;
    }

    /*public void fireOnPreProcess(ProteinEvent evt) {
        for (ProteinProcessorListener listener : getListeners(ProteinProcessorListener.class)) {
            if (isFinalizationRequested()) {
                log.debug("Processor finalization already requested. Skipping: " + listener.getClass());
                return;
            }
            listener.onPreProcess(evt);
        }
    }

    public void fireOnProcess(ProteinEvent evt) {
        for (ProteinProcessorListener listener : getListeners(ProteinProcessorListener.class)) {
            if (isFinalizationRequested()) {
                log.debug("Processor finalization already requested. Skipping: " + listener.getClass());
                return;
            }
            listener.onProcess(evt);
        }
    }*/

    protected void registerListenersIfNotDoneYet() {
        if (listenerList.getListenerCount() == 0) {
            registerListeners();
        }

        if (listenerList.getListenerCount() == 0) {
            throw new IllegalStateException("No listener registered for ProteinProcessor");
        }
    }

    // other private methods
    private void commitTransaction(TransactionStatus transactionStatus, DataContext dataContext) throws ProcessorException {
        try {
            dataContext.commitTransaction(transactionStatus);
        } catch (IntactTransactionException e) {
            throw new ProcessorException("Problem committing", e);
        }
    }

    public Protein getCurrentProtein() {
        return currentProtein;
    }
}