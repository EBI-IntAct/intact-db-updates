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

import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.uniprot.service.UniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.biosource.BioSourceService;

/**
 * Protein update processor config.
 * <p/>
 * It is recommended to get access to this configuration through the <code>ProteinUpdateContext</code>.
 *
 * @see uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessorConfig {

    private boolean fixDuplicates = true;

    /**
     * If true, the processor will actively look at deleting protein, isoforms and chains that are not involved in interactions.
     *
     * @see uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig#deleteSpliceVariantsWithoutInteractions
     * @see uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig#deleteFeatureChainsWithoutInteractions
     */
    private boolean deleteProtsWithoutInteractions = true;

    /**
     * If true, will delete any isoforms not involved in interactions.
     */
    private boolean deleteSpliceVariantsWithoutInteractions = false;

    /**
     * If true, will delete any chains not involved in interactions.
     */
    private boolean deleteFeatureChainsWithoutInteractions = false;

    private UpdateReportHandler reportHandler;

    private UniprotService uniprotService;

    private TaxonomyService taxonomyService;

    private boolean isGlobalProteinUpdate = false;

    ProteinUpdateProcessorConfig() {
        this.uniprotService = new UniprotRemoteService();
        final BioSourceService bioSourceService = BioSourceServiceFactory.getInstance().buildBioSourceService();
        this.taxonomyService = bioSourceService.getTaxonomyService();
    }

//    ProteinUpdateProcessorConfig(UpdateReportHandler reportHandler) {
//        this();
//        this.reportHandler = reportHandler;
//    }

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

    public boolean isDeleteSpliceVariantsWithoutInteractions() {
        return deleteSpliceVariantsWithoutInteractions;
    }

    public void setDeleteSpliceVariantsWithoutInteractions(boolean deleteSpliceVariantsWithoutInteractions) {
        this.deleteSpliceVariantsWithoutInteractions = deleteSpliceVariantsWithoutInteractions;
    }

    public boolean isDeleteFeatureChainsWithoutInteractions() {
        return deleteFeatureChainsWithoutInteractions;
    }

    public void setDeleteFeatureChainsWithoutInteractions( boolean deleteFeatureChainsWithoutInteractions ) {
        this.deleteFeatureChainsWithoutInteractions = deleteFeatureChainsWithoutInteractions;
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

    public boolean isGlobalProteinUpdate() {
        return isGlobalProteinUpdate;
    }

    public void setGlobalProteinUpdate( boolean globalProteinUpdate ) {
        isGlobalProteinUpdate = globalProteinUpdate;
    }
}
