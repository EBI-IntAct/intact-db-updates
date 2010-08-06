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
import uk.ac.ebi.intact.core.IntactException;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Interactor;
import uk.ac.ebi.intact.model.Range;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.clone.IntactClonerException;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Fixes the ranges of the features of a protein when updating the sequence.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeChecker {

    private static final Log log = LogFactory.getLog( RangeChecker.class );

    /**
     *
     * @param feature : the feature to check
     * @param sequence : the sequence
     * @return a collection of outOfBoundRanges for all the ranges of the feature which are not within the sequence
     */
    public Collection<OutOfBoundRange> collectOutOfBoundRanges(Feature feature, String sequence){
        if (feature == null) throw new NullPointerException("Feature was null");
        if (sequence == null) throw new NullPointerException("Sequence was null");

        List<OutOfBoundRange> outOfBoundRanges = new ArrayList<OutOfBoundRange>();

        for (Range range : feature.getRanges()) {

            if (!isRangeWithinSequence(sequence, range)){
                outOfBoundRanges.add(new OutOfBoundRange(range, sequence));
            }
        }

        return outOfBoundRanges;
    }

    /**
     * Changes the features ranges by analysing the shift in positions after a sequence is changed.
     * @param feature The feature to update
     * @param oldSequence The old sequence
     * @param newSequence The new sequence
     * @return a collection that contains the ranges that have been updated
     */
    public Collection<UpdatedRange> shiftFeatureRanges(Feature feature, String oldSequence, String newSequence) {
        if (feature == null) throw new NullPointerException("Feature was null");
        if (oldSequence == null) throw new NullPointerException("Old sequence was null");
        if (newSequence == null) throw new NullPointerException("New sequence was null");

        List<Diff> diffs = DiffUtils.diff(oldSequence, newSequence);

        List<UpdatedRange> updatedRanges = new ArrayList<UpdatedRange>();

        for (Range range : feature.getRanges()) {
            final IntactCloner intactCloner = new IntactCloner();
            Range oldRange = null;
            try {
                oldRange = intactCloner.clone(range);
            } catch (IntactClonerException e) {
                throw new IntactException("Could not clone range: "+range, e);
            }

            if (isRangeWithinSequence(oldSequence, range)){
                boolean rangeShifted = shiftRange(diffs, range, oldSequence, newSequence);

                range.prepareSequence(newSequence);

                if (rangeShifted) {
                    if (log.isInfoEnabled())
                        log.info("Range shifted from " + oldRange + " to " + range + ": " + logInfo(range));

                    updatedRanges.add(new UpdatedRange(oldRange, range));
                }
            }
        }

        return updatedRanges;
    }

    protected boolean isRangeWithinSequence(String sequence, Range range){
        if (sequence == null){
            return true;
        }

        int sequenceLength = sequence.length();
        if (range.getFromIntervalEnd() > sequenceLength || range.getToIntervalEnd() > sequenceLength || range.getFromIntervalStart() > sequenceLength || range.getToIntervalStart() > sequenceLength){
            return false;
        }
        return true;
    }

    protected boolean shiftRange(List<Diff> diffs, Range range, String oldSequence, String newSequence) {
        boolean rangeShifted = false;
        int lengthBefore = range.getToIntervalEnd() - range.getFromIntervalStart();

        // case: n-terminal or c-terminal
        if (range.getToCvFuzzyType() != null &&
                (range.getToCvFuzzyType().isCTerminal() ||
                        range.getToCvFuzzyType().isNTerminal())) {
            return rangeShifted;
        }

        // case: from/to was the last aa in the old sequence
        if (oldSequence.length() != newSequence.length()) {
            if (range.getFromIntervalStart() == oldSequence.length()) {
                rangeShifted = true;
                range.setFromIntervalStart(newSequence.length());
                range.setFromIntervalEnd(newSequence.length());
            }

            if (range.getToIntervalStart() >= oldSequence.length()) {
                rangeShifted = true;
                range.setToIntervalStart(newSequence.length());
                range.setToIntervalEnd(newSequence.length());
            }

            if (rangeShifted) {
                return rangeShifted;
            }
        }

        // don't shift the range if it is undetermined, and reset it to 0 if necessary
        if (range.isUndetermined()) {

            if (range.getFromIntervalStart() != 0 ||
                    range.getToIntervalStart() != 0 ||
                    range.getFromIntervalEnd() != 0 ||
                    range.getToIntervalEnd() != 0) {
                rangeShifted = true;
            }

            range.setFromIntervalStart(0);
            range.setToIntervalStart(0);
            range.setFromIntervalEnd(0);
            range.setToIntervalEnd(0);

            return rangeShifted;
        }

        // calculate the shift of each position based on the diffs between the old and the new sequences.
        // We need to apply a correction (-1/+1) because the shift calculation is index based (0 is the first position),
        // whereas the range positions are not (first position is 1)

        int shiftedFromIntervalStart = calculatePositionShift(diffs, range.getFromIntervalStart(), oldSequence);

        if (shiftedFromIntervalStart != range.getFromIntervalStart()) {
            range.setFromIntervalStart(shiftedFromIntervalStart);
            rangeShifted = true;
        }

        int shiftedFromIntervalEnd = calculatePositionShift(diffs, range.getFromIntervalEnd(), oldSequence);

        if (shiftedFromIntervalEnd != range.getFromIntervalEnd()) {
            range.setFromIntervalEnd(shiftedFromIntervalEnd);
            rangeShifted = true;
        }

        int shiftedToIntervalStart = calculatePositionShift(diffs, range.getToIntervalStart(), oldSequence);

        if (shiftedToIntervalStart != range.getToIntervalStart()) {
            range.setToIntervalStart(shiftedToIntervalStart);
            rangeShifted = true;
        }

        int shiftedToIntervalEnd = calculatePositionShift(diffs, range.getToIntervalEnd(), oldSequence);

        if (shiftedToIntervalEnd != range.getToIntervalEnd()) {
            range.setToIntervalEnd(shiftedToIntervalEnd);
            rangeShifted = true;
        }

        int lengthAfter = range.getToIntervalEnd() - range.getFromIntervalStart();

        if (lengthBefore != lengthAfter && log.isWarnEnabled()) {
            log.warn("Range length changed after shifting its position when updating the sequence from "+
                    lengthBefore+" to "+lengthAfter+": "+logInfo(range));
        }

        if (range.getFromIntervalStart() <= 0 ||
                range.getToIntervalEnd() <= 0) {
            range.setUndetermined(true);
        }

        return rangeShifted;
    }

    /**
     * Calculates the shift in position (1-based)
     * @param diffs The differences between the sequences
     * @param sequencePosition The original position in the sequence
     * @return The final position in the sequence. If it couldn't be found, returns 0.
     */
    protected int calculatePositionShift(List<Diff> diffs, int sequencePosition, String oldSequence) {
        if (sequencePosition <= 0) return 0;

        if (sequencePosition > oldSequence.length()) {
            if (log.isWarnEnabled()) log.warn("Index position out of bounds for range: "+sequencePosition+" (new sequence length: "+oldSequence.length());
            sequencePosition = oldSequence.length();
        }

        int index = sequencePosition-1; // index 0-based (sequence is 1-based)
        int shiftedIndex = DiffUtils.calculateIndexShift(diffs, index);

        int shiftedPos;

        // if exists (is is not -1) return the index 1-based (position)
        // if not, return 0
        if (shiftedIndex > -1) {
            shiftedPos = shiftedIndex+1;
        } else {
            shiftedPos = 0;
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
