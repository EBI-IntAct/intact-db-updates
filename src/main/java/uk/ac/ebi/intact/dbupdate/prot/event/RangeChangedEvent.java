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
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeChangedEvent implements ProteinProcessorEvent {

    private DataContext dataContext;
    private UpdatedRange updatedRange;

    public RangeChangedEvent(DataContext dataContext, UpdatedRange updatedRange) {
        this.dataContext = dataContext;
        this.updatedRange = updatedRange;
    }

    public DataContext getDataContext() {
        return dataContext;
    }

    public UpdatedRange getUpdatedRange() {
        return updatedRange;
    }
}
