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
import uk.ac.ebi.intact.dbupdate.prot.actions.ProteinTranscript;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.Collection;
import java.util.EventObject;

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

    private UniprotServiceResult uniprotServiceResult;

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
                           Collection<ProteinTranscript> primaryFeatureChains) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.primaryProteins = primaryProteins;
        this.secondaryProteins = secondaryProteins;
        this.primaryIsoforms = primaryIsoforms;
        this.secondaryIsoforms = secondaryIsoforms;
        this.primaryFeatureChains = primaryFeatureChains;
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

    public UniprotServiceResult getUniprotServiceResult() {
        return uniprotServiceResult;
    }

    public void setUniprotServiceResult(UniprotServiceResult uniprotServiceResult) {
        this.uniprotServiceResult = uniprotServiceResult;
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
}