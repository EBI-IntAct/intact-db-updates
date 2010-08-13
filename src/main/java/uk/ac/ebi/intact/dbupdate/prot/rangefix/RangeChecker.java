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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Interactor;
import uk.ac.ebi.intact.model.Range;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.clone.IntactClonerException;
import uk.ac.ebi.intact.model.util.FeatureUtils;

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
     * Changes the features ranges by analysing the shift in positions after a sequence is changed.
     * @param feature The feature to update
     * @param oldSequence The old sequence
     * @param newSequence The new sequence
     * @return a collection that contains the ranges that have been updated
     */
    public Collection<UpdatedRange> shiftFeatureRanges(Feature feature, String oldSequence, String newSequence, ProteinUpdateProcessor processor) {
        if (feature == null) throw new NullPointerException("Feature was null");
        if (oldSequence == null) throw new NullPointerException("Old sequence was null");
        if (newSequence == null) throw new NullPointerException("New sequence was null");

        List<Diff> diffs = DiffUtils.diff(oldSequence, newSequence);

        List<UpdatedRange> updatedRanges = new ArrayList<UpdatedRange>();

        for (Range range : feature.getRanges()) {
            if (!FeatureUtils.isABadRange(range, oldSequence)){
                final IntactCloner intactCloner = new IntactCloner();
                Range oldRange = null;
                try {
                    oldRange = intactCloner.clone(range);
                } catch (IntactClonerException e) {
                    throw new IntactException("Could not clone range: "+range, e);
                }

                boolean rangeShifted = shiftRange(diffs, range, oldSequence, newSequence, processor);

                if (rangeShifted) {
                    if (log.isInfoEnabled())
                        log.info("Range shifted from " + oldRange + " to " + range + ": " + logInfo(range));

                    range.prepareSequence(newSequence);

                    updatedRanges.add(new UpdatedRange(oldRange, range));
                }
            }
        }

        return updatedRanges;
    }

    protected boolean shiftRange(List<Diff> diffs, Range range, String oldSequence, String newSequence, ProteinUpdateProcessor processor) {
        boolean rangeShifted = false;
        boolean canShiftToCvFuzzyType = false;
        boolean canShiftFromCvFuzzyType = false;

        String oldFullFeatureSequence = range.getFullSequence();
        String oldTruncatedFeatureSequence = range.getSequence();

        // case: n-terminal or c-terminal
        if (range.getToCvFuzzyType() != null) {
            if (!range.getToCvFuzzyType().isCTerminal() && !range.getToCvFuzzyType().isNTerminal() && !range.getToCvFuzzyType().isUndetermined()){
                canShiftToCvFuzzyType = true;
            }
        }
        else {
            canShiftToCvFuzzyType = true;
        }

        if (range.getFromCvFuzzyType() != null) {
            if (!range.getFromCvFuzzyType().isCTerminal() && !range.getFromCvFuzzyType().isNTerminal() && !range.getFromCvFuzzyType().isUndetermined()){
                canShiftFromCvFuzzyType = true;
            }
        }
        else {
            canShiftFromCvFuzzyType = true;
        }

        Range clone = new Range(range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToIntervalStart(), range.getToIntervalEnd(), oldSequence);
        clone.setFromCvFuzzyType(range.getFromCvFuzzyType());
        clone.setToCvFuzzyType(range.getToCvFuzzyType());

        // If we can shift the positions, calculate the shift of each position based on the diffs between the old and the new sequences.
        // We need to apply a correction (-1/+1) because the shift calculation is index based (0 is the first position),
        // whereas the range positions are not (first position is 1)

        if (canShiftFromCvFuzzyType){
            int shiftedFromIntervalStart = calculatePositionShift(diffs, range.getFromIntervalStart(), oldSequence);

            if (shiftedFromIntervalStart != range.getFromIntervalStart()) {
                clone.setFromIntervalStart(shiftedFromIntervalStart);
                rangeShifted = true;
            }

            int shiftedFromIntervalEnd = calculatePositionShift(diffs, range.getFromIntervalEnd(), oldSequence);

            if (shiftedFromIntervalEnd != range.getFromIntervalEnd()) {
                clone.setFromIntervalEnd(shiftedFromIntervalEnd);
                rangeShifted = true;
            }
        }

        if (canShiftToCvFuzzyType){
            int shiftedToIntervalStart = calculatePositionShift(diffs, range.getToIntervalStart(), oldSequence);

            if (shiftedToIntervalStart != range.getToIntervalStart()) {
                clone.setToIntervalStart(shiftedToIntervalStart);
                rangeShifted = true;
            }

            int shiftedToIntervalEnd = calculatePositionShift(diffs, range.getToIntervalEnd(), oldSequence);

            if (shiftedToIntervalEnd != range.getToIntervalEnd()) {
                clone.setToIntervalEnd(shiftedToIntervalEnd);
                rangeShifted = true;
            }
        }

        if (rangeShifted){
            if (!FeatureUtils.isABadRange(clone, newSequence)){

                if ((range.getFromIntervalEnd() == oldSequence.length() && clone.getFromIntervalEnd() != newSequence.length())
                        || (range.getToIntervalEnd() == oldSequence.length() && clone.getToIntervalEnd() != newSequence.length())){
                    // the range has been shifted but the feature is not at the C-terminal position anymore
                    processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The feature ranges have been successfully shifted ("+clone.toString()+") but need to be reviewed because the global sequence of the protein length has been changed and it affects the feature positions (not N or C terminal anymore).", clone.toString())));
                    rangeShifted = false;
                }
                else if ((range.getFromIntervalStart() == 1 && clone.getFromIntervalStart() != 1)
                        || (range.getToIntervalStart() == 1 && clone.getToIntervalStart() != 1)){
                    // the range has been shifted but the feature is not at the N-terminal position anymore
                    processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The feature ranges have been successfully shifted ("+clone.toString()+") but need to be reviewed because the global sequence of the protein length has been changed and it affects the feature positions (not N or C terminal anymore).", clone.toString())));
                    rangeShifted = false;
                }
                else {
                    clone.prepareSequence(newSequence);
                    String newFullFeatureSequence = clone.getFullSequence();
                    String newTruncatedFeatureSequence = clone.getSequence();

                    if (newFullFeatureSequence != null && oldFullFeatureSequence != null){
                        if (newFullFeatureSequence.equals(oldFullFeatureSequence)){
                            range.setFromIntervalStart(clone.getFromIntervalStart());
                            range.setFromIntervalEnd(clone.getFromIntervalEnd());
                            range.setToIntervalStart(clone.getToIntervalStart());
                            range.setToIntervalEnd(clone.getToIntervalEnd());

                            IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().update(range);
                        }
                        else {
                            // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                            processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new full feature sequence is different from the previous one.", clone.toString())));
                            rangeShifted = false;
                        }
                    }
                    else if (newFullFeatureSequence == null && oldFullFeatureSequence != null){
                        // the new full sequence couldn't be computed, a problem occured : we can't shift the ranges
                        processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new full feature sequence cannot be computed.", clone.toString())));
                        rangeShifted = false;
                    }
                    else if (newTruncatedFeatureSequence != null && oldTruncatedFeatureSequence != null){
                        if (newTruncatedFeatureSequence.equals(oldTruncatedFeatureSequence)){
                            range.setFromIntervalStart(clone.getFromIntervalStart());
                            range.setFromIntervalEnd(clone.getFromIntervalEnd());
                            range.setToIntervalStart(clone.getToIntervalStart());
                            range.setToIntervalEnd(clone.getToIntervalEnd());

                            range.prepareSequence(newSequence);

                            IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().update(range);
                        }
                        else {
                            // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                            processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new feature sequence is different from the previous one.", clone.toString())));
                            rangeShifted = false;
                        }
                    }
                    else if (newTruncatedFeatureSequence == null && oldTruncatedFeatureSequence != null){
                        // the new truncated sequence couldn't be computed, a problem occured : we can't shift the ranges
                        processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new feature sequence cannot be computed.", clone.toString())));
                        rangeShifted = false;
                    }
                    else {
                        range.setFromIntervalStart(clone.getFromIntervalStart());
                        range.setFromIntervalEnd(clone.getFromIntervalEnd());
                        range.setToIntervalStart(clone.getToIntervalStart());
                        range.setToIntervalEnd(clone.getToIntervalEnd());

                        IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().update(range);
                    }
                }
            }
            else {
                // we couldn't shift the ranges properly for one reason
                processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "It was impossible to shift the feature ranges when the protein sequence has been updated.", clone.toString())));
                rangeShifted = false;
            }
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
        if (sequencePosition <= 0) {
            throw new IllegalArgumentException("We can't shift a range which is inferior or equal to 0 ("+sequencePosition+")");
        }

        if (sequencePosition > oldSequence.length()) {
            throw new IllegalArgumentException("We can't shift a range ("+sequencePosition+") which is superior to the sequence length ("+oldSequence.length()+")");
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
