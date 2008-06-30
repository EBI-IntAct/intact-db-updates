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
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.RangeChangedEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;

import java.util.List;
import java.util.Collection;

/**
 * Listens for sequence changes and updates the ranges of the features.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeFixer extends AbstractProteinUpdateProcessorListener {

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        RangeChecker rangeChecker = new RangeChecker();

        for (Component component : evt.getProtein().getActiveInstances()) {
            for (Feature feature : component.getBindingDomains()) {
                Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, evt.getOldSequence(), evt.getProtein().getSequence());

                // fire the events for the range changes
                for (UpdatedRange updatedRange : updatedRanges) {
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnRangeChange(new RangeChangedEvent(evt.getDataContext(),
                                                                      updatedRange.getOldRange(),
                                                                      updatedRange.getNewRange(),
                                                                      updatedRange.getMessage()));
                }
            }
        }
    }
}
