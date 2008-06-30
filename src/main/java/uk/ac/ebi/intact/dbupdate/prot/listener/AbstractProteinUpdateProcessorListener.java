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
package uk.ac.ebi.intact.dbupdate.prot.listener;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.model.Protein;

/**
 * Basic implementation of the ProteinProcessorListener
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class AbstractProteinUpdateProcessorListener implements ProteinProcessorListener, ProteinUpdateProcessorListener {

    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProcess(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onDelete(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {
         // nothing
    }

    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        // nothing
    }

    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onRangeChanged(RangeChangedEvent evt) throws ProcessorException {
        // nothing
    }

    protected String protInfo(Protein protein) {
        return protein.getShortLabel()+" ("+protein.getAc()+")";
    }
}
