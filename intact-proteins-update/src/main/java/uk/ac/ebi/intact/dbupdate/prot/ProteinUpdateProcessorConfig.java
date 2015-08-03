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
import uk.ac.ebi.intact.dbupdate.prot.errors.DefaultProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.protein.mapping.factories.ReportsFactory;
import uk.ac.ebi.intact.protein.mapping.factories.ResultsFactory;
import uk.ac.ebi.intact.protein.mapping.factories.impl.DefaultReportsFactory;
import uk.ac.ebi.intact.protein.mapping.factories.impl.DefaultResultsFactory;
import uk.ac.ebi.intact.uniprot.service.SimpleUniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;

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
     * @see uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig#deleteProteinTranscriptWithoutInteractions
     */
    private boolean deleteProtsWithoutInteractions = true;

    /**
     * If true, will delete any protein transcript not involved in interactions.
     */
    private boolean deleteProteinTranscriptWithoutInteractions = true;

    /**
     * If true, will put all the proteins we cannot retrieve in uniprot as dead protein, remove all the cross references which are not from intact.
     * The XRef uniprot identity becomes uniprot uniprot-removed-ac
     */
    private boolean processProteinNotFoundInUniprot = true;

    private UpdateReportHandler reportHandler;

    private UniprotService uniprotService;

    private TaxonomyService taxonomyService;

    private boolean isGlobalProteinUpdate = false;

    private boolean isBlastEnabled = true;

    private ProteinUpdateErrorFactory errorFactory;
    private ResultsFactory proteinMappingResultsFactory;
    private ReportsFactory proteinMappingReportFactory;

    public ProteinUpdateProcessorConfig() {
        this.uniprotService = new SimpleUniprotRemoteService();
        final BioSourceService bioSourceService = BioSourceServiceFactory.getInstance().buildBioSourceService();
        this.taxonomyService = bioSourceService.getTaxonomyService();
        this.errorFactory = new DefaultProteinUpdateErrorFactory();
        this.proteinMappingResultsFactory = new DefaultResultsFactory();
        this.proteinMappingReportFactory = new DefaultReportsFactory();
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

    public boolean isDeleteProteinTranscriptWithoutInteractions() {
        return deleteProteinTranscriptWithoutInteractions;
    }

    public void setDeleteProteinTranscriptWithoutInteractions(boolean deleteProteinTranscriptWithoutInteractions) {
        this.deleteProteinTranscriptWithoutInteractions = deleteProteinTranscriptWithoutInteractions;
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

    public boolean isProcessProteinNotFoundInUniprot() {
        return processProteinNotFoundInUniprot;
    }

    public void setProcessProteinNotFoundInUniprot(boolean processProteinNotFoundInUniprot) {
        this.processProteinNotFoundInUniprot = processProteinNotFoundInUniprot;
    }

    public boolean isBlastEnabled() {
        return isBlastEnabled;
    }

    public void setBlastEnabled(boolean blastEnabled) {
        isBlastEnabled = blastEnabled;
    }

    public ProteinUpdateErrorFactory getErrorFactory() {
        return errorFactory;
    }

    public void setErrorFactory(ProteinUpdateErrorFactory errorFactory) {
        this.errorFactory = errorFactory;
    }

    public ResultsFactory getProteinMappingResultsFactory() {
        return proteinMappingResultsFactory;
    }

    public void setProteinMappingResultsFactory(ResultsFactory proteinMappingResultsFactory) {
        this.proteinMappingResultsFactory = proteinMappingResultsFactory;
    }

    public ReportsFactory getProteinMappingReportFactory() {
        return proteinMappingReportFactory;
    }

    public void setProteinMappingReportFactory(ReportsFactory proteinMappingReportFactory) {
        this.proteinMappingReportFactory = proteinMappingReportFactory;
    }
}
