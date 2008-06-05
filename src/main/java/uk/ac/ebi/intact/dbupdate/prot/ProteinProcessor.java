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

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.business.IntactTransactionException;
import uk.ac.ebi.intact.context.DataContext;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinProcessorListener;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.util.DebugUtil;

import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * Iterates through the proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    // to allow listeners
    protected EventListenerList listenerList = new EventListenerList();

    private int batchSize = 50;

    private List<String> previousBatchACs;

    private boolean finalizationRequested;

    private Protein currentProtein;


    public ProteinProcessor() {
        previousBatchACs = new ArrayList<String>();
    }

    public ProteinProcessor(int batchSize){
        this();
        this.batchSize = batchSize;
    }

    protected abstract void registerListeners();

    /**
     * Updates all the proteins in the database
     * @throws ProcessorException
     */
    public void updateAll() throws ProcessorException {
        registerListenersIfNotDoneYet();
        
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        ProteinDao protDao = dataContext.getDaoFactory().getProteinDao();

        dataContext.beginTransaction();
        int totalCount = protDao.countAll();
        commitTransaction();

        if (log.isDebugEnabled()) log.debug("Found a total of "+totalCount+" proteins in the database");

        int maxResults = batchSize;
        int firstResult = 0;

        List<? extends Protein> protsToUpdate;

        do {
            if (log.isTraceEnabled()) log.trace("Processing batch "+firstResult+"-"+(firstResult+maxResults));
            dataContext.beginTransactionManualFlush();
            protsToUpdate = protDao.getAllSorted(firstResult, maxResults, "created", true);

            // we check if the previous batch of ACs and this one overlapps, just to check
            // that the iteration is correct and no external (or from listeners) changes
            // affect the iteration
            List<String> currentBatchACs = DebugUtil.acList(protsToUpdate);

            if (log.isTraceEnabled()) log.trace("Current batch of ACs: "+currentBatchACs);

            Collection<String> intersectedACs = CollectionUtils.intersection(previousBatchACs, currentBatchACs);

            if (firstResult > 0 && intersectedACs.isEmpty() && !protsToUpdate.isEmpty()) {
                if (log.isInfoEnabled()) log.info("No overlap between batches in iteration. Will adjust firstResult");
                firstResult = firstResult-(batchSize/2)+1;
            } else {
                if (log.isTraceEnabled()) log.trace("Intersection between batches: "+intersectedACs);
                previousBatchACs = currentBatchACs;
            }

            // we removed the overlapped elements from the list, so they are not processed again
            //protsToUpdate.subList(intersectedACs.size(), protsToUpdate.size());
            for (Iterator<? extends Protein> protIterator = protsToUpdate.iterator(); protIterator.hasNext();) {
                Protein protein = protIterator.next();
                if (intersectedACs.contains(protein.getAc())) {
                    protIterator.remove();
                }
            }

            commitTransaction();

            updateByACs(DebugUtil.acList(protsToUpdate));

            firstResult = firstResult+(batchSize/2);

        } while (!protsToUpdate.isEmpty());
    }

    public void updateByACs(List<String> protACsToUpdate) throws ProcessorException {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        for (String protAc : protACsToUpdate) {
            dataContext.beginTransaction();
            ProteinImpl prot = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

            if (prot == null) {
                if (log.isWarnEnabled()) log.warn("Protein was not found in the database. Probably it was deleted already? "+protACsToUpdate);
                continue;
            }

            // load annotations (to avoid lazyinitializationexceptions later)
            prot.getXrefs().size();
            prot.getAnnotations().size();

            update(prot);

            try {
                dataContext.commitTransaction();
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

    public void update(Protein protToUpdate) throws ProcessorException {
        this.currentProtein = protToUpdate;
        
        registerListenersIfNotDoneYet();

        finalizationRequested = false;

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        if (log.isTraceEnabled()) log.trace("Pre-processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent preProcessEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnPreProcess(preProcessEvent);

        if (isFinalizationRequested()) {
            if (log.isDebugEnabled()) log.debug("Finalizing after Pre-Process phase");
            return;
        }

        try {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        } catch (IntactTransactionException e) {
            throw new ProcessorException(e);
        }
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        if (log.isTraceEnabled()) log.trace("Processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent processEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnProcess(processEvent);
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
            throw new IllegalStateException("No listeners registered for ProteinProcessor");
        }
    }

    // other private methods

    private void commitTransaction() throws ProcessorException {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        try {
            dataContext.commitTransaction();
        } catch (IntactTransactionException e) {
            throw new ProcessorException("Problem committing", e);
        }
    }

    public Protein getCurrentProtein() {
        return currentProtein;
    }
}
