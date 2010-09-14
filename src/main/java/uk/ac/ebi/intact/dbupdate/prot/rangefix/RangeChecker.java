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
                    IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().update(range);

                    updatedRanges.add(new UpdatedRange(oldRange, range));
                }
            }
        }

        return updatedRanges;
    }

    protected boolean shiftRange(List<Diff> diffs, Range range, String oldSequence, String newSequence, ProteinUpdateProcessor processor) {
        // to know if we have shifted a position
        boolean rangeShifted = false;
        // to know if it is possible to shift the start positions of the range
        boolean canShiftFromCvFuzzyType = false;
        // to know if it is possible to shift the end positions of the range
        boolean canShiftToCvFuzzyType = false;

        // the old full feature sequence
        String oldFullFeatureSequence = range.getFullSequence();
        // the old truncated sequence (100 aa)
        String oldTruncatedFeatureSequence = range.getSequence();

        // case 'from': undetermined, cannot be shifted
        if (range.getFromCvFuzzyType() != null) {
            // if not undetermined, we have different cases.
            if (!range.getFromCvFuzzyType().isUndetermined()){

                // if the start status is N-terminal and the end status is undetermined, we don't shift the ranges because we don't look at the sequence anyway
                if (range.getFromCvFuzzyType().isNTerminal() && range.getToCvFuzzyType() != null){
                    if (!range.getToCvFuzzyType().isUndetermined()){
                        canShiftFromCvFuzzyType = true;
                    }
                    else {
                        canShiftFromCvFuzzyType = false;
                    }
                }
                else {
                    canShiftFromCvFuzzyType = true;
                }
            }
        }
        // It is not a fuzzy type, we can shift the ranges
        else {
            canShiftFromCvFuzzyType = true;
        }

        // case 'to': undetermined, cannot be shifted
        if (range.getToCvFuzzyType() != null) {
            // if not undetermined, we can shift the ranges.
            if (!range.getToCvFuzzyType().isUndetermined()){

                // if the end status is C-terminal and the start status is undetermined, we don't shift the ranges because we don't look at the sequence anyway.
                // However, it is necessary to update the position of the last amino acid
                if (range.getToCvFuzzyType().isCTerminal() && range.getFromCvFuzzyType() != null){
                    if (!range.getFromCvFuzzyType().isUndetermined()){
                        canShiftToCvFuzzyType = true;
                    }
                    // we update the position of the c-terminus if the new sequence is not null
                    else{
                        canShiftToCvFuzzyType = false;

                        if (newSequence != null){
                            range.setToIntervalStart(newSequence.length());
                            range.setToIntervalEnd(newSequence.length());

                            IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().update(range);
                        }
                    }
                }
                else {
                    canShiftToCvFuzzyType = true;
                }
            }
        }
        // It is not a fuzzy type, we can shift the ranges
        else {
            canShiftToCvFuzzyType = true;
        }

        // we create a clone to test the new range positions
        Range clone = new Range(range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToIntervalStart(), range.getToIntervalEnd(), null);
        clone.setFromCvFuzzyType(range.getFromCvFuzzyType());
        clone.setToCvFuzzyType(range.getToCvFuzzyType());

        // If we can shift the positions, calculate the shift of each position based on the diffs between the old and the new sequences.
        // We need to apply a correction (-1/+1) because the shift calculation is index based (0 is the first position),
        // whereas the range positions are not (first position is 1)
        int shiftedFromIntervalStart;
        int shiftedFromIntervalEnd;
        int shiftedToIntervalStart;
        int shiftedToIntervalEnd;

        // we can shift the start positions, the range is not undetermined
        if (canShiftFromCvFuzzyType){
            shiftedFromIntervalStart = calculatePositionShift(diffs, range.getFromIntervalStart(), oldSequence);

            if (shiftedFromIntervalStart != range.getFromIntervalStart()) {
                clone.setFromIntervalStart(shiftedFromIntervalStart);
                rangeShifted = true;
            }

            shiftedFromIntervalEnd = calculatePositionShift(diffs, range.getFromIntervalEnd(), oldSequence);

            if (shiftedFromIntervalEnd != range.getFromIntervalEnd()) {
                clone.setFromIntervalEnd(shiftedFromIntervalEnd);
                rangeShifted = true;
            }
        }

        // we can shift the end positions, the range is not undetermined
        if (canShiftToCvFuzzyType){
            shiftedToIntervalStart = calculatePositionShift(diffs, range.getToIntervalStart(), oldSequence);

            if (shiftedToIntervalStart != range.getToIntervalStart()) {
                clone.setToIntervalStart(shiftedToIntervalStart);
                rangeShifted = true;
            }

            shiftedToIntervalEnd = calculatePositionShift(diffs, range.getToIntervalEnd(), oldSequence);

            if (shiftedToIntervalEnd != range.getToIntervalEnd()) {
                clone.setToIntervalEnd(shiftedToIntervalEnd);
                rangeShifted = true;
            }
        }

        // One of the range positions has been shifted
        if (rangeShifted){

            // check that the new shifted range is within the new sequence and consistent
            if (!FeatureUtils.isABadRange(clone, newSequence)){

                // the end position was at the end of the sequence and now is not anymore
                if (range.getToIntervalEnd() == oldSequence.length() && clone.getToIntervalEnd() != newSequence.length()){
                    // the range has been shifted but the feature is not at the C-terminal position anymore
                    processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The feature ranges have been successfully shifted ("+clone.toString()+") but need to be reviewed because the global sequence of the protein length has been changed and it affects the feature positions (not N or C terminal anymore).", clone.toString())));
                    rangeShifted = false;
                }
                // the start position was at the beginning of the sequence and now is not anymore
                else if (range.getFromIntervalStart() == 1 && clone.getFromIntervalStart() != 1){
                    // the range has been shifted but the feature is not at the N-terminal position anymore
                    processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The feature ranges have been successfully shifted ("+clone.toString()+") but need to be reviewed because the global sequence of the protein length has been changed and it affects the feature positions (not N or C terminal anymore).", clone.toString())));
                    rangeShifted = false;
                }
                // the intern range has been shifted
                else {

                    // we prepare the new feature sequence
                    clone.prepareSequence(newSequence);
                    // the new full feature sequence
                    String newFullFeatureSequence = clone.getFullSequence();
                    // the new truncated feature sequence
                    String newTruncatedFeatureSequence = clone.getSequence();

                    // the full feature sequence was and is still not null
                    if (newFullFeatureSequence != null && oldFullFeatureSequence != null){

                        // check that the new feature sequence is the same
                        rangeShifted = checkNewFeatureContent(range, newSequence, processor, rangeShifted, oldFullFeatureSequence, clone, newFullFeatureSequence);
                    }
                    // the new full feature sequence is null but was not before shifting the ranges
                    else if (newFullFeatureSequence == null && oldFullFeatureSequence != null){
                        // the new full sequence couldn't be computed, a problem occured : we can't shift the ranges
                        processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new full feature sequence cannot be computed.", clone.toString())));
                        rangeShifted = false;
                    }
                    // the truncated sequence was and is still not null
                    else if (newTruncatedFeatureSequence != null && oldTruncatedFeatureSequence != null){
                        // check that the new feature sequence is the same
                        rangeShifted = checkNewFeatureContent(range, newSequence, processor, rangeShifted, oldTruncatedFeatureSequence, clone, newTruncatedFeatureSequence);
                    }
                    // the new truncated sequence is null and was not before shifting the ranges
                    else if (newTruncatedFeatureSequence == null && oldTruncatedFeatureSequence != null){
                        // the new truncated sequence couldn't be computed, a problem occured : we can't shift the ranges
                        processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new feature sequence cannot be computed.", clone.toString())));
                        rangeShifted = false;
                    }
                    // Either the previous feature sequence was null and is not anymore, or the previous sequence was null and is still null.
                    // if it was null, we need to update anyway (maybe a problem from a previous bug when loading xml files)
                    else {
                        range.setFromIntervalStart(clone.getFromIntervalStart());
                        range.setFromIntervalEnd(clone.getFromIntervalEnd());
                        range.setToIntervalStart(clone.getToIntervalStart());
                        range.setToIntervalEnd(clone.getToIntervalEnd());
                    }
                }
            }
            // one position has been shifted but is not valid
            else {
                // we couldn't shift the ranges properly for one reason
                processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "It was impossible to shift the feature ranges when the protein sequence has been updated.", clone.toString())));
                rangeShifted = false;
            }
        }

        return rangeShifted;
    }

    /**
     *
     * @param range : the range to update
     * @param newSequence : the new sequence of the protein
     * @param processor : the processor in case we have an invalidRangeEvent to fire
     * @param rangeShifted : the current value of rangeshifted
     * @param oldFeatureSequence : the old feature sequence
     * @param clone : the range clone with the shifted positions
     * @param newFeatureSequence : the new feature sequence
     * @return true if the shifted ranges are valid and the feature sequence is conserved
     */
    private boolean checkNewFeatureContent(Range range, String newSequence, ProteinUpdateProcessor processor, boolean rangeShifted, String oldFeatureSequence, Range clone, String newFeatureSequence) {
        // the feature sequence is conserved, we can update the range
        if (newFeatureSequence.equals(oldFeatureSequence)){
            range.setFromIntervalStart(clone.getFromIntervalStart());
            range.setFromIntervalEnd(clone.getFromIntervalEnd());
            range.setToIntervalStart(clone.getToIntervalStart());
            range.setToIntervalEnd(clone.getToIntervalEnd());
        }
        // the feature sequence is not conserved. We need to check if there have been some inserts at the beginning of the feature which could explain
        // this problem. For that, we check if the new feature sequence contains the old feature sequence
        else {
            int indexOfOldFeatureSequence = newFeatureSequence.indexOf(oldFeatureSequence);

            // the new feature sequence contains the old feature sequence : we can correct the positions
            if (indexOfOldFeatureSequence != -1){
                // in case of insertion at the begining of the feature sequence, we could not have shifted the range because the insertion
                int correctedPosition = clone.getFromIntervalStart() + newFeatureSequence.indexOf(oldFeatureSequence);
                // from interval end : the distance between from interval start and from interval end should be conserved. We can determine
                // the new from interval end from that.
                int correctedFromIntervalEnd = correctedPosition + (clone.getFromIntervalEnd() - clone.getFromIntervalStart());
                // to interval end : we have corrected the first position of the feature, we can determine the end position by adding the length of the
                // feature sequence to the corrected first position.
                int correctedToIntervalEnd = correctedPosition + oldFeatureSequence.length() - 1;
                // to interval end : the distance between to interval start and to interval end should be conserved. We can determine
                // the new from interval end from that.
                int correctedToIntervalStart = correctedToIntervalEnd - (clone.getFromIntervalEnd() - clone.getFromIntervalStart());

                // check that the corrected positions are not overlapping because the start/end intervals are conserved
                if (correctedFromIntervalEnd <= correctedToIntervalStart && correctedFromIntervalEnd <= newSequence.length() && correctedToIntervalStart >= 1){
                    range.setFromIntervalStart(correctedPosition);
                    range.setFromIntervalEnd(correctedFromIntervalEnd);
                    range.setToIntervalStart(correctedToIntervalStart);
                    range.setToIntervalEnd(correctedToIntervalEnd);
                }
                // we can't correct the positions of the ranges to have the conserved feature sequence
                else {
                    // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                    processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied because it is out of bound or overlapping.", clone.toString())));
                    rangeShifted = false;
                }
            }
            // we can't correct the positions of the ranges to have the conserved feature sequence
            else {
                // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                processor.fireOnInvalidRange(new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), new InvalidRange(range, newSequence, "The new feature ranges ("+clone.toString()+") couldn't be applied as the new full feature sequence is different from the previous one.", clone.toString())));
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
