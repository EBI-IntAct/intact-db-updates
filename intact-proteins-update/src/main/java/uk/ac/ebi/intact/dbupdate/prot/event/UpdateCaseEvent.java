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
package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.utils.AliasUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.ProteinNameUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;

import java.util.*;

/**
 * An event that contains the information found for the formal cases during uniprot update.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UpdateCaseEvent extends EventObject implements ProteinProcessorEvent{

    private UniprotProtein protein;
    private DataContext dataContext;
    private Collection<Protein> primaryProteins;
    private Collection<Protein> secondaryProteins;
    private Collection<ProteinTranscript> primaryIsoforms;
    private Collection<ProteinTranscript> secondaryIsoforms;
    private Collection<ProteinTranscript> primaryFeatureChains;

    /**
     * A collection of retrieved proteins.
     */
    private Set<String> proteins = new HashSet<String>();

    private List<XrefUpdaterReport> xrefUpdaterReports = new ArrayList<XrefUpdaterReport>();
    private List<AliasUpdateReport> aliasUpdaterReports = new ArrayList<AliasUpdateReport>();
    private List<ProteinNameUpdateReport> nameUpdateReports = new ArrayList<ProteinNameUpdateReport>();

    private Map<String, Collection<Annotation>> newAnnotations = new HashMap<String, Collection<Annotation>>();

    /**
     * The query sent to the UniprotService for protein update(ex : P12345).
     */
    private String querySentToService;

    private Map<String, RangeUpdateReport> updatedRanges = new HashMap<String, RangeUpdateReport>();

    /**
     * An event thrown when a specific case is found during update
     *
     * @param source The object on which the Event initially occurred.
     * @param dataContext The DataContext
     * @param primaryProteins The proteins found that contain the uniprot AC as identity
     * @param secondaryProteins The proteins found that contain the uniprot AC as secondary-ac
     */
    public UpdateCaseEvent(Object source, DataContext dataContext,
                           UniprotProtein protein,
                           Collection<Protein> primaryProteins,
                           Collection<Protein> secondaryProteins,
                           Collection<ProteinTranscript> primaryIsoforms,
                           Collection<ProteinTranscript> secondaryIsoforms,
                           Collection<ProteinTranscript> primaryFeatureChains,
                           String querySentToService) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.primaryProteins = primaryProteins;
        this.secondaryProteins = secondaryProteins;
        this.primaryIsoforms = primaryIsoforms;
        this.secondaryIsoforms = secondaryIsoforms;
        this.primaryFeatureChains = primaryFeatureChains;
        this.querySentToService = querySentToService;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public UniprotProtein getProtein() {
        return protein;
    }

    public Collection<Protein> getPrimaryProteins() {
        return primaryProteins;
    }

    public Collection<Protein> getSecondaryProteins() {
        return secondaryProteins;
    }

    public void setPrimaryProteins(Collection<Protein> primaryProteins) {
        this.primaryProteins = primaryProteins;
    }

    public void setSecondaryProteins(Collection<Protein> secondaryProteins) {
        this.secondaryProteins = secondaryProteins;
    }

    public void setPrimaryIsoforms(Collection<ProteinTranscript> primaryIsoforms) {
        this.primaryIsoforms = primaryIsoforms;
    }

    public void setSecondaryIsoforms(Collection<ProteinTranscript> secondaryIsoforms) {
        this.secondaryIsoforms = secondaryIsoforms;
    }

    public void setPrimaryFeatureChains(Collection<ProteinTranscript> primaryFeatureChains) {
        this.primaryFeatureChains = primaryFeatureChains;
    }

    public Collection<ProteinTranscript> getSecondaryIsoforms() {
        return secondaryIsoforms;
    }

    public Collection<ProteinTranscript> getPrimaryIsoforms() {
        return primaryIsoforms;
    }

    public Collection<ProteinTranscript> getPrimaryFeatureChains() {
        return primaryFeatureChains;
    }

    public String getQuerySentToService() {
        return querySentToService;
    }

    public void setQuerySentToService(String querySentToService) {
        this.querySentToService = querySentToService;
    }

    public Set<String> getProteins() {
        return proteins;
    }

    public List<XrefUpdaterReport> getXrefUpdaterReports() {
        return xrefUpdaterReports;
    }

    public void addXrefUpdaterReport(XrefUpdaterReport report) {
        xrefUpdaterReports.add(report);
    }

    public List<AliasUpdateReport> getAliasUpdaterReports() {
        return aliasUpdaterReports;
    }

    public void addAliasUpdaterReport(AliasUpdateReport report) {
        aliasUpdaterReports.add(report);
    }

    public List<ProteinNameUpdateReport> getNameUpdaterReports() {
        return nameUpdateReports;
    }

    public void addNameUpdaterReport(ProteinNameUpdateReport report) {
        nameUpdateReports.add(report);
    }

    public Map<String, Collection<Annotation>> getNewAnnotations() {
        return newAnnotations;
    }

    public void addNewAnnotationReport(String ac, Collection<Annotation> annotations) {
        newAnnotations.put(ac, annotations);
    }

    public void addAllToProteins(Collection<Protein> proteins){
        for (Protein prot : proteins){
            if (prot.getAc() != null){
                this.proteins.add(prot.getAc());
            }
        }
    }

    public Map<String, RangeUpdateReport> getUpdatedRanges() {
        return updatedRanges;
    }
}