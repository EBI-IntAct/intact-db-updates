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
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.FeatureUtils;
import uk.ac.ebi.intact.util.protein.utils.AnnotationUpdateReport;

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
    public void shiftFeatureRanges(Feature feature, String oldSequence, String newSequence, DataContext context, RangeUpdateReport report) {
        if (feature == null) throw new NullPointerException("Feature was null");
        if (oldSequence == null) throw new NullPointerException("Old sequence was null");
        if (newSequence == null) throw new NullPointerException("New sequence was null");

        List<Diff> diffs = DiffUtils.diff(oldSequence, newSequence);

        for (Range range : feature.getRanges()) {
            if (!FeatureUtils.isABadRange(range, oldSequence)){
                Range oldRange = new Range(range.getFromCvFuzzyType(), range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToCvFuzzyType(), range.getToIntervalStart(), range.getToIntervalEnd(), null);
                oldRange.setFullSequence(range.getFullSequence());

                boolean rangeShifted = shiftRange(diffs, range, oldSequence, newSequence, context, report);

                UpdatedRange updatedRange;

                if (rangeShifted) {
                    if (log.isInfoEnabled())
                        log.info("Range shifted from " + oldRange + " to " + range + ": " + logInfo(range));

                    range.prepareSequence(newSequence);
                    context.getDaoFactory().getRangeDao().update(range);

                    updatedRange = new UpdatedRange(oldRange, range);

                    report.getShiftedRanges().add(updatedRange);
                }
            }
        }
    }

    /**
     * Changes the features ranges by analysing the shift in positions after a sequence is changed.
     * @param range The range to update
     * @param oldSequence The old sequence
     * @param newSequence The new sequence
     * @return a collection that contains the ranges that have been updated
     */
    public void shiftFeatureRange(Range range, String oldSequence, String newSequence, DataContext context, RangeUpdateReport report) {
        if (range == null) throw new NullPointerException("Range was null");
        if (oldSequence == null) throw new NullPointerException("Old sequence was null");
        if (newSequence == null) throw new NullPointerException("New sequence was null");

        List<Diff> diffs = DiffUtils.diff(oldSequence, newSequence);

        UpdatedRange updatedRange = null;

        if (!FeatureUtils.isABadRange(range, oldSequence)){
            Range oldRange = new Range(range.getFromCvFuzzyType(), range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToCvFuzzyType(), range.getToIntervalStart(), range.getToIntervalEnd(), null);
            oldRange.setFullSequence(range.getFullSequence());
            oldRange.setUpStreamSequence(range.getUpStreamSequence());
            oldRange.setDownStreamSequence(range.getDownStreamSequence());

            boolean rangeShifted = shiftRange(diffs, range, oldSequence, newSequence, context, report);

            if (rangeShifted) {
                if (log.isInfoEnabled())
                    log.info("Range shifted from " + oldRange + " to " + range + ": " + logInfo(range));

                range.prepareSequence(newSequence);
                context.getDaoFactory().getRangeDao().update(range);

                updatedRange = new UpdatedRange(oldRange, range);

                report.getShiftedRanges().add(updatedRange);
            }
        }
    }

    public Collection<UpdatedRange> prepareFeatureSequences(Feature feature, String newSequence, DataContext context) {
        if (feature == null) throw new NullPointerException("Feature was null");

        List<UpdatedRange> updatedRanges = new ArrayList<UpdatedRange>();

        for (Range range : feature.getRanges()) {
            if (!FeatureUtils.isABadRange(range, newSequence)){
                Range oldRange = new Range(range.getFromCvFuzzyType(), range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToCvFuzzyType(), range.getToIntervalStart(), range.getToIntervalEnd(), null);
                oldRange.setFullSequence(range.getFullSequence());

                if (log.isInfoEnabled())
                    log.info("Prepare sequence of the range " + logInfo(range));

                range.prepareSequence(newSequence);
                context.getDaoFactory().getRangeDao().update(range);

                updatedRanges.add(new UpdatedRange(oldRange, range));
            }
        }

        return updatedRanges;
    }

    public UpdatedRange prepareFeatureSequence(Range range, String newSequence, DataContext context) {
        if (range == null) throw new NullPointerException("Range was null");

        UpdatedRange updatedRange = null;

        if (!FeatureUtils.isABadRange(range, null)){
            if (!FeatureUtils.isABadRange(range, newSequence)){
                String previousFeatureSequence = range.getFullSequence();

                Range oldRange = new Range(range.getFromCvFuzzyType(), range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToCvFuzzyType(), range.getToIntervalStart(), range.getToIntervalEnd(), null);
                oldRange.setFullSequence(range.getFullSequence());

                if (log.isInfoEnabled())
                    log.info("Prepare sequence of the range " + logInfo(range));

                range.prepareSequence(newSequence);
                context.getDaoFactory().getRangeDao().update(range);

                if (previousFeatureSequence == null && range.getFullSequence() == null){
                    return null;
                }
                else if ((previousFeatureSequence == null && range.getFullSequence() != null) || (previousFeatureSequence != null && range.getFullSequence() == null)){
                    updatedRange = new UpdatedRange(oldRange, range);
                }
                else if (previousFeatureSequence.equals(range.getFullSequence())){
                    updatedRange = new UpdatedRange(oldRange, range);
                }
            }
        }

        return updatedRange;
    }

    /**
     * Shift the range positions according to the new sequence
     * @param diffs
     * @param range
     * @param oldSequence
     * @param newSequence
     * @return
     */
    protected boolean shiftRange(List<Diff> diffs, Range range, String oldSequence, String newSequence, DataContext context, RangeUpdateReport report) {
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
        // if not undetermined, we have different cases.
        if (!(range.getFromCvFuzzyType().isUndetermined() || range.getFromCvFuzzyType().isCTerminalRegion() || range.getFromCvFuzzyType().isNTerminalRegion())){

            canShiftFromCvFuzzyType = true;
        }

        // if not undetermined, we can shift the ranges.
        if (!(range.getToCvFuzzyType().isUndetermined() || range.getToCvFuzzyType().isCTerminalRegion() || range.getToCvFuzzyType().isNTerminalRegion())){

            canShiftToCvFuzzyType = true;
        }

        // we create a clone to test the new range positions
        Range clone = new Range(range.getFromCvFuzzyType(), range.getFromIntervalStart(), range.getFromIntervalEnd(), range.getToCvFuzzyType(), range.getToIntervalStart(), range.getToIntervalEnd(), null);

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
            // case we have first amino acid deleted, it is possible the feature is still conserved,
            if (clone.getFromIntervalStart() == 0 && clone.getToIntervalEnd() > 0){
                int supposedStart = clone.getToIntervalEnd() - (range.getToIntervalEnd() - range.getFromIntervalStart());

                clone.setFromIntervalStart(supposedStart);

                if (clone.getFromIntervalEnd() == 0){
                    clone.setFromIntervalEnd(supposedStart);
                }
            }

            // check that the new shifted range is within the new sequence and consistent
            if (!FeatureUtils.isABadRange(clone, newSequence)){
                boolean wasCTerminal = false;
                boolean wasNTerminal = false;

                // the end position was at the end of the sequence and now is not anymore
                if (range.getToIntervalEnd() == oldSequence.length() && clone.getToIntervalEnd() != newSequence.length()){
                    // the range has been shifted but the feature is not at the C-terminal position anymore
                    wasCTerminal = true;
                }
                // the start position was at the beginning of the sequence and now is not anymore
                else if (range.getFromIntervalStart() == 1 && clone.getFromIntervalStart() != 1){
                    // the range has been shifted but the feature is not at the N-terminal position anymore
                    wasNTerminal = true;
                }

                // we prepare the new feature sequence
                clone.prepareSequence(newSequence);
                // the new full feature sequence
                String newFullFeatureSequence = clone.getFullSequence();

                // the full feature sequence was and is still not null
                if (newFullFeatureSequence != null && oldFullFeatureSequence != null){

                    // check that the new feature sequence is the same
                    rangeShifted = checkNewFeatureContent(range, newSequence, rangeShifted, oldFullFeatureSequence, clone, newFullFeatureSequence);
                }
                // the new full feature sequence is null but was not before shifting the ranges
                else if (newFullFeatureSequence == null && oldFullFeatureSequence != null){
                    // the new full sequence couldn't be computed, a problem occured : we can't shift the ranges
                    rangeShifted = false;
                }
                // Either the previous feature sequence was null and is not anymore, or the previous sequence was null and is still null.
                // if it was null, we need to update anyway (maybe a problem from a previous bug when loading xml files)
                // but we check that the feature length is not affected
                else {
                    if (range.getToIntervalEnd() - range.getFromIntervalStart() == clone.getToIntervalEnd() - clone.getFromIntervalStart()
                            && range.getToIntervalEnd() - range.getToIntervalStart() == clone.getToIntervalEnd() - clone.getToIntervalStart()
                            && range.getFromIntervalEnd() - range.getFromIntervalStart() == clone.getFromIntervalEnd() - clone.getFromIntervalStart()){
                        range.setFromIntervalStart(clone.getFromIntervalStart());
                        range.setFromIntervalEnd(clone.getFromIntervalEnd());
                        range.setToIntervalStart(clone.getToIntervalStart());
                        range.setToIntervalEnd(clone.getToIntervalEnd());
                    }
                    else {
                        rangeShifted = false;
                    }
                }

                // get the caution from the DB or create it and persist it
                final DaoFactory daoFactory = context.getDaoFactory();
                CvTopic caution = daoFactory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

                if (caution == null) {
                    caution = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
                }

                Feature f = range.getFeature();
                Collection<Annotation> annotations = f.getAnnotations();

                if (rangeShifted && wasCTerminal){
                    String cautionMessage = "["+range.getAc()+"] The range " + range.toString() + " was C-terminal and is not anymore.";
                    boolean hasAnnotation = false;

                    for (Annotation a : annotations){
                        if (caution.equals(a.getCvTopic())){
                            if (cautionMessage.equals(a.getAnnotationText())){
                                hasAnnotation = true;
                            }
                        }
                    }

                    if (!hasAnnotation){
                        Annotation cautionRange = new Annotation(caution, cautionMessage);
                        daoFactory.getAnnotationDao().persist(cautionRange);

                        f.addAnnotation(cautionRange);
                        daoFactory.getFeatureDao().update(f);

                        if (report.getUpdatedFeatureAnnotations().containsKey(f.getAc())){
                            report.getUpdatedFeatureAnnotations().get(f.getAc()).getAddedAnnotations().add(cautionRange);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getAddedAnnotations().add(cautionRange);
                            report.getUpdatedFeatureAnnotations().put(f.getAc(), annreport);
                        }
                    }
                }
                else if (rangeShifted && wasNTerminal){
                    String cautionMessage = "["+range.getAc()+"] The range " + range.toString() + " was N-terminal and is not anymore.";

                    boolean hasAnnotation = false;

                    for (Annotation a : annotations){
                        if (caution.equals(a.getCvTopic())){
                            if (cautionMessage.equals(a.getAnnotationText())){
                                hasAnnotation = true;
                            }
                        }
                    }

                    if (!hasAnnotation){
                        Annotation cautionRange = new Annotation(caution, cautionMessage);
                        daoFactory.getAnnotationDao().persist(cautionRange);

                        f.addAnnotation(cautionRange);
                        daoFactory.getFeatureDao().update(f);

                        if (report.getUpdatedFeatureAnnotations().containsKey(f.getAc())){
                            report.getUpdatedFeatureAnnotations().get(f.getAc()).getAddedAnnotations().add(cautionRange);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getAddedAnnotations().add(cautionRange);
                            report.getUpdatedFeatureAnnotations().put(f.getAc(), annreport);
                        }
                    }
                }
            }
            // one position has been shifted but is not valid
            else {
                // we couldn't shift the ranges properly for one reason
                rangeShifted = false;
            }
        }
        else {
            if (!FeatureUtils.isABadRange(clone, newSequence)){
                // we prepare the new feature sequence
                clone.prepareSequence(newSequence);
                // the new full feature sequence
                String newFullFeatureSequence = clone.getFullSequence();

                // the full feature sequence was and is still not null
                if (newFullFeatureSequence != null && oldFullFeatureSequence != null){

                    // check that the new feature sequence is the same
                    rangeShifted = checkNewFeatureContent(range, newSequence, rangeShifted, oldFullFeatureSequence, clone, newFullFeatureSequence);
                }
            }
        }
        return rangeShifted;
    }

    /**
     * Shift the range positions according to the new sequence
     * @param diffs
     * @param range
     * @param oldSequence
     * @param newSequence
     * @return
     */
    protected InvalidRange collectBadlyShiftedRangeInfo(List<Diff> diffs, Range range, String oldSequence, String newSequence) {
        // to know if we have shifted a position
        boolean rangeShifted = false;
        // to know if it is possible to shift the start positions of the range
        boolean canShiftFromCvFuzzyType = false;
        // to know if it is possible to shift the end positions of the range
        boolean canShiftToCvFuzzyType = false;

        InvalidRange invalidRange = null;

        // the old full feature sequence
        String oldFullFeatureSequence = range.getFullSequence();

        // case 'from': undetermined, cannot be shifted
        // if not undetermined, we have different cases.
        if (!(range.getFromCvFuzzyType().isUndetermined() || range.getFromCvFuzzyType().isCTerminalRegion() || range.getFromCvFuzzyType().isNTerminalRegion())){

            canShiftFromCvFuzzyType = true;
        }

        // if not undetermined, we can shift the ranges.
        if (!(range.getToCvFuzzyType().isUndetermined() || range.getToCvFuzzyType().isCTerminalRegion() || range.getToCvFuzzyType().isNTerminalRegion())){

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

            // case we have first amino acid deleted, it is possible the feature is still conserved,
            if (clone.getFromIntervalStart() == 0 && clone.getToIntervalEnd() > 0){
                int supposedStart = clone.getToIntervalEnd() - (range.getToIntervalEnd() - range.getFromIntervalStart());

                clone.setFromIntervalStart(supposedStart);

                if (clone.getFromIntervalEnd() == 0){
                    clone.setFromIntervalEnd(supposedStart);
                }
            }

            // check that the new shifted range is within the new sequence and consistent
            if (!FeatureUtils.isABadRange(clone, newSequence)){

                // we prepare the new feature sequence
                clone.prepareSequence(newSequence);
                // the new full feature sequence
                String newFullFeatureSequence = clone.getFullSequence();

                // the full feature sequence was and is still not null
                if (newFullFeatureSequence != null && oldFullFeatureSequence != null){

                    // check that the new feature sequence is the same
                    invalidRange = collectInvalidFeatureContent(range, newSequence, oldFullFeatureSequence, clone, newFullFeatureSequence);
                }
                // the new full feature sequence is null but was not before shifting the ranges
                else if (newFullFeatureSequence == null && oldFullFeatureSequence != null){
                    String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                    String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                    // the new full sequence couldn't be computed, a problem occured : we can't shift the ranges
                    invalidRange = new InvalidRange(range, clone, newSequence, "The feature sequence for the ranges ("+clone.toString()+") cannot be computed.", fromStatus, toStatus, true);
                }
            }
            // one position has been shifted but is not valid
            else {
                String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                // we couldn't shift the ranges properly for one reason
                invalidRange = new InvalidRange(range, clone, newSequence, "It was impossible to shift the feature ranges when the protein sequence has been updated.", fromStatus, toStatus, true);
            }
        }
        else {
            if (FeatureUtils.isABadRange(clone, newSequence)){
                String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                invalidRange = new InvalidRange(range, clone, newSequence, "The ranges ("+clone.toString()+") are not valid anymore with the new uniprot sequence.", fromStatus, toStatus, true);
            }
            else {
                // we prepare the new feature sequence
                clone.prepareSequence(newSequence);
                // the new full feature sequence
                String newFullFeatureSequence = clone.getFullSequence();

                // the full feature sequence was and is still not null
                if (newFullFeatureSequence != null && oldFullFeatureSequence != null){

                    // check that the new feature sequence is the same
                    invalidRange = collectInvalidFeatureContent(range, newSequence, oldFullFeatureSequence, clone, newFullFeatureSequence);
                }
                // the new full feature sequence is null but was not before shifting the ranges
                else if (newFullFeatureSequence == null && oldFullFeatureSequence != null){
                    String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                    String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                    // the new full sequence couldn't be computed, a problem occured : we can't shift the ranges
                    invalidRange = new InvalidRange(range, clone, newSequence, "The feature sequence for the ranges ("+clone.toString()+") cannot be computed.", fromStatus, toStatus, true);
                }
            }
        }

        return invalidRange;
    }

    /**
     *
     * @param range : the range to update
     * @param newSequence : the new sequence of the protein
     * @param rangeShifted : the current value of rangeshifted
     * @param oldFeatureSequence : the old feature sequence
     * @param clone : the range clone with the shifted positions
     * @param newFeatureSequence : the new feature sequence
     * @return true if the shifted ranges are valid and the feature sequence is conserved
     */
    protected boolean checkNewFeatureContent(Range range, String newSequence, boolean rangeShifted, String oldFeatureSequence, Range clone, String newFeatureSequence) {
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
                    rangeShifted = false;
                }
            }
            // we can't correct the positions of the ranges to have the conserved feature sequence
            else {
                // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                rangeShifted = false;
            }
        }
        return rangeShifted;
    }

    protected InvalidRange collectInvalidFeatureContent(Range range, String newSequence, String oldFeatureSequence, Range clone, String newFeatureSequence) {
        InvalidRange invalidRange = null;

        // the feature sequence is not conserved. We need to check if there have been some inserts at the beginning of the feature which could explain
        // this problem. For that, we check if the new feature sequence contains the old feature sequence
        if (!newFeatureSequence.equals(oldFeatureSequence)) {
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
                if (correctedFromIntervalEnd > correctedToIntervalStart || correctedFromIntervalEnd > newSequence.length() || correctedToIntervalStart < 1){
                    String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                    String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                    // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                    new InvalidRange(range, clone, newSequence, "The shifted ranges ("+clone.toString()+") couldn't be applied because it is out of bound or overlapping with the new protein sequence.", fromStatus, toStatus, true);
                }
            }
            // we can't correct the positions of the ranges to have the conserved feature sequence
            else {
                String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                // the feature sequence has been changed, we need a curator to check this one, can't shift the ranges
                invalidRange = new InvalidRange(range, clone, newSequence, "The shifted ranges ("+clone.toString()+") couldn't be applied as the new full feature sequence is different from the previous one.", fromStatus, toStatus, true);
            }
        }
        return invalidRange;
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

    protected String logInfo(Range range) {
        final Feature feature = range.getFeature();
        final Component component = feature.getComponent();
        final Interactor interactor = component.getInteractor();
        return "Range["+range.getAc()+"], Feature["+ feature.getAc()+","+ feature.getShortLabel()+"], Component["+ component.getAc()+
                "], Protein["+ interactor.getAc()+","+ interactor.getShortLabel()+"]";
    }

    public Collection<InvalidRange> collectRangesImpossibleToShift(Feature feature, String oldSequence, String newSequence){
        if (feature == null){
            throw new IllegalArgumentException("The feature should not be null");
        }
        if (newSequence == null){
            throw new IllegalArgumentException("The new protein sequence should not be null");
        }

        Collection<InvalidRange> invalidRanges = new ArrayList<InvalidRange>();

        List<Diff> diffs = new ArrayList<Diff>();
        if (oldSequence != null){
            diffs = DiffUtils.diff(oldSequence, newSequence);
        }

        for (Range range : feature.getRanges()) {
            if (oldSequence != null){
                String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);
                if (isABadRange == null){
                    InvalidRange invalid = collectBadlyShiftedRangeInfo(diffs, range, oldSequence, newSequence);
                    if (invalid != null){
                        invalidRanges.add(invalid);
                    }
                }
                else {
                    String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                    String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                    InvalidRange invalid = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
                    invalidRanges.add(invalid);
                }
            }
            else {
                String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);
                String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                if (isABadRange != null){
                    InvalidRange invalid = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
                    invalidRanges.add(invalid);
                }
                else {
                    String isABadRange2 = FeatureUtils.getBadRangeInfo(range, newSequence);

                    if (isABadRange2 != null){
                        InvalidRange invalid = new InvalidRange(range, null, newSequence, isABadRange2, fromStatus, toStatus, false);
                        invalidRanges.add(invalid);
                    }
                }
            }
        }

        return invalidRanges;
    }

    public InvalidRange collectRangeImpossibleToShift(Range range, String oldSequence, String newSequence){
        if (range == null){
            throw new IllegalArgumentException("The range should not be null");
        }
        if (newSequence == null){
            throw new IllegalArgumentException("The new protein sequence should not be null");
        }

        InvalidRange invalidRange = null;

        List<Diff> diffs = new ArrayList<Diff>();
        if (oldSequence != null){
            diffs = DiffUtils.diff(oldSequence, newSequence);
        }

        if (oldSequence != null){
            String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);
            if (isABadRange == null){
                InvalidRange invalid = collectBadlyShiftedRangeInfo(diffs, range, oldSequence, newSequence);
                if (invalid != null){
                    invalidRange = invalid;
                }
            }
            else {
                String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
                String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

                InvalidRange invalid = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
                invalidRange = invalid;
            }
        }
        else {
            String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);
            String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
            String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;

            if (isABadRange != null){
                invalidRange = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
            }
            else {
                String isABadRange2 = FeatureUtils.getBadRangeInfo(range, newSequence);

                if (isABadRange2 != null){
                    invalidRange = new InvalidRange(range, null, newSequence, isABadRange2, fromStatus, toStatus, false);
                }
            }
        }

        return invalidRange;
    }

    public InvalidRange collectRangeInvalidWithCurrentSequence(Range range, String oldSequence){
        if (range == null){
            throw new IllegalArgumentException("The range should not be null");
        }


        InvalidRange invalidRange = null;
        String fromStatus = range.getFromCvFuzzyType() != null ? range.getFromCvFuzzyType().getShortLabel() : null;
        String toStatus = range.getToCvFuzzyType() != null ? range.getToCvFuzzyType().getShortLabel() : null;
        if (oldSequence != null){
            String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);
            if (isABadRange != null){
                InvalidRange invalid = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
                invalidRange = invalid;
            }
        }
        else {
            String isABadRange = FeatureUtils.getBadRangeInfo(range, oldSequence);

            if (isABadRange != null){
                invalidRange = new InvalidRange(range, null, oldSequence, isABadRange, fromStatus, toStatus, false);
            }
        }

        return invalidRange;
    }
}
