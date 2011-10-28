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
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.ReportWriterListener;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;

import javax.swing.event.EventListenerList;
import java.io.IOException;
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

    protected int COMMIT_INTERVAL = 50;

    public ProteinProcessor() {

        previousBatchACs = new ArrayList<String>();
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
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        Iterator<String> protAcsIterator = protACsToUpdate.iterator();
        int currentIndex = 0;
        ProteinImpl prot = null;
        String protAc = null;

        while (protAcsIterator.hasNext()){
            TransactionStatus transactionStatus = dataContext.beginTransaction();

            try {

                while (currentIndex < COMMIT_INTERVAL && protAcsIterator.hasNext()){
                    protAc = protAcsIterator.next();

                    if (!processedIntactProteins.contains(protAc)){

                        prot = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

                        if (prot != null){
                            processedIntactProteins.addAll(update(prot, dataContext));
                        }
                        else {
                            if (log.isWarnEnabled()) log.warn("Protein was not found in the database. Probably it was deleted already? "+protAc);
                            processedIntactProteins.add(protAc);
                        }

                        // load annotations (to avoid lazyinitializationexceptions later)
                        /*Hibernate.initialize(prot.getXrefs());
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
                        }*/
                    }
                    currentIndex ++;
                }

                dataContext.commitTransaction(transactionStatus);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + protAc , e);

                if (!transactionStatus.isCompleted()){
                    dataContext.rollbackTransaction(transactionStatus);
                }
            }

            currentIndex = 0;
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
    public Set<String> update(Protein protToUpdate, DataContext dataContext){
        // the proteins processed during this update
        Set<String> processedProteins = new HashSet<String>();

        return processedProteins;
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

    public int getCOMMIT_INTERVAL() {
        return COMMIT_INTERVAL;
    }

    public void setCOMMIT_INTERVAL(int COMMIT_INTERVAL) {
        this.COMMIT_INTERVAL = COMMIT_INTERVAL;
    }
}