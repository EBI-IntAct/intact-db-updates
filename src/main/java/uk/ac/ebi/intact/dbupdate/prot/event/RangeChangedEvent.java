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
import uk.ac.ebi.intact.model.Range;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeChangedEvent implements ProteinProcessorEvent, MessageContainer {

    private DataContext dataContext;
    private Range oldRange;
    private Range newRange;
    private String message;

    public RangeChangedEvent(DataContext dataContext, Range oldRange, Range newRange, String message) {
        this.dataContext = dataContext;
        this.oldRange = oldRange;
        this.newRange = newRange;
        this.message = message;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public Range getOldRange() {
        return oldRange;
    }

    public Range getNewRange() {
        return newRange;
    }

    public String getMessage() {
        return message;
    }
}
