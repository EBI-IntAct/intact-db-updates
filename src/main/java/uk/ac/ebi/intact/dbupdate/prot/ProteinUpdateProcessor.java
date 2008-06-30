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
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.*;

/**
 * Updates the database proteins using the latest information from UniProt
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessor extends ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    private ProteinUpdateProcessorConfig configUpdate;

    public ProteinUpdateProcessor(){
        super(60, 20);
        this.configUpdate = new ProteinUpdateProcessorConfig();
    }

    public ProteinUpdateProcessor(ProteinUpdateProcessorConfig configUpdate){
        super(configUpdate.getProcessBatchSize(), configUpdate.getProcessStepSize());
        this.configUpdate = configUpdate;
    }

    protected void registerListeners() {
        addListener(new LoggingProcessorListener());

        boolean forceDeleteOfProteins = false;

        if (configUpdate.isFixDuplicates()) {
            addListener(new DuplicatesFinder());
            addListener(new DuplicatesFixer());
            forceDeleteOfProteins = true;
        }

        if (configUpdate.isDeleteProtsWithoutInteractions()) {
            ProtWithoutInteractionDeleter deleter = new ProtWithoutInteractionDeleter();
            deleter.setDeleteSpliceVariantsWithoutInteractions(configUpdate.isDeleteSpliceVariantsWithoutInteractions());
            addListener(deleter);
            forceDeleteOfProteins = true;
        }

        if (forceDeleteOfProteins) {
            addListener(new ProteinDeleter());
        }
        
        addListener(new UniprotProteinUpdater(configUpdate.getUniprotService(), configUpdate.getTaxonomyService()));

        if (configUpdate.getReportHandler() != null) {
            addListener(new ReportWriterListener(configUpdate.getReportHandler()));
        }

        addListener(new RangeFixer());
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

}
