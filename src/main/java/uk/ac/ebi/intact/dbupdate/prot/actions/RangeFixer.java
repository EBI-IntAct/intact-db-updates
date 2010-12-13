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
package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.bridges.unisave.UnisaveService;
import uk.ac.ebi.intact.bridges.unisave.UnisaveServiceException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.FeatureDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidFeatureReport;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.FeatureUtils;

import java.util.*;

/**
 * Listens for sequence changes and updates the ranges of the features.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeFixer {

    private static final Log log = LogFactory.getLog( RangeFixer.class );
    protected RangeChecker checker;
    public static final String invalidPositions = "invalid-positions";
    public static final String rangeSeparator = "..";
    public static final String positionsSeparator = "-";
    public static final String rangeConflicts = "range-conflicts";
    public static final String sequenceVersion = "sequence-version";
    public static final String sequenceVersionSeparator = ",";

    protected UnisaveService unisave;

    public RangeFixer(){
        this.checker = new RangeChecker();
        this.unisave = new UnisaveService();
    }

    private boolean isInvalidRangeUndetermined(Range range){
        if (range == null){
            return true;
        }

        if (range.getFromCvFuzzyType() == null){
            return true;
        }
        else if(range.getToCvFuzzyType() == null){
            return true;
        }
        else if(range.getFromCvFuzzyType().isUndetermined() && range.getToCvFuzzyType().isUndetermined()){
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
    protected Map<String, InvalidFeatureReport> checkConsistencyFeatureBeforeRangeShifting(Feature feature, DataContext context, ProteinUpdateProcessor processor){
        DaoFactory factory = context.getDaoFactory();

        // the annotations of a feature
        feature.getAnnotations().size();

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

        AnnotationDao annDao = factory.getAnnotationDao();
        FeatureDao featureDao = factory.getFeatureDao();

        // update or remove the feature annotations and collect invalid ranges annotations
        for (Annotation annotation : annotationsFeature){
            if (CvTopic.INVALID_RANGE.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (!existingRanges.containsKey(rangeAc)){
                    ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                }
                else{
                    if (existingRanges.get(rangeAc)){
                        invalidRanges.add(rangeAc);
                    }
                    else{
                        ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                    }
                }
            }
            else if (rangeConflicts.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (!existingRanges.containsKey(rangeAc)){
                    ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                }
                else {
                    if (!existingRanges.get(rangeAc)){
                        ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                    }
                    else if(!featureReports.containsKey(rangeAc)){
                        InvalidFeatureReport report = new InvalidFeatureReport();
                        report.setRangeAc(rangeAc);
                        featureReports.put(rangeAc, report);
                    }
                }
            }
            else if (invalidPositions.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (!existingRanges.containsKey(rangeAc)){
                    ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                }
                else {
                    if (!existingRanges.get(rangeAc)){
                        ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                    }
                    else if(!invalidRanges.contains(rangeAc)){
                        if (featureReports.containsKey(rangeAc)){
                            InvalidFeatureReport report = featureReports.get(rangeAc);
                            report.setRangePositions(annotation);
                        }
                        else{
                            InvalidFeatureReport report = new InvalidFeatureReport();
                            report.setRangeAc(rangeAc);
                            report.setRangePositions(annotation);
                            featureReports.put(rangeAc, report);
                        }
                    }
                }
            }
            else if (sequenceVersion.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (!existingRanges.containsKey(rangeAc)){
                    ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                }
                else{
                    if (!existingRanges.get(rangeAc)){
                        ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                    }
                    else if (featureReports.containsKey(rangeAc)){
                        InvalidFeatureReport report = featureReports.get(rangeAc);
                        report.setSequenceVersion(annotation);
                    }
                    else{
                        InvalidFeatureReport report = new InvalidFeatureReport();
                        report.setRangeAc(rangeAc);
                        report.setSequenceVersion(annotation);
                        featureReports.put(rangeAc, report);
                    }
                }
            }
            else if (CvTopic.CAUTION_MI_REF.equals(annotation.getCvTopic().getIdentifier())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (!existingRanges.containsKey(rangeAc)){
                    ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                }                
            }
        }

        featureDao.update(feature);

        return featureReports;
    }

    /**
     * After having shifted the ranges, it is possible that ranges with previous range conflicts have been fixed and we need to
     * remove annotations concerning this ranges at the level of the feature
     * @param feature
     * @param context
     */
    protected void checkConsistencyFeatureAfterRangeShifting(Feature feature, DataContext context, ProteinUpdateProcessor processor){
        DaoFactory factory = context.getDaoFactory();

        Collection<Annotation> annotationsFeature = new ArrayList<Annotation>();
        annotationsFeature.addAll(feature.getAnnotations());

        Map<String, Boolean> existingInvalidRanges = new HashMap<String, Boolean>();

        for (Range r : feature.getRanges()){
            if (r.getAc() != null){
                if (isInvalidRangeUndetermined(r)){
                    existingInvalidRanges.put(r.getAc(), true);
                }
                else {
                    existingInvalidRanges.put(r.getAc(), false);
                }
            }
        }

        AnnotationDao annDao = factory.getAnnotationDao();
        FeatureDao featureDao = factory.getFeatureDao();

        for (Annotation annotation : annotationsFeature){
            if (CvTopic.INVALID_RANGE.equalsIgnoreCase(annotation.getCvTopic().getShortLabel()) ||
                    rangeConflicts.equalsIgnoreCase(annotation.getCvTopic().getShortLabel()) ||
                    invalidPositions.equalsIgnoreCase(annotation.getCvTopic().getShortLabel()) ||
                    sequenceVersion.equalsIgnoreCase(annotation.getCvTopic().getShortLabel())){
                String rangeAc = InvalidFeatureReport.extractRangeAcFromAnnotation(annotation);

                if (existingInvalidRanges.containsKey(rangeAc)){
                    if (!existingInvalidRanges.get(rangeAc)){
                        ProteinTools.deleteAnnotation(feature, context, annotation, processor);
                    }
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
    protected void shiftRanges(String oldSequence, String newSequence, Collection<Component> componentsToUpdate, ProteinUpdateProcessor processor, DataContext context) throws ProcessorException {

        if (oldSequence != null && newSequence != null) {

            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

                    Collection<UpdatedRange> updatedRanges = checker.shiftFeatureRanges(feature, oldSequence, newSequence, context);

                    // fire the events for the range changes
                    for (UpdatedRange updatedRange : updatedRanges) {
                        processor.fireOnRangeChange(new RangeChangedEvent(context, updatedRange));
                    }
                }
            }
        }
        else if (oldSequence == null && newSequence != null){
            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

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
     * @param processor
     * @param context
     * @throws ProcessorException
     */
    protected void shiftRange(String oldSequence, String newSequence, Range range, ProteinUpdateProcessor processor, DataContext context) throws ProcessorException {

        if (oldSequence != null && newSequence != null) {

            UpdatedRange updatedRange = checker.shiftFeatureRange(range, oldSequence, newSequence, context);

            // fire the events for the range changes
            if (updatedRange != null) {
                processor.fireOnRangeChange(new RangeChangedEvent(context, updatedRange));
            }
        }
        else if (oldSequence == null && newSequence != null){
            UpdatedRange updatedRange = checker.prepareFeatureSequence(range, newSequence, context);

            // fire the events for the range changes
            if (updatedRange != null) {
                processor.fireOnRangeChange(new RangeChangedEvent(context, updatedRange));
            }
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
    public void fixInvalidRanges(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String positions = convertPositionsToString(evt.getInvalidRange().getInvalidRange());

        if (range != null){
            String prefix = "["+range.getAc()+"]";
            String message = prefix +evt.getInvalidRange().getMessage();

            Feature feature = range.getFeature();

            if (feature != null){
                // get the invalid_caution from the DB or create it and persist it
                final DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
                CvTopic invalid_caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.INVALID_RANGE);

                if (invalid_caution == null) {
                    invalid_caution = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, CvTopic.INVALID_RANGE);
                    daoFactory.getCvObjectDao(CvTopic.class).persist(invalid_caution);
                }

                CvTopic invalidPositions = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixer.invalidPositions);

                if (invalidPositions == null) {
                    invalidPositions = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixer.invalidPositions);
                    daoFactory.getCvObjectDao(CvTopic.class).persist(invalidPositions);
                }

                Collection<Annotation> annotations = feature.getAnnotations();
                Annotation invalid = null;
                Annotation pos = null;

                for (Annotation a : annotations){
                    if (invalid_caution.equals(a.getCvTopic())){
                        if (a.getAnnotationText().startsWith(prefix)){
                            invalid = a;
                        }
                    }
                    else if (invalidPositions.equals(a.getCvTopic())){
                        if (a.getAnnotationText().startsWith(prefix)){
                            pos = a;
                        }
                    }
                }

                if (invalid == null){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }
                else{
                    invalid.setAnnotationText(message);
                    daoFactory.getAnnotationDao().update(invalid);
                }

                if (pos == null){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                }
                else{
                    pos.setAnnotationText(positions);
                    daoFactory.getAnnotationDao().update(pos);
                }

                setRangeUndetermined(range, daoFactory);

                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

    /**
     * Fix out of date ranges by adding annotations at the feature level and set the range to undetermined
     * @param evt
     */
    public void fixOutOfDateRanges(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String positions = convertPositionsToString(evt.getInvalidRange().getInvalidRange());
        int validSequenceVersion = evt.getInvalidRange().getValidSequenceVersion();

        if (range != null){
            String prefix = "["+range.getAc()+"]";

            String message = prefix +evt.getInvalidRange().getMessage();
            String validSequence = prefix+evt.getInvalidRange().getUniprotAc()+","+validSequenceVersion;

            Feature feature = range.getFeature();

            if (feature != null){
                // get the invalid_caution from the DB or create it and persist it
                final DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
                CvTopic invalid_caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixer.rangeConflicts);

                if (invalid_caution == null) {
                    invalid_caution = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixer.rangeConflicts);
                    daoFactory.getCvObjectDao(CvTopic.class).persist(invalid_caution);
                }

                CvTopic invalidPositions = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixer.invalidPositions);

                if (invalidPositions == null) {
                    invalidPositions = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixer.invalidPositions);
                    daoFactory.getCvObjectDao(CvTopic.class).persist(invalidPositions);
                }

                CvTopic sequenceVersion = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(RangeFixer.sequenceVersion);

                if (sequenceVersion == null) {
                    sequenceVersion = CvObjectUtils.createCvObject(range.getOwner(), CvTopic.class, null, RangeFixer.sequenceVersion);
                    daoFactory.getCvObjectDao(CvTopic.class).persist(sequenceVersion);
                }

                Collection<Annotation> annotations = feature.getAnnotations();
                Annotation invalid = null;
                Annotation pos = null;
                Annotation sv = null;

                for (Annotation a : annotations){
                    if (invalid_caution.equals(a.getCvTopic())){
                        if (a.getAnnotationText().startsWith(prefix)){
                            invalid = a;
                        }
                    }
                    else if (invalidPositions.equals(a.getCvTopic())){
                        if (a.getAnnotationText().startsWith(prefix)){
                            pos = a;
                        }
                    }
                    else if (sequenceVersion.equals(a.getCvTopic())){

                        if (a.getAnnotationText().startsWith(prefix)){
                            sv = a;
                        }
                    }
                }

                if (invalid == null){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }
                else {
                    invalid.setAnnotationText(message);
                    daoFactory.getAnnotationDao().update(invalid);
                }

                if (pos == null){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                }
                else {
                    pos.setAnnotationText(positions);
                    daoFactory.getAnnotationDao().update(pos);
                }

                if (pos == null && validSequenceVersion != -1 && evt.getInvalidRange().getUniprotAc() != null){
                    Annotation validSeqVersion = new Annotation(sequenceVersion, validSequence);
                    daoFactory.getAnnotationDao().persist(validSeqVersion);

                    feature.addAnnotation(validSeqVersion);
                }
                else if (pos != null && validSequenceVersion != -1 && evt.getInvalidRange().getUniprotAc() != null) {
                    sv.setAnnotationText(validSequence);
                    daoFactory.getAnnotationDao().update(sv);
                }

                setRangeUndetermined(range, daoFactory);

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
        CvFuzzyType undetermined = f.getCvObjectDao(CvFuzzyType.class).getByPsiMiRef(CvFuzzyType.UNDETERMINED_MI_REF);

        if (undetermined == null) {
            undetermined = CvObjectUtils.createCvObject(r.getOwner(), CvFuzzyType.class, CvFuzzyType.UNDETERMINED_MI_REF, CvFuzzyType.UNDETERMINED);
            f.getCvObjectDao(CvFuzzyType.class).persist(undetermined);
        }

        r.setFromCvFuzzyType(undetermined);
        r.setToCvFuzzyType(undetermined);
        r.setFromIntervalStart(0);
        r.setFromIntervalEnd(0);
        r.setToIntervalStart(0);
        r.setToIntervalEnd(0);

        f.getRangeDao().update(r);
    }

    /**
     * For each component with range conflicts, fix the invalid or out of date ranges
     * @param protein
     * @param evt
     * @param uniprotAc
     * @param oldSequence
     * @param report
     * @param fixedProtein
     * @param processor
     * @param fixOutOfDateRanges
     */
    public void processInvalidRanges(Protein protein, UpdateCaseEvent evt, String uniprotAc, String oldSequence, RangeUpdateReport report, ProteinTranscript fixedProtein, ProteinUpdateProcessor processor, boolean fixOutOfDateRanges) {
        for (Map.Entry<Component, Collection<InvalidRange>> entry : report.getInvalidComponents().entrySet()){
            for (InvalidRange invalid : entry.getValue()){
                // range is bad from the beginning, not after the range shifting
                if (!ProteinTools.isSequenceChanged(oldSequence, invalid.getSequence())){
                    InvalidRangeEvent invalidEvent = new InvalidRangeEvent(evt.getDataContext(), invalid);
                    processor.fireOnInvalidRange(invalidEvent);
                    fixInvalidRanges(invalidEvent);
                }
                else {
                    int sequenceVersion = -1;
                    try {
                        sequenceVersion = unisave.getSequenceVersion(uniprotAc, false, protein.getSequence());
                    } catch (UnisaveServiceException e) {
                        log.error("The version of the sequence for the protein " + protein.getAc() + "could not be found in unisave.");
                    }

                    InvalidRangeEvent invalidEvent = new InvalidRangeEvent(evt.getDataContext(), invalid);

                    if (sequenceVersion != -1){
                        invalid.setValidSequenceVersion(sequenceVersion);
                    }
                    invalid.setUniprotAc(uniprotAc);

                    if (!isInvalidRangeUndetermined(invalid.getInvalidRange())){
                        processor.fireOnOutOfDateRange(invalidEvent);
                        if (fixedProtein == null && fixOutOfDateRanges){
                            fixOutOfDateRanges(invalidEvent);
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

        RangeUpdateReport report = new RangeUpdateReport();

        String oldSequence = protein.getSequence();
        String sequence = uniprotSequence;

        Collection<Component> components = protein.getActiveInstances();

        for (Component component : components){

            Collection<Feature> features = component.getBindingDomains();
            Collection<InvalidRange> totalInvalidRanges = new ArrayList<InvalidRange>();

            for (Feature feature : features){
                Map<String, InvalidFeatureReport> featureReports = checkConsistencyFeatureBeforeRangeShifting(feature, datacontext, processor);

                // shift ranges without conflicts first
                for (Range r : feature.getRanges()){
                    if(!featureReports.containsKey(r.getAc())) {
                        InvalidRange invalid = checker.collectRangeImpossibleToShift(r,oldSequence, sequence );

                        if (invalid != null){
                            totalInvalidRanges.add(invalid);
                        }
                        else {
                            shiftRange(oldSequence, uniprotSequence, r, processor, datacontext);
                        }
                    }
                }

                if (!featureReports.isEmpty()){

                    // try to shift ranges with previous range conflicts
                    for (Map.Entry<String, InvalidFeatureReport> entry : featureReports.entrySet()){
                        String oldSequenceFromUnisave = retrieveSequenceInUnisave(entry.getValue().getUniprotAc(), entry.getValue().getSequenceVersion());

                        Range createdFromUnisave = createRangeFromFeatureReport(entry.getValue(), oldSequenceFromUnisave, datacontext);

                        if (createdFromUnisave != null){
                            for (Range r : feature.getRanges()){
                                if (entry.getKey().equalsIgnoreCase(r.getAc())){

                                    r.setFromCvFuzzyType(createdFromUnisave.getFromCvFuzzyType());
                                    r.setToCvFuzzyType(createdFromUnisave.getToCvFuzzyType());
                                    r.setFromIntervalStart(createdFromUnisave.getFromIntervalStart());
                                    r.setFromIntervalEnd(createdFromUnisave.getFromIntervalEnd());
                                    r.setToIntervalStart(createdFromUnisave.getToIntervalStart());
                                    r.setToIntervalEnd(createdFromUnisave.getToIntervalEnd());

                                    r.prepareSequence(oldSequenceFromUnisave, false);

                                    datacontext.getDaoFactory().getRangeDao().update(r);

                                    InvalidRange invalid = checker.collectRangeImpossibleToShift(r,oldSequenceFromUnisave, sequence );

                                    if (invalid != null){
                                        InvalidRangeEvent invalidEvent = new InvalidRangeEvent(datacontext, invalid);
                                        processor.fireOnOutOfDateRange(invalidEvent);
                                        fixInvalidRanges(invalidEvent);
                                        totalInvalidRanges.add(invalid);
                                    }
                                    else {
                                        shiftRange(oldSequenceFromUnisave, uniprotSequence, r, processor, datacontext);
                                    }
                                }
                            }
                        }
                    }

                    checkConsistencyFeatureAfterRangeShifting(feature, datacontext, processor);
                }

                if (!totalInvalidRanges.isEmpty()){
                    report.getInvalidComponents().put(component, totalInvalidRanges);
                }
            }
        }

        return report;
    }
}
