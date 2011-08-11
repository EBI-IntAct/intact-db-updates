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
package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.unisave.UnisaveService;
import uk.ac.ebi.intact.bridges.unisave.UnisaveServiceException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.FeatureDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.actions.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.RangeChangedEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidFeatureReport;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.FeatureUtils;
import uk.ac.ebi.intact.util.protein.utils.AnnotationUpdateReport;

import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Map;

/**
 * updates the ranges of the features whenever it is possible. If impossible to update, the ranges become undetermined
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeFixerImpl implements RangeFixer{

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( RangeFixerImpl.class );

    /**
     * The range checker
     */
    protected RangeChecker checker;

    /**
     * Short label of the cv topic for invalid-positions
     */
    public static final String invalidPositions = "invalid-positions";

    /**
     * in the feature annotation, a range of type range can be separated by this separator
     */
    public static final String rangeSeparator = "..";

    /**
     * in the feature annotation, the range positions are separated by this separator
     */
    public static final String positionsSeparator = "-";

    /**
     * Short label of the cv topic for range-conflicts
     */
    public static final String rangeConflicts = "range-conflicts";

    /**
     * Short label of the cv topic for sequence version
     */
    public static final String sequenceVersion = "sequence-version";

    /**
     * in the feature annotation, the sequence version is separated from the uniprot ac by this separator
     */
    public static final String sequenceVersionSeparator = ",";

    /**
     * The unisave service
     */
    protected UnisaveService unisave;

    public RangeFixerImpl(){
        this.checker = new RangeChecker();
        this.unisave = new UnisaveService();
    }

    /**
     *
     * @param range
     * @return true if the range is considered as undetermined (start range status or end range status is null or
     * start range status and end range status are both undetermined)
     */
    private boolean isInvalidRangeUndetermined(Range range){
        // the range cannot be null
        if (range == null){
            return true;
        }

        if (range.getFromCvFuzzyType() == null){
            return true;
        }
        else if(range.getToCvFuzzyType() == null){
            return true;
        }
        else if(range.getFromCvFuzzyType().isUndetermined() && range.getToCvFuzzyType().isUndetermined() && range.getFromIntervalStart() == 0 && range.getFromIntervalEnd() == range.getFromIntervalStart() && range.getToIntervalStart() == range.getFromIntervalStart() && range.getToIntervalEnd() == range.getFromIntervalStart()){
            return true;
        }

        return false;
    }

    /**
     * Checks if a feature contains annotations for invalid ranges or ranges with conflicts and checks if these annotations are still valids.
     * If not remove them.
     * @param feature
     * @param context
     * @return
     */
    protected Map<String, InvalidFeatureReport> checkConsistencyFeatureBeforeRangeShifting(Feature feature, DataContext context, ProteinUpdateProcessor processor, RangeUpdateReport rangeReport){
        DaoFactory factory = context.getDaoFactory();

        Map<String, AnnotationUpdateReport> featureReport = rangeReport.getUpdatedFeatureAnnotations();

        // a collection containing the feature annotations
        Collection<Annotation> annotationsFeature = new ArrayList<Annotation>();
        annotationsFeature.addAll(feature.getAnnotations());

        // this map contains all the annotations reporting information about a specific range per range ac
        Map<String, InvalidFeatureReport> featureReports = new HashMap<String, InvalidFeatureReport>();

        // this map contains all the ranges acs attached to the current feature and the boolean value is here to say if the range is totally undetermined
        // or not. It will help to know if an annotation is out of date because the invalid range is not undetermined anymore (the annotations
        // are only valid when the concerned ranges are undetermined).
        Map<String, Boolean> existingRanges = new HashMap<String, Boolean>();

        // a list of invalid range acs for what the protein update cannot do anything
        Collection<String> invalidRanges = new ArrayList<String>();

        // collect all the rang acs attached to this feature
        for (Range r : feature.getRanges()){
            if (r.getAc() != null){
                // if one cvFuzzyType is null or both fuzzytypes are undetermined, the range is considered as fully undetermined
                if (isInvalidRangeUndetermined(r)){
                    existingRanges.put(r.getAc(), true);
                }
                else {
                    existingRanges.put(r.getAc(), false);
                }
            }
        }

        FeatureDao featureDao = factory.getFeatureDao();

        // update or remove the feature annotations and collect invalid ranges annotations
        for (Annotation annotation : annotationsFeature){
            // invalid-range
            if (CvTopic.INVALID_RANGE.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // the range ac is not attached to this feature anymore, must delete the annotation
                if (!existingRanges.containsKey(rangeAc)){

                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
                // the range ac still exists
                else{
                    // the range is still undetermined, meaning that it was not fixed, keep the annotation and add the rangeAc to the list of invalid-ranges
                    if (existingRanges.get(rangeAc)){
                        invalidRanges.add(rangeAc);
                    }
                    // the range is not undetermined anymore, meaning that the annotation is out of date and must be removed
                    else{
                        if (featureReport.containsKey(feature.getAc())){
                            featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getRemovedAnnotations().add(annotation);
                            featureReport.put(feature.getAc(), annreport);
                        }

                        ProteinTools.deleteAnnotation(feature, context, annotation);
                    }
                }
            }
            // range-conflicts
            else if (rangeConflicts.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // the range ac is not attached to this feature anymore, must delete the annotation
                if (!existingRanges.containsKey(rangeAc)){
                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
                // the range ac still exists
                else {
                    // the range is not undetermined anymore, meaning that the annotation is out of date and must be removed
                    if (!existingRanges.get(rangeAc)){
                        if (featureReport.containsKey(feature.getAc())){
                            featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getRemovedAnnotations().add(annotation);
                            featureReport.put(feature.getAc(), annreport);
                        }

                        ProteinTools.deleteAnnotation(feature, context, annotation);
                    }
                    // if the map containing the features having range conflicts doesn't contains this range, create a new invalidFeatureReport
                    // and set the range ac of this report
                    else if(!featureReports.containsKey(rangeAc)){
                        InvalidFeatureReport report = new InvalidFeatureReport();
                        report.setRangeAc(rangeAc);
                        featureReports.put(rangeAc, report);
                    }
                }
            }
            // invalid-positions
            else if (invalidPositions.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // the range ac is not attached to this feature anymore, must delete the annotation
                if (!existingRanges.containsKey(rangeAc)){
                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
                // the range ac still exists
                else {
                    // the range is not undetermined anymore, meaning that the annotation is out of date and must be removed
                    if (!existingRanges.get(rangeAc)){
                        if (featureReport.containsKey(feature.getAc())){
                            featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getRemovedAnnotations().add(annotation);
                            featureReport.put(feature.getAc(), annreport);
                        }

                        ProteinTools.deleteAnnotation(feature, context, annotation);
                    }
                    // it is not an invalid-range from the beginning, can be remapped eventually
                    else if(!invalidRanges.contains(rangeAc)){
                        // the feature report contains the range ac, just set the positions of the range
                        if (featureReports.containsKey(rangeAc)){
                            InvalidFeatureReport report = featureReports.get(rangeAc);
                            report.setRangePositions(annotation);
                        }
                        // if the map containing the features having range conflicts doesn't contains this range, create a new invalidFeatureReport
                        // and set the range ac of this report and the positions of the range
                        else{
                            InvalidFeatureReport report = new InvalidFeatureReport();
                            report.setRangeAc(rangeAc);
                            report.setRangePositions(annotation);
                            featureReports.put(rangeAc, report);
                        }
                    }
                }
            }
            // sequence version
            else if (sequenceVersion.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // the range ac is not attached to this feature anymore, must delete the annotation
                if (!existingRanges.containsKey(rangeAc)){
                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
                // the range ac still exists
                else{
                    // the range is not undetermined anymore, meaning that the annotation is out of date and must be removed
                    if (!existingRanges.get(rangeAc)){
                        if (featureReport.containsKey(feature.getAc())){
                            featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getRemovedAnnotations().add(annotation);
                            featureReport.put(feature.getAc(), annreport);
                        }

                        ProteinTools.deleteAnnotation(feature, context, annotation);
                    }
                    // add the sequence version to the proper feature report
                    else if (featureReports.containsKey(rangeAc)){
                        InvalidFeatureReport report = featureReports.get(rangeAc);
                        report.setSequenceVersion(annotation);
                    }
                    // if the map containing the features having range conflicts doesn't contains this range, create a new invalidFeatureReport
                    // and set the range ac of this report and the sequence version
                    else{
                        InvalidFeatureReport report = new InvalidFeatureReport();
                        report.setRangeAc(rangeAc);
                        report.setSequenceVersion(annotation);
                        featureReports.put(rangeAc, report);
                    }
                }
            }
            // caution
            else if (CvTopic.CAUTION_MI_REF.equals(annotation.getCvTopic().getIdentifier())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // the range ac is not attached to this feature anymore, must delete the annotation
                if (!existingRanges.containsKey(rangeAc)){
                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
            }
        }

        // update feature
        featureDao.update(feature);

        // returns all the reports about previous range conflicts for this feature
        return featureReports;
    }

    /**
     * After having shifted the ranges, it is possible that ranges with previous range conflicts have been fixed and we need to
     * remove annotations concerning this ranges at the level of the feature
     * @param feature
     * @param context
     */
    protected void checkConsistencyFeatureAfterRangeShifting(Feature feature, DataContext context, ProteinUpdateProcessor processor, RangeUpdateReport report){
        DaoFactory factory = context.getDaoFactory();

        Map<String, AnnotationUpdateReport> featureReport = report.getUpdatedFeatureAnnotations();

        // a collection containing the feature annotations
        Collection<Annotation> annotationsFeature = new ArrayList<Annotation>();
        annotationsFeature.addAll(feature.getAnnotations());

        // this map contains all the ranges acs attached to the current feature and the boolean value is here to say if the range is totally undetermined
        // or not. It will help to know if an annotation is out of date because the invalid range is not undetermined anymore (the annotations
        // are only valid when the concerned ranges are undetermined).
        Map<String, Boolean> existingInvalidRanges = new HashMap<String, Boolean>();

        // collect all the rang acs attached to this feature
        for (Range r : feature.getRanges()){
            if (r.getAc() != null){
                // if one cvFuzzyType is null or both fuzzytypes are undetermined, the range is considered as fully undetermined
                if (isInvalidRangeUndetermined(r)){
                    existingInvalidRanges.put(r.getAc(), true);
                }
                else {
                    existingInvalidRanges.put(r.getAc(), false);
                }
            }
        }

        FeatureDao featureDao = factory.getFeatureDao();

        // for each annotation attached to this feature
        for (Annotation annotation : annotationsFeature){
            // if it is 'range-conflict', 'sequence-version', 'invalid-positions  => can be obsoletes now because problem fixed when trying to remap ranges?
            if (rangeConflicts.equalsIgnoreCase(annotation.getCvTopic().getShortLabel()) ||
                    invalidPositions.equalsIgnoreCase(annotation.getCvTopic().getShortLabel()) ||
                    sequenceVersion.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                // the range ac
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                // if the range still exists
                if (existingInvalidRanges.containsKey(rangeAc)){
                    // the range is not undetermined anymore, meaning the range has been shifted successfully, the annotation must be deleted
                    if (!existingInvalidRanges.get(rangeAc)){
                        if (featureReport.containsKey(feature.getAc())){
                            featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                        }
                        else {
                            AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                            annreport.getRemovedAnnotations().add(annotation);
                            featureReport.put(feature.getAc(), annreport);
                        }

                        ProteinTools.deleteAnnotation(feature, context, annotation);
                    }
                }
                // delete the annotation
                else {
                    if (featureReport.containsKey(feature.getAc())){
                        featureReport.get(feature.getAc()).getRemovedAnnotations().add(annotation);
                    }
                    else {
                        AnnotationUpdateReport annreport = new AnnotationUpdateReport();
                        annreport.getRemovedAnnotations().add(annotation);
                        featureReport.put(feature.getAc(), annreport);
                    }

                    ProteinTools.deleteAnnotation(feature, context, annotation);
                }
            }
        }

        featureDao.update(feature);
    }

    /**
     * Shift the positions of all ranges attached to a component
     * @param oldSequence
     * @param newSequence
     * @param componentsToUpdate
     * @param processor
     * @param context
     * @throws ProcessorException
     */
    protected void shiftRanges(String oldSequence, String newSequence, Collection<Component> componentsToUpdate, ProteinUpdateProcessor processor, DataContext context, RangeUpdateReport report) throws ProcessorException {

        // oldsequence not null, new sequence not null, the range may have to be shifted
        if (oldSequence != null && newSequence != null) {

            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

                    // shift the ranges whenever it is necessary (don't touch ranges impossible to shift)
                    checker.shiftFeatureRanges(feature, oldSequence, newSequence, context, report);
                }
            }
        }
        // TODO : caution?
        // old sequence null, new sequence not null, the range cannot be shifted but we can now extract a feature sequence
        else if (oldSequence == null && newSequence != null){
            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

                    // extract feature sequence whenever it is necessary (don't touch ranges invalids with the new sequence)
                    Collection<UpdatedRange> updatedRanges = checker.prepareFeatureSequences(feature, newSequence, context);

                    // fire the events for the range changes
                    for (UpdatedRange updatedRange : updatedRanges) {
                        processor.fireOnRangeChange(new RangeChangedEvent(context, updatedRange));
                    }
                }
            }
        }
    }

    /**
     * Shift range positions of a specific range
     * @param oldSequence
     * @param newSequence
     * @param range
     * @param context
     * @throws ProcessorException
     */
    protected void shiftRange(String oldSequence, String newSequence, Range range, DataContext context, RangeUpdateReport report) throws ProcessorException {

        // oldsequence not null, new sequence not null, the range may have to be shifted
        if (oldSequence != null && newSequence != null) {

            // shift the ranges whenever it is necessary (don't touch ranges impossible to shift)
            checker.shiftFeatureRange(range, oldSequence, newSequence, context, report);
        }
        // TODO : caution?
        // old sequence null, new sequence not null, the range cannot be shifted but we can now extract a feature sequence
        else if (oldSequence == null && newSequence != null){

            // extract feature sequence whenever it is necessary (don't touch ranges invalids with the new sequence)            
            checker.prepareFeatureSequence(range, newSequence, context);
        }
    }

    /**
     * Returns the positions of a range as a String (this can only be used for ranges certain or range)
     * @param r
     * @return
     */
    protected String convertPositionsToString(Range r){
        String start;
        String end;

        if (r.getFromIntervalStart() != r.getFromIntervalEnd()){
            start = r.getFromIntervalStart() + rangeSeparator + r.getFromIntervalEnd();
        }
        else {
            start = Integer.toString(r.getFromIntervalStart());
        }

        if (r.getToIntervalStart() != r.getToIntervalEnd()){
            end = r.getToIntervalStart() + rangeSeparator + r.getToIntervalEnd();
        }
        else {
            end = Integer.toString(r.getToIntervalEnd());
        }

        return "["+r.getAc()+"]"+start + positionsSeparator + end;
    }

    /**
     * Fix invalid ranges by adding annotations at the feature level and set the range to undetermined
     * @param evt
     */
    public void fixInvalidRanges(InvalidRangeEvent evt, ProteinUpdateProcessor processor){

        RangeUpdateReport report = evt.getRangeReport();
        // get the range
        Range range = evt.getInvalidRange().getOldRange();
        // get the invalid positions
        String positions = convertPositionsToString(evt.getInvalidRange().getOldRange());

        // the range is not null
        if (range != null){
            // create a prefix for the annotation containing the range ac
            String prefix = "["+range.getAc()+"]";
            // get the message
            String message = prefix +evt.getInvalidRange().getMessage();

            // get the feature
            Feature feature = range.getFeature();

            // if the feature is not null, we can fix the invalid range
            if (feature != null){

                AnnotationUpdateReport annotationReport;
                if (!report.getUpdatedFeatureAnnotations().containsKey(feature.getAc())){
                    annotationReport = new AnnotationUpdateReport();
                    report.getUpdatedFeatureAnnotations().put(feature.getAc(), annotationReport);
                } else {
                    annotationReport = report.getUpdatedFeatureAnnotations().get(feature.getAc());
                }

                // get the invalid_caution from the DB or create it and persist it
                final DaoFactory daoFactory = evt.getDataContext().getDaoFactory();

                // invalid range
                CvTopic invalid_caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.INVALID_RANGE);

                if (invalid_caution == null) {
                    invalid_caution = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, CvTopic.INVALID_RANGE);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(invalid_caution);
                }

                // invalid positions
                CvTopic invalidPositions = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixerImpl.invalidPositions);

                if (invalidPositions == null) {
                    invalidPositions = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixerImpl.invalidPositions);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(invalidPositions);
                }

                // the feature annotations
                Collection<Annotation> annotations = feature.getAnnotations();

                // existing invalid-range annotation
                Annotation invalid = null;
                // existing invalid-positions annotation
                Annotation pos = null;

                for (Annotation a : annotations){
                    // we have a 'invalid-range'
                    if (invalid_caution.equals(a.getCvTopic())){
                        // it starts with the same prefix : concerns same range and need to be updated
                        if (a.getAnnotationText().startsWith(prefix)){
                            invalid = a;
                        }
                    }
                    // we have a invalid-positions
                    else if (invalidPositions.equals(a.getCvTopic())){
                        // it starts with the same prefix : concerns same range and need to be updated
                        if (a.getAnnotationText().startsWith(prefix)){
                            pos = a;
                        }
                    }
                }

                // no invalid range annotation : needs to be created
                if (invalid == null){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);

                    annotationReport.getAddedAnnotations().add(cautionRange);
                }
                // existing invalid range annotation : needs to be updated
                else{
                    invalid.setAnnotationText(message);
                    daoFactory.getAnnotationDao().update(invalid);

                    annotationReport.getAddedAnnotations().add(invalid);
                }

                // no invalid positions annotation : needs to be created
                if (pos == null){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                    annotationReport.getAddedAnnotations().add(invalidPosRange);
                }
                // existing invalid positions annotation : needs to be updated
                else{
                    pos.setAnnotationText(positions);
                    daoFactory.getAnnotationDao().update(pos);
                    annotationReport.getAddedAnnotations().add(pos);
                }

                // fire the event
                processor.fireOnInvalidRange(evt);

                // set the range to undetermined
                setRangeUndetermined(range, daoFactory);

                // update the feature
                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

    /**
     * Fix out of date ranges by adding annotations at the feature level and set the range to undetermined
     * @param evt
     */
    public void fixOutOfDateRanges(InvalidRangeEvent evt, ProteinUpdateProcessor processor){
        RangeUpdateReport report = evt.getRangeReport();

        // get the range
        Range range = evt.getInvalidRange().getNewRange();
        Range currentRange = evt.getInvalidRange().getOldRange();

        // the range is not null
        if (range != null && currentRange != null){
            range.setAc(currentRange.getAc());
            // get the invalid positions
            String positions = convertPositionsToString(range);
            // get the sequence version
            int validSequenceVersion = evt.getInvalidRange().getValidSequenceVersion();

            // create a prefix for the annotation containing the range ac
            String prefix = "["+currentRange.getAc()+"]";

            // get the message
            String message = prefix +evt.getInvalidRange().getMessage();

            // get the valid sequence version with the uniprot ac
            String validSequence = prefix+evt.getInvalidRange().getUniprotAc()+","+validSequenceVersion;

            // get the feature
            Feature feature = currentRange.getFeature();

            // if the feature is not null, we can fix the out of date range
            if (feature != null){

                AnnotationUpdateReport annotationReport;
                if (!report.getUpdatedFeatureAnnotations().containsKey(feature.getAc())){
                    annotationReport = new AnnotationUpdateReport();
                    report.getUpdatedFeatureAnnotations().put(feature.getAc(), annotationReport);
                } else {
                    annotationReport = report.getUpdatedFeatureAnnotations().get(feature.getAc());
                }

                // get the invalid_caution from the DB or create it and persist it
                final DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
                CvTopic invalid_caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixerImpl.rangeConflicts);

                if (invalid_caution == null) {
                    invalid_caution = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixerImpl.rangeConflicts);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(invalid_caution);
                }

                // invalid positions
                CvTopic invalidPositions = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixerImpl.invalidPositions);

                if (invalidPositions == null) {
                    invalidPositions = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixerImpl.invalidPositions);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(invalidPositions);
                }

                // sequence version
                CvTopic sequenceVersion = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixerImpl.sequenceVersion);

                if (sequenceVersion == null) {
                    sequenceVersion = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixerImpl.sequenceVersion);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(sequenceVersion);
                }

                // annotations of the feature
                Collection<Annotation> annotations = feature.getAnnotations();

                // range-conflict annotation
                Annotation invalid = null;
                // invalid-positions annotation
                Annotation pos = null;
                // sequence version annotation
                Annotation sv = null;

                for (Annotation a : annotations){
                    // range-conflict
                    if (invalid_caution.equals(a.getCvTopic())){
                        // it starts with the same prefix : concerns same range and need to be updated
                        if (a.getAnnotationText().startsWith(prefix)){
                            invalid = a;
                        }
                    }
                    // invalid-positions
                    else if (invalidPositions.equals(a.getCvTopic())){
                        // it starts with the same prefix : concerns same range and need to be updated
                        if (a.getAnnotationText().startsWith(prefix)){
                            pos = a;
                        }
                    }
                    // sequence version
                    else if (sequenceVersion.equals(a.getCvTopic())){
                        // it starts with the same prefix : concerns same range and need to be updated
                        if (a.getAnnotationText().startsWith(prefix)){
                            sv = a;
                        }
                    }
                }

                // no range-conflicts annotation : needs to be created
                if (invalid == null){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                    annotationReport.getAddedAnnotations().add(cautionRange);
                }
                // existing range conflict annotation : needs to be updated
                else {
                    invalid.setAnnotationText(message);
                    daoFactory.getAnnotationDao().update(invalid);
                    annotationReport.getAddedAnnotations().add(invalid);
                }

                // no invalid positions annotation : needs to be created
                if (pos == null){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                    annotationReport.getAddedAnnotations().add(invalidPosRange);
                }
                // existing invalid positions annotation : needs to be updated
                else {
                    pos.setAnnotationText(positions);
                    daoFactory.getAnnotationDao().update(pos);
                    annotationReport.getAddedAnnotations().add(pos);
                }

                // no sequence version annotation : needs to be created because a valid sequence version exists for this range
                if (sv == null && validSequenceVersion != -1 && evt.getInvalidRange().getUniprotAc() != null){
                    Annotation validSeqVersion = new Annotation(sequenceVersion, validSequence);
                    daoFactory.getAnnotationDao().persist(validSeqVersion);

                    feature.addAnnotation(validSeqVersion);
                    annotationReport.getAddedAnnotations().add(validSeqVersion);
                }
                // existing sequence version annotation : needs to be updated because a valid sequence version exists for this range                
                else if (sv != null && validSequenceVersion != -1 && evt.getInvalidRange().getUniprotAc() != null) {
                    sv.setAnnotationText(validSequence);
                    daoFactory.getAnnotationDao().update(sv);
                    annotationReport.getAddedAnnotations().add(sv);
                }

                processor.fireOnOutOfDateRange(evt);

                // set the range to undetermined
                setRangeUndetermined(currentRange, daoFactory);

                // update feature
                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

    /**
     * Set a range to undetermined
     * @param r
     * @param f
     */
    protected void setRangeUndetermined(Range r, DaoFactory f){
        // undetermined status
        CvFuzzyType undetermined = f.getCvObjectDao(CvFuzzyType.class).getByPsiMiRef(CvFuzzyType.UNDETERMINED_MI_REF);

        if (undetermined == null) {
            undetermined = CvObjectUtils.createCvObject(r.getOwner(), CvFuzzyType.class, CvFuzzyType.UNDETERMINED_MI_REF, CvFuzzyType.UNDETERMINED);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(undetermined);
        }

        // set undetermined status
        r.setFromCvFuzzyType(undetermined);
        r.setToCvFuzzyType(undetermined);

        // set positions to 0
        r.setFromIntervalStart(0);
        r.setFromIntervalEnd(0);
        r.setToIntervalStart(0);
        r.setToIntervalEnd(0);

        // update the range
        f.getRangeDao().update(r);
    }

    /**
     * For each component with range conflicts, fix the invalid or out of date ranges
     * @param protein : protein having range conflicts
     * @param context
     * @param uniprotAc : the uniprot ac
     * @param oldSequence : the previous sequence of the protein
     * @param report : the range update report
     * @param fixedProtein : the proteins the ranges can be remapped to
     * @param processor
     * @param fixOutOfDateRanges : enable or not to fix the out of date ranges
     */
    public void processInvalidRanges(Protein protein, DataContext context, String uniprotAc, String oldSequence, RangeUpdateReport report, ProteinTranscript fixedProtein, ProteinUpdateProcessor processor, boolean fixOutOfDateRanges) {
        // for each component with range conflicts
        for (Map.Entry<Component, Collection<InvalidRange>> entry : report.getInvalidComponents().entrySet()){
            for (InvalidRange invalid : entry.getValue()){
                // range is bad from the beginning, not after the range shifting : fix it.
                if (!invalid.isOutOfDate()){
                    InvalidRangeEvent invalidEvent = new InvalidRangeEvent(context, invalid, report);
                    fixInvalidRanges(invalidEvent, processor);
                }
                // range is out of date fix it if necessary and enabled
                else {
                    // try to get the sequence version of this range
                    int sequenceVersion = -1;
                    try {
                        sequenceVersion = unisave.getSequenceVersion(uniprotAc, false, oldSequence);
                    } catch (UnisaveServiceException e) {
                        log.error("The version of the sequence for the protein " + protein.getAc() + "could not be found in unisave.");
                    }

                    // create an event
                    InvalidRangeEvent invalidEvent = new InvalidRangeEvent(context, invalid, report);

                    // the sequence version has been found
                    if (sequenceVersion != -1){
                        invalid.setValidSequenceVersion(sequenceVersion);
                    }
                    invalid.setUniprotAc(uniprotAc);

                    // if the range is not undetermined yet, needs to be fixed. The old range is always the current range to update
                    if (!isInvalidRangeUndetermined(invalid.getOldRange())){

                        // if no protein exists to remap the ranges to it and the option is enabled, fix out of date ranges
                        if (fixedProtein == null && fixOutOfDateRanges){
                            fixOutOfDateRanges(invalidEvent, processor);
                        }
                        else {
                            processor.fireOnOutOfDateRange(invalidEvent);
                        }
                    }
                }
            }
        }
    }

    /**
     * Retrieve a sequence from unisave given a protein ac and a version
     * @param uniprotAc
     * @param version
     * @return
     */
    private String retrieveSequenceInUnisave(String uniprotAc, int version){
        if (uniprotAc != null && version != -1){
            String oldSequence = null;
            try {
                oldSequence = unisave.getSequenceFor(uniprotAc, false, version);

                return oldSequence;

            } catch (UnisaveServiceException e) {
                try{
                    oldSequence = unisave.getSequenceFor(uniprotAc, true, version);

                    return oldSequence;
                }catch(UnisaveServiceException e2){
                    log.error("Impossible to find the sequence (version "+version+") for the uniprot ac " + uniprotAc , e2);
                }
            }
        }

        return null;
    }

    /**
     * Recreate a range from the 'invalid-positions' at the level of a feature
     * @param featureReport
     * @param oldSequence
     * @param context
     * @return
     */
    private Range createRangeFromFeatureReport(InvalidFeatureReport featureReport, String oldSequence, DataContext context){

        if (oldSequence != null){
            Range r = null;
            try{
                r = FeatureUtils.createRangeFromString(featureReport.getRangePositions(), oldSequence);

                CvFuzzyType fromType = r.getFromCvFuzzyType();
                CvFuzzyType toType = r.getToCvFuzzyType();

                CvFuzzyType fromTypeFromDb = context.getDaoFactory().getCvObjectDao(CvFuzzyType.class).getByShortLabel(fromType.getShortLabel());
                CvFuzzyType toTypeFromDb = context.getDaoFactory().getCvObjectDao(CvFuzzyType.class).getByShortLabel(toType.getShortLabel());

                if (fromTypeFromDb == null) {
                    fromTypeFromDb = fromType;
                    context.getDaoFactory().getCvObjectDao(CvFuzzyType.class).persist(fromTypeFromDb);
                }
                if (toTypeFromDb == null) {
                    toTypeFromDb = toType;
                    context.getDaoFactory().getCvObjectDao(CvFuzzyType.class).persist(toTypeFromDb);
                }

                r.setFromCvFuzzyType(fromTypeFromDb);
                r.setToCvFuzzyType(toTypeFromDb);
            }
            catch (Exception e){
                log.error("Impossible to create a range for these positions " + featureReport.getRangePositions() + " using the sequence from unisave." , e);
            }

            return r;
        }

        return null;
    }

    /**
     * Update ranges if the protein sequence need to be updated
     * @param protein
     * @param uniprotSequence
     * @param processor
     * @param datacontext
     * @return
     */
    public RangeUpdateReport updateRanges(Protein protein, String uniprotSequence, ProteinUpdateProcessor processor, DataContext datacontext){

        // create a range update report
        RangeUpdateReport report = new RangeUpdateReport();

        // oldsequence = protein sequence
        String oldSequence = protein.getSequence();
        // new sequence = uniprot sequence
        String sequence = uniprotSequence;

        // get the components of a protein
        Collection<Component> components = protein.getActiveInstances();

        for (Component component : components){

            // get the features
            Collection<Feature> features = component.getFeatures();
            // the list of invalid ranges attached to this feature
            Collection<InvalidRange> totalInvalidRanges = new ArrayList<InvalidRange>();

            for (Feature feature : features){
                // collect previous reports about previous range conflicts
                Map<String, InvalidFeatureReport> featureReports = checkConsistencyFeatureBeforeRangeShifting(feature, datacontext, processor, report);

                // shift ranges without conflicts first
                for (Range r : feature.getRanges()){
                    // the range never had conflicts before
                    if(!featureReports.containsKey(r.getAc())) {
                        // collect a non null invalid range if the range cannot be shifted, null if it can be shifted successfully
                        InvalidRange invalid = checker.collectRangeImpossibleToShift(r,oldSequence, sequence );

                        // the range cannot be shifted, add it to the list of bad ranges
                        if (invalid != null){
                            totalInvalidRanges.add(invalid);
                        }
                        // the range can be shifted, shift it
                        else {
                            shiftRange(oldSequence, uniprotSequence, r, datacontext, report);
                        }
                    }
                }

                // we have ranges with previous conflicts to remap
                if (!featureReports.isEmpty()){

                    // try to shift ranges with previous range conflicts
                    for (Map.Entry<String, InvalidFeatureReport> entry : featureReports.entrySet()){
                        // get the sequence from unisave if possible
                        String oldSequenceFromUnisave = retrieveSequenceInUnisave(entry.getValue().getUniprotAc(), entry.getValue().getSequenceVersion());

                        // create the range as it was when it was valid if possible
                        Range createdFromUnisave = createRangeFromFeatureReport(entry.getValue(), oldSequenceFromUnisave, datacontext);

                        // the ranges could be recreated
                        if (createdFromUnisave != null){
                            for (Range r : feature.getRanges()){
                                // the range is the range we try to remap
                                if (entry.getKey().equalsIgnoreCase(r.getAc())){

                                    // reset previous positions and status
                                    r.setFromCvFuzzyType(createdFromUnisave.getFromCvFuzzyType());
                                    r.setToCvFuzzyType(createdFromUnisave.getToCvFuzzyType());
                                    r.setFromIntervalStart(createdFromUnisave.getFromIntervalStart());
                                    r.setFromIntervalEnd(createdFromUnisave.getFromIntervalEnd());
                                    r.setToIntervalStart(createdFromUnisave.getToIntervalStart());
                                    r.setToIntervalEnd(createdFromUnisave.getToIntervalEnd());

                                    // extract previous feature sequence
                                    r.prepareSequence(oldSequenceFromUnisave, false);
                                    // update the range
                                    datacontext.getDaoFactory().getRangeDao().update(r);

                                    // see if it is possible to remap this range from the sequence in unisave to the sequence in uniprot
                                    InvalidRange invalid = checker.collectRangeImpossibleToShift(r,oldSequenceFromUnisave, sequence );

                                    // not possible, fix the range
                                    if (invalid != null){
                                        invalid.setValidSequenceVersion(entry.getValue().getSequenceVersion());
                                        invalid.setUniprotAc(entry.getValue().getUniprotAc());
                                        InvalidRangeEvent invalidEvent = new InvalidRangeEvent(datacontext, invalid, report);

                                        fixOutOfDateRanges(invalidEvent, processor);
                                        totalInvalidRanges.add(invalid);
                                    }
                                    // possible update it
                                    else {
                                        shiftRange(oldSequenceFromUnisave, uniprotSequence, r, datacontext, report);
                                    }
                                }
                            }
                        }
                    }

                    // clean annotations features if necessary
                    checkConsistencyFeatureAfterRangeShifting(feature, datacontext, processor, report);
                }

                if (!totalInvalidRanges.isEmpty()){
                    report.getInvalidComponents().put(component, totalInvalidRanges);
                }
            }
        }

        return report;
    }

    /**
     * Update invalid ranges
     * @param protein
     * @param processor
     * @param datacontext
     * @return
     */
    public RangeUpdateReport updateOnlyInvalidRanges(Protein protein, ProteinUpdateProcessor processor, DataContext datacontext){

        // create a range update report
        RangeUpdateReport report = new RangeUpdateReport();

        // oldsequence = protein sequence
        String oldSequence = protein.getSequence();

        // get the components of a protein
        Collection<Component> components = protein.getActiveInstances();

        for (Component component : components){

            // get the features
            Collection<Feature> features = component.getBindingDomains();
            // the list of invalid ranges attached to this feature
            Collection<InvalidRange> totalInvalidRanges = new ArrayList<InvalidRange>();

            for (Feature feature : features){
                // collect previous reports about previous range conflicts
                Map<String, InvalidFeatureReport> featureReports = checkConsistencyFeatureBeforeRangeShifting(feature, datacontext, processor, report);

                // shift ranges without conflicts first
                for (Range r : feature.getRanges()){
                    // collect a non null invalid range if the range cannot be shifted, null if it can be shifted successfully
                    InvalidRange invalid = checker.collectRangeInvalidWithCurrentSequence(r, oldSequence);

                    // the range cannot be shifted, add it to the list of bad ranges
                    if (invalid != null){
                        totalInvalidRanges.add(invalid);
                    }
                }

                if (!totalInvalidRanges.isEmpty()){
                    report.getInvalidComponents().put(component, totalInvalidRanges);
                }
            }
        }

        return report;
    }

    public RangeChecker getChecker() {
        return checker;
    }

    public void setChecker(RangeChecker checker) {
        this.checker = checker;
    }
}
