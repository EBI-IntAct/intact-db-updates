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
import uk.ac.ebi.intact.dbupdate.prot.event.MultiProteinEvent;
import uk.ac.ebi.intact.model.Protein;
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

    public ProteinProcessor() {
        previousBatchACs = new ArrayList<String>();
    }

    public ProteinProcessor(int batchSize){
        this();
        this.batchSize = batchSize;
    }

    protected abstract void registerListeners();

    protected void addListener(ProteinProcessorListener processorListener) {
        listenerList.add(ProteinProcessorListener.class, processorListener);
    }

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

                // load annotations (to avoid lazyinitializationexceptions later)
                protein.getAnnotations().size();
            }

            update(protsToUpdate);

            commitTransaction();

            firstResult = firstResult+(batchSize/2);

        } while (!protsToUpdate.isEmpty());
    }

    public void update(Collection<? extends Protein> protsToUpdate) throws ProcessorException {
       registerListenersIfNotDoneYet();

        if (log.isTraceEnabled()) log.trace("Going to process "+protsToUpdate.size()+" proteins");

       for (Protein protToUpdate : protsToUpdate) {
           update(protToUpdate);
       }
    }

    public void update(Protein protToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        if (log.isTraceEnabled()) log.trace("Pre-processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent preProcessEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnPreProcess(preProcessEvent);

        if (preProcessEvent.isFinalizationRequested()) {
            if (log.isDebugEnabled()) log.debug("Finalizing after Pre-Process phase");
            return;
        }

        if (log.isTraceEnabled()) log.trace("Processing protein: "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

        ProteinEvent processEvent = new ProteinEvent(this, dataContext, protToUpdate);
        fireOnProcess(processEvent);

        if (preProcessEvent.isFinalizationRequested()) {
            if (log.isDebugEnabled()) log.debug("Finalizing after Process phase");
            return;
        }
    }


    // listener methods
    public void addProteinUpdaterListener(ProteinProcessorListener listener) {
        listenerList.add(ProteinProcessorListener.class, listener);
    }

    public void removeProteinUpdaterListener(ProteinProcessorListener listener) {
        listenerList.remove(ProteinProcessorListener.class, listener);
    }

    public void fireOnPreProcess(ProteinEvent evt) {
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                try {
                    ((ProteinProcessorListener) listeners[i + 1]).onPreProcess(evt);
                } catch (ProcessorException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireOnProcess(ProteinEvent evt) {
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                try {
                    ((ProteinProcessorListener) listeners[i + 1]).onProcess(evt);
                } catch (ProcessorException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireOnPreDelete(ProteinEvent evt) {
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                try {
                    ((ProteinProcessorListener) listeners[i + 1]).onPreDelete(evt);
                } catch (ProcessorException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireOnProteinDuplicationFound(MultiProteinEvent evt) {
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                try {
                    ((ProteinProcessorListener) listeners[i + 1]).onProteinDuplicationFound(evt);
                } catch (ProcessorException e) {
                    e.printStackTrace();
                }
            }
        }
    }

    public void fireOnDeadProteinFound(ProteinEvent evt) {
        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinProcessorListener.class) {
                try {
                    ((ProteinProcessorListener) listeners[i + 1]).onDeadProteinFound(evt);
                } catch (ProcessorException e) {
                    e.printStackTrace();
                }
            }
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
}
