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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;

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

    protected UnisaveService unisave;

    public RangeFixer(){
        this.checker = new RangeChecker();
        this.unisave = new UnisaveService();
    }

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

    public void fixInvalidRanges(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String message = "["+range.getAc()+"]" +evt.getInvalidRange().getMessage();
        String positions = convertPositionsToString(evt.getInvalidRange().getInvalidRange());

        if (range != null){
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
                boolean hasInvalid = false;
                boolean hasInvalidPos = false;

                for (Annotation a : annotations){
                    if (invalid_caution.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(message, a)){
                            hasInvalid = true;
                        }
                    }
                    else if (invalidPositions.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(positions, a)){
                            hasInvalidPos = true;
                        }
                    }
                }

                if (!hasInvalid){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }

                if (!hasInvalidPos){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                }

                setRangeUndetermined(range, daoFactory);

                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

    public void fixOutOfDateRanges(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String message = "["+range.getAc()+"]" +evt.getInvalidRange().getMessage();
        String positions = convertPositionsToString(evt.getInvalidRange().getInvalidRange());
        int validSequenceVersion = evt.getInvalidRange().getValidSequenceVersion();

        String validSequence = null;

        if (range != null){
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
                boolean hasInvalid = false;
                boolean hasInvalidPos = false;
                boolean hasSequenceVersion = false;

                for (Annotation a : annotations){
                    if (invalid_caution.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(message, a)){
                            hasInvalid = true;
                        }
                    }
                    else if (invalidPositions.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(positions, a)){
                            hasInvalidPos = true;
                        }
                    }
                    else if (sequenceVersion.equals(a.getCvTopic())){
                        if (validSequenceVersion != -1){
                            validSequence = "["+range.getAc()+"]"+validSequenceVersion;

                            if (hasAnnotationMessage(validSequence, a)){
                                hasSequenceVersion = true;
                            }
                        }
                    }
                }

                if (!hasInvalid){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }

                if (!hasInvalidPos){
                    Annotation invalidPosRange = new Annotation(invalidPositions, positions);
                    daoFactory.getAnnotationDao().persist(invalidPosRange);

                    feature.addAnnotation(invalidPosRange);
                }

                if (!hasSequenceVersion && validSequence != null){
                    Annotation validSeqVersion = new Annotation(sequenceVersion, validSequence);
                    daoFactory.getAnnotationDao().persist(validSeqVersion);

                    feature.addAnnotation(validSeqVersion);
                }

                setRangeUndetermined(range, daoFactory);

                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

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

    protected boolean hasAnnotationMessage(String message, Annotation a) {

        if (message != null){
            if (message.equals(a.getAnnotationText())){
                if (log.isDebugEnabled()) {
                    log.debug("Feature object already contains this annotation. Not adding another one: "+a);
                }
                return true;
            }
        }
        else if (message == null && a.getAnnotationText() == null){
            if (log.isDebugEnabled()) {
                log.debug("Feature object already contains this annotation. Not adding another one: "+a);
            }
            return true;
        }

        return false;
    }

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

                    processor.fireOnOutOfDateRange(invalidEvent);
                    if (fixedProtein == null && fixOutOfDateRanges){
                        fixOutOfDateRanges(invalidEvent);
                    }
                }
            }
        }
    }

    public RangeUpdateReport updateRanges(Protein protein, String uniprotSequence, ProteinUpdateProcessor processor, DataContext datacontext){

        RangeUpdateReport report = new RangeUpdateReport();

        String oldSequence = protein.getSequence();
        String sequence = uniprotSequence;

        Collection<Component> components = protein.getActiveInstances();

        for (Component component : components){

            Collection<Feature> features = component.getBindingDomains();
            Collection<InvalidRange> totalInvalidRanges = new ArrayList<InvalidRange>();

            for (Feature feature : features){
                Collection<InvalidRange> invalidRanges = checker.collectRangesImpossibleToShift(feature, oldSequence, sequence);
                totalInvalidRanges.addAll(invalidRanges);

                if (!invalidRanges.isEmpty()){
                    report.getInvalidComponents().put(component, totalInvalidRanges);
                }
            }
        }

        if (!report.getInvalidComponents().isEmpty()){
            Collection<Component> componentsToFix = report.getInvalidComponents().keySet();

            Collection<Component> componentsToUpdate = CollectionUtils.subtract(components, componentsToFix);

            if (!componentsToUpdate.isEmpty()){
                shiftRanges(oldSequence, uniprotSequence, componentsToUpdate, processor, datacontext);
            }
        }
        else {
            shiftRanges(oldSequence, uniprotSequence, components, processor, datacontext);
        }

        return report;
    }
}
