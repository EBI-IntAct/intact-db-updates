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

import java.util.Collection;
import java.util.EventObject;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class MultiProteinEvent extends EventObject implements ProteinProcessorEvent {

    private Collection<Protein> proteins;
    private DataContext dataContext;

    private Protein referenceProtein;

    private boolean finalizationRequested;

    /**
     * An event involving a list of proteins.
     */
    public MultiProteinEvent(Object source, DataContext dataContext, Collection<Protein> proteins) {
        super(source);
        this.proteins = proteins;
        this.dataContext = dataContext;
    }

    public void requestFinalization() {
        this.finalizationRequested = true;
    }

    public boolean isFinalizationRequested() {
        return finalizationRequested;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public Collection<Protein> getProteins() {
        return proteins;
    }

    public Protein getReferenceProtein() {
        return referenceProtein;
    }

    public void setReferenceProtein(Protein referenceProtein) {
        this.referenceProtein = referenceProtein;
    }
}