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
package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.commons.util.DiffUtils;
import uk.ac.ebi.intact.commons.util.diff.Diff;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Interactor;
import uk.ac.ebi.intact.model.Range;

import java.util.Collection;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Fixes the ranges of the features of a protein when updating the sequence.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeChecker {

    private static final Log log = LogFactory.getLog( RangeChecker.class );

    /**
     * Changes the features ranges by analysing the shift in positions after a sequence is changed.
     * @param features The features to update
     * @param oldSequence The old sequence
     * @param newSequence The new sequence
     */
    public Collection<Feature> shiftFeatureRanges(Collection<Feature> features, String oldSequence, String newSequence) {
        if (features == null) throw new NullPointerException("Feature list was null");
        if (oldSequence == null) throw new NullPointerException("Old sequence was null");
        if (newSequence == null) throw new NullPointerException("New sequence was null");

        // determine the kind of sequence update

        List<Diff> diffs = DiffUtils.diff(oldSequence, newSequence);

        Map<String,Feature> updatedFeatures = new HashMap<String,Feature>();

             for (Feature feature : features) {
                 for (Range range : feature.getRanges()) {
                     String initialRange = range.toString();

                     boolean rangeShifted = shiftRange(diffs, range);

                     if (rangeShifted) {
                         if (log.isInfoEnabled())
                             log.info("Range shifted from " + initialRange + " to " + range.toString() + ": " + logInfo(range));

                         updatedFeatures.put(feature.getAc(), feature);
                     }
                 }
             }

        return updatedFeatures.values();
    }

    protected boolean shiftRange(List<Diff> diffs, Range range) {
        boolean rangeShifted = false;
        int lengthBefore = range.getToIntervalEnd() - range.getFromIntervalStart();

        // calculate the shift of each position based on the diffs between the old and the new sequences.
        // We need to apply a correction (-1/+1) because the shift calculation is index based (0 is the first position),
        // whereas the range positions are not (first position is 1)

        int shiftedFromIntervalStart = calculatePositionShift(diffs, range.getFromIntervalStart());

        if (shiftedFromIntervalStart != range.getFromIntervalStart()) {
            range.setFromIntervalStart(shiftedFromIntervalStart);
            rangeShifted = true;
        }

        int shiftedFromIntervalEnd = calculatePositionShift(diffs, range.getFromIntervalEnd());

        if (shiftedFromIntervalEnd != range.getFromIntervalEnd()) {
            range.setFromIntervalEnd(shiftedFromIntervalEnd);
            rangeShifted = true;
        }

        int shiftedToIntervalStart = calculatePositionShift(diffs, range.getToIntervalStart());

        if (shiftedToIntervalStart != range.getToIntervalStart()) {
            range.setToIntervalStart(shiftedToIntervalStart);
            rangeShifted = true;
        }

        int shiftedToIntervalEnd = calculatePositionShift(diffs, range.getToIntervalEnd());

        if (shiftedToIntervalEnd != range.getToIntervalEnd()) {
            range.setToIntervalEnd(shiftedToIntervalEnd);
            rangeShifted = true;
        }

        int lengthAfter = range.getToIntervalEnd() - range.getFromIntervalStart();

        if (lengthBefore != lengthAfter && log.isWarnEnabled()) {
            log.warn("Range length changed after shifting its position when updating the sequence from "+
                     lengthBefore+" to "+lengthAfter+": "+logInfo(range));
        }

        return rangeShifted;
    }

    /**
     * Calculates the shift in position (1-based)
     * @param diffs The differences between the sequences
     * @param sequencePosition The original position in the sequence
     * @return The final position in the sequence. If it couldn't be found, returns -1.
     */
    protected int calculatePositionShift(List<Diff> diffs, int sequencePosition) {
        int index = sequencePosition-1; // index 0-based (sequence is 1-based)
        int shiftedIndex = DiffUtils.calculateIndexShift(diffs, index);

        int shiftedPos;

        // if exists (is is not -1) return the index 1-based (position)
        // if not, return -1
        if (shiftedIndex > -1) {
            shiftedPos = shiftedIndex+1;
        } else {
            shiftedPos = shiftedIndex;
        }

        return shiftedPos;
    }

    private String logInfo(Range range) {
        final Feature feature = range.getFeature();
        final Component component = feature.getComponent();
        final Interactor interactor = component.getInteractor();
        return "Range["+range.getAc()+"], Feature["+ feature.getAc()+","+ feature.getShortLabel()+"], Component["+ component.getAc()+
           "], Protein["+ interactor.getAc()+","+ interactor.getShortLabel()+"]";
    }
}
