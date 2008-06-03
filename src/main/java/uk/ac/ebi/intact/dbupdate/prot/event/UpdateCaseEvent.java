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

import uk.ac.ebi.intact.context.DataContext;
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
    private Collection<? extends Protein> primaryProteins;
    private Collection<? extends Protein> secondaryProteins;

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
                           Collection<? extends Protein> primaryProteins,
                           Collection<? extends Protein> secondaryProteins) {
        super(source);
        this.protein = protein;
        this.dataContext = dataContext;
        this.primaryProteins = primaryProteins;
        this.secondaryProteins = secondaryProteins;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public UniprotProtein getProtein() {
        return protein;
    }

    public Collection<? extends Protein> getPrimaryProteins() {
        return primaryProteins;
    }

    public Collection<? extends Protein> getSecondaryProteins() {
        return secondaryProteins;
    }

    public UniprotServiceResult getUniprotServiceResult() {
        return uniprotServiceResult;
    }

    public void setUniprotServiceResult(UniprotServiceResult uniprotServiceResult) {
        this.uniprotServiceResult = uniprotServiceResult;
    }
}