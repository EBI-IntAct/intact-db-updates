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

import uk.ac.ebi.intact.bridges.taxonomy.NewtTaxonomyService;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.uniprot.service.UniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessorConfig {

    private boolean fixDuplicates = true;

    private boolean deleteProtsWithoutInteractions = true;

    private boolean deleteSpliceVariantsWithoutInteractions = false;

    private UpdateReportHandler reportHandler;

    private UniprotService uniprotService;
    private TaxonomyService taxonomyService;

    private int processBatchSize = 20;
    private int processStepSize = 10;

    public ProteinUpdateProcessorConfig() {
        this.uniprotService = new UniprotRemoteService();
        this.taxonomyService = new NewtTaxonomyService();
    }

    public ProteinUpdateProcessorConfig(UpdateReportHandler reportHandler) {
        this();
        this.reportHandler = reportHandler;
    }

    public boolean isFixDuplicates() {
        return fixDuplicates;
    }

    public void setFixDuplicates(boolean fixDuplicates) {
        this.fixDuplicates = fixDuplicates;
    }

    public boolean isDeleteProtsWithoutInteractions() {
        return deleteProtsWithoutInteractions;
    }

    public void setDeleteProtsWithoutInteractions(boolean deleteProtsWithoutInteractions) {
        this.deleteProtsWithoutInteractions = deleteProtsWithoutInteractions;
    }

    public int getProcessBatchSize() {
        return processBatchSize;
    }

    public void setProcessBatchSize(int processBatchSize) {
        this.processBatchSize = processBatchSize;
    }

    public int getProcessStepSize() {
        return processStepSize;
    }

    public void setProcessStepSize(int processStepSize) {
        this.processStepSize = processStepSize;
    }

    public boolean isDeleteSpliceVariantsWithoutInteractions() {
        return deleteSpliceVariantsWithoutInteractions;
    }

    public void setDeleteSpliceVariantsWithoutInteractions(boolean deleteSpliceVariantsWithoutInteractions) {
        this.deleteSpliceVariantsWithoutInteractions = deleteSpliceVariantsWithoutInteractions;
    }

    public UpdateReportHandler getReportHandler() {
        return reportHandler;
    }

    public void setReportHandler(UpdateReportHandler reportHandler) {
        this.reportHandler = reportHandler;
    }

    public UniprotService getUniprotService() {
        return uniprotService;
    }

    public void setUniprotService(UniprotService uniprotService) {
        this.uniprotService = uniprotService;
    }

    public TaxonomyService getTaxonomyService() {
        return taxonomyService;
    }

    public void setTaxonomyService(TaxonomyService taxonomyService) {
        this.taxonomyService = taxonomyService;
    }
}
