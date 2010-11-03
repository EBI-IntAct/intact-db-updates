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
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.listener.UniprotProteinUpdater;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

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

    private Protein currentProtein;


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
        commitTransaction(transactionStatus);

        updateByACs(acs);
    }

    public void updateByACs(List<String> protACsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        Set<String> processedIntactProteins = new HashSet<String>();

        for (String protAc : protACsToUpdate) {
            TransactionStatus transactionStatus = dataContext.beginTransaction();
            ProteinImpl prot = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

            if (prot == null) {
                if (log.isWarnEnabled()) log.warn("Protein was not found in the database. Probably it was deleted already? "+protAc);
                continue;
            }

            // load annotations (to avoid lazyinitializationexceptions later)
            prot.getXrefs().size();
            prot.getAnnotations().size();

            if (!processedIntactProteins.contains(prot.getAc())){
                processedIntactProteins.addAll(update(prot));
            }

            try {
                dataContext.commitTransaction(transactionStatus);
            } catch (IntactTransactionException e) {
                throw new ProcessorException(e);
            }
        }
    }

    public void update(List<? extends Protein> protsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        if (log.isTraceEnabled()) log.trace("Going to process "+protsToUpdate.size()+" proteins");

        for (Protein protToUpdate : protsToUpdate) {
            update(protToUpdate);
        }
    }

    private Set<String> getProcessedProteins(String proteinAc){
        Set<String> updatedProteins = new HashSet<String>();
        updatedProteins.add(proteinAc);

        List<UniprotProteinUpdater> listOfUniprotUpdater = getListeners(UniprotProteinUpdater.class);

        if (!listOfUniprotUpdater.isEmpty()){

            for (UniprotProteinUpdater updater : listOfUniprotUpdater){
                UniprotServiceResult serviceResult = updater.getUniprotServiceResult();

                if (serviceResult != null){
                    Collection<Protein> prots = updater.getUniprotServiceResult().getProteins();

                    for (Protein prot : prots){
                        if (prot != null){
                            updatedProteins.add(prot.getAc());
                        }
                    }
                }
            }
        }
        return updatedProteins;
    }

    public Set<String> update(Protein protToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        this.currentProtein = protToUpdate;

        finalizationRequested = false;

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        TransactionStatus transactionStatus = dataContext.beginTransaction();

        if (log.isTraceEnabled()) log.trace("Pre-processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent preProcessEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnPreProcess(preProcessEvent);

        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction(transactionStatus);
        } catch (IntactTransactionException e) {
            throw new ProcessorException(e);
        }

        if (isFinalizationRequested()) {
            if (log.isDebugEnabled()) log.debug("Finalizing after Pre-Process phase");

            return getProcessedProteins(this.currentProtein.getAc());
        }

        TransactionStatus transactionStatus2 = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        if (log.isTraceEnabled()) log.trace("Processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent processEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnProcess(processEvent);

        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction(transactionStatus2);

        } catch (IntactTransactionException e) {
            throw new ProcessorException(e);
        }

        return getProcessedProteins(this.currentProtein.getAc());
    }

    public void finalizeAfterCurrentPhase() {
        finalizationRequested = true;
    }

    public boolean isFinalizationRequested() {
        return finalizationRequested;
    }

    // listener methods
    public void addListener(ProteinProcessorListener listener) {
        listenerList.add(ProteinProcessorListener.class, listener);
    }

    public void removeListener(ProteinProcessorListener listener) {
        listenerList.remove(ProteinProcessorListener.class, listener);
    }

    protected <T> List<T> getListeners(Class<T> listenerClass) {
        List list = new ArrayList();

        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                if (listenerClass.isAssignableFrom(listeners[i+1].getClass())) {
                    list.add(listeners[i+1]);
                }
            }
        }
        return list;
    }

    public void fireOnPreProcess(ProteinEvent evt) {
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
    }

    private void registerListenersIfNotDoneYet() {
        if (listenerList.getListenerCount() == 0) {
            registerListeners();
        }

        if (listenerList.getListenerCount() == 0) {
            throw new IllegalStateException("No listener registered for ProteinProcessor");
        }
    }

    // other private methods

    private void commitTransaction(TransactionStatus transactionStatus) throws ProcessorException {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
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
