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

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;

/**
 * Basic implementation of the ProteinProcessorListener
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class AbstractProteinProcessorListener implements ProteinProcessorListener {

    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProcess(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onPreDelete(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinDuplicationFound(MultiProteinEvent evt) throws ProcessorException {

    }

    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {

    }

    protected String protInfo(Protein protein) {
        return protein.getShortLabel()+" ("+protein.getAc()+")";
    }

}
