package uk.ac.ebi.intact.tools.feature.shortlabel.generator;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Annotation;
import psidev.psi.mi.jami.model.Range;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.ExperimentalRange;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.exception.FeatureShortlabelGenerationException;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.utils.OntologyServiceHelper;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.utils.ShortlabelGeneratorHelper;

import java.util.HashSet;
import java.util.Iterator;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGenerator {
    private final static String MUTATION_MI_ID = "MI:0118";
    private final static String CV_TERM_REMARK_INTERNAL_AC = "EBI-20";
    private final static String CV_TERM_NO_MUTATION_UP = "EBI-11795051";

    private final static int OLS_SEARCHING_DEPTH = 10;
    private final static int TRIES = 3;

    private static Set<String> allowedFeatureTypes = new HashSet<String>();

    private ShortlabelGeneratorManager manager = new ShortlabelGeneratorManager();
    private ShortlabelGeneratorHelper helper = new ShortlabelGeneratorHelper();

    private IntactDao intactDao;

    public ShortlabelGenerator() {
        initAllowedFeatureTypes();
    }

    private void initAllowedFeatureTypes() {
        allowedFeatureTypes.addAll(OntologyServiceHelper.getOntologyServiceHelper().getAssociatedMITerms(MUTATION_MI_ID, OLS_SEARCHING_DEPTH));
    }

    public void subscribeToEvents(ShortlabelGeneratorObserver shortlabelGeneratorObserver) {
        manager.setShortlabelGeneratorObserver(shortlabelGeneratorObserver);
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public IntactFeatureEvidence getFeatureEvidence(String ac, int tries) {

        IntactFeatureEvidence featureEvidence = intactDao.getFeatureEvidenceDao().getByAc(ac);
        if (featureEvidence == null && tries > 0) {
            tries--;
            featureEvidence = getFeatureEvidence(ac, tries);
        } else if (featureEvidence == null && tries == 0) {
            ObjRetrieveErrorEvent event = new ObjRetrieveErrorEvent(ac, null,
                    ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_FEATURE);
            manager.fireOnRetrieveObjErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }
        return featureEvidence;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public void generateNewShortLabel(String ac) {
        IntactFeatureEvidence featureEvidence = getFeatureEvidence(ac, TRIES);
        generateNewShortLabel(featureEvidence);
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public void generateNewShortLabel(IntactFeatureEvidence featureEvidence) {
        String orgShortlabel = featureEvidence.getShortName();
        String featureAc = featureEvidence.getAc();
        String interactorAc;
        String interactorSeq;
        String interactorType;
        Iterator<Range> rangeIterator;

        IntactInteractor interactor = helper.getInteractorByFeatureEvidence(featureEvidence);

        if (interactor == null) {
            ObjRetrieveErrorEvent event = new ObjRetrieveErrorEvent(featureAc, null, ObjRetrieveErrorEvent.ErrorType.UNABLE_RETRIEVE_INTERACTOR);
            manager.fireOnRetrieveObjErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }

        interactorAc = interactor.getAc();
        interactorType = interactor.getInteractorType().getShortName();

        if (!interactorType.equals("protein") && !interactorType.equals("peptide")) {
            TypeErrorEvent event = new TypeErrorEvent(featureAc, interactorAc, TypeErrorEvent.ObjTypeErrorType.WRONG_INTERACTOR_TYPE);
            manager.fireOnTypeErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }

        interactorSeq = helper.getInteractorSeqByInteractor(interactor);

        if (interactorSeq == null) {
            ObjRetrieveErrorEvent event = new ObjRetrieveErrorEvent(featureAc, interactorAc, ObjRetrieveErrorEvent.ErrorType.UNABLE_RETRIEVE_INTERACTOR_SEQUENCE);
            manager.fireOnRetrieveObjErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }

        if (!allowedFeatureTypes.contains(featureEvidence.getType().getMIIdentifier())) {
            TypeErrorEvent event = new TypeErrorEvent(featureAc, interactorAc, TypeErrorEvent.ObjTypeErrorType.WRONG_FEATURE_TYPE);
            manager.fireOnTypeErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }

        for (Annotation annotation : featureEvidence.getAnnotations()) {
            if(annotation.getTopic() == getIntActCVTermNoMutationUpdate(3)){
                FeatureAnnotationFoundEvent event = new FeatureAnnotationFoundEvent(featureAc, interactorAc, FeatureAnnotationFoundEvent.AnnotationType.NO_MUTATION_UPDATE);
                manager.fireOnFeatureAnnotationFoundEvent(event);
                throw new FeatureShortlabelGenerationException();
            }

            if (annotation.getTopic() == getIntActCVTermRemarkInternal(3)) {
                if (annotation.getValue().startsWith(FeatureAnnotationFoundEvent.AnnotationType.FEATURE_CORRECTED.getMessage())) {
                    FeatureAnnotationFoundEvent event = new FeatureAnnotationFoundEvent(featureAc, interactorAc, FeatureAnnotationFoundEvent.AnnotationType.FEATURE_CORRECTED);
                    manager.fireOnFeatureAnnotationFoundEvent(event);
                } else if (annotation.getValue().startsWith(FeatureAnnotationFoundEvent.AnnotationType.FEATURE_WRONG.getMessage())) {
                    FeatureAnnotationFoundEvent event = new FeatureAnnotationFoundEvent(featureAc, interactorAc, FeatureAnnotationFoundEvent.AnnotationType.FEATURE_WRONG);
                    manager.fireOnFeatureAnnotationFoundEvent(event);
                    throw new FeatureShortlabelGenerationException();
                }
            }
        }

        featureEvidence.setShortName("");
        rangeIterator = featureEvidence.getRanges().iterator();
        while (rangeIterator.hasNext()) {
            ExperimentalRange range = (ExperimentalRange) rangeIterator.next();
            String newShortlabel = "";
            long rangeStart;
            long rangeEnd;
            String orgSeq;
            String resSeq;
            String calculatedOrgSeq;
            if (range == null) {
                RangeErrorEvent event = new RangeErrorEvent(featureAc, interactorAc, null, RangeErrorEvent.ErrorType.RANGE_NULL);
                manager.fireOnRangeErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }

            String rangeAc = range.getAc();

            if (range.getStart().getStart() == 0) {
                RangeErrorEvent event = new RangeErrorEvent(featureAc, interactorAc, rangeAc, RangeErrorEvent.ErrorType.START_POS_ZERO);
                manager.fireOnRangeErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }
            if (range.getStart().isPositionUndetermined()) {
                RangeErrorEvent event = new RangeErrorEvent(featureAc, interactorAc, rangeAc, RangeErrorEvent.ErrorType.START_POS_UNDETERMINED);
                manager.fireOnRangeErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }
            if (range.getResultingSequence().getOriginalSequence() == null) {
                RangeErrorEvent event = new RangeErrorEvent(featureAc, interactorAc, rangeAc, RangeErrorEvent.ErrorType.ORG_SEQ_NULL);
                manager.fireOnRangeErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }
            if (range.getResultingSequence().getNewSequence() == null) {
                RangeErrorEvent event = new RangeErrorEvent(featureAc, interactorAc, rangeAc, RangeErrorEvent.ErrorType.RES_SEQ_NULL);
                manager.fireOnRangeErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }

            rangeStart = range.getStart().getStart();
            rangeEnd = range.getEnd().getEnd();
            orgSeq = range.getResultingSequence().getOriginalSequence();
            resSeq = range.getResultingSequence().getNewSequence();
            calculatedOrgSeq = helper.generateOrgSeq(interactorSeq, rangeStart, rangeEnd);

            if (calculatedOrgSeq == null) {
                SequenceErrorEvent event = new SequenceErrorEvent(featureAc, interactorAc, rangeAc, SequenceErrorEvent.ErrorType.UNABLE_CALCULATE_ORG_SEQ);
                manager.fireOnSeqErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }
            if (helper.orgSeqWrong(orgSeq, calculatedOrgSeq)) {
                String message = "Original sequence does not match interactor sequence. Is " + orgSeq + " should be " + calculatedOrgSeq + " Range: (" + rangeStart + "-" + rangeEnd + ")";
                SequenceErrorEvent event = new SequenceErrorEvent(featureAc, interactorAc, rangeAc, SequenceErrorEvent.ErrorType.ORG_SEQ_WRONG, message);
                manager.fireOnSeqErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }
            if (helper.containsLowerCaseLetters(resSeq)) {
                SequenceErrorEvent event = new SequenceErrorEvent(featureAc, interactorAc, rangeAc, SequenceErrorEvent.ErrorType.RES_SEQ_CONTAINS_LOWER_CASE);
                manager.fireOnSeqErrorEvent(event);
                throw new FeatureShortlabelGenerationException();
            }

            newShortlabel += helper.seq2ThreeLetterCodeOnDefault(orgSeq);

            if (helper.isSingleAminoAcidChange(rangeStart, rangeEnd)) {
                newShortlabel += helper.generateNonSequentialRange(rangeStart);
            } else {
                newShortlabel += helper.generateSequentialRange(rangeStart, rangeEnd);
            }

            if (helper.resultingSeqDescreased(orgSeq, resSeq)) {
                if (helper.isDeletion(orgSeq, resSeq)) {
                    ResultingSequenceChangedEvent event = new ResultingSequenceChangedEvent(featureAc, interactorAc, rangeAc, ResultingSequenceChangedEvent.ChangeType.DELETION);
                    manager.fireOnResSeqChangedEvent(event);
                    newShortlabel += helper.newSequence2ThreeLetterCodeOnDelete(range.getResultingSequence().getOriginalSequence());
                } else {
                    ResultingSequenceChangedEvent event = new ResultingSequenceChangedEvent(featureAc, interactorAc, rangeAc, ResultingSequenceChangedEvent.ChangeType.DECREASE);
                    manager.fireOnResSeqChangedEvent(event);
                    newShortlabel += helper.seq2ThreeLetterCodeOnDefault(range.getResultingSequence().getNewSequence());
                }
            } else if (helper.resultingSeqIncreased(orgSeq, resSeq)) {
                ResultingSequenceChangedEvent event = new ResultingSequenceChangedEvent(featureAc, interactorAc, rangeAc, ResultingSequenceChangedEvent.ChangeType.INCREASE);
                manager.fireOnResSeqChangedEvent(event);
                newShortlabel += helper.seq2ThreeLetterCodeOnDefault(range.getResultingSequence().getNewSequence());
            } else {
                ResultingSequenceChangedEvent event = new ResultingSequenceChangedEvent(featureAc,
                        interactorAc, rangeAc, ResultingSequenceChangedEvent.ChangeType.STABLE);
                manager.fireOnResSeqChangedEvent(event);
                newShortlabel += helper.seq2ThreeLetterCodeOnDefault(range.getResultingSequence().getNewSequence());
            }

            featureEvidence.setShortName(featureEvidence.getShortName() + newShortlabel + (rangeIterator.hasNext() ? "," : ""));
        }
        SuccessfulGeneratedEvent event = new SuccessfulGeneratedEvent(featureAc, interactorAc, featureEvidence, orgShortlabel);
        manager.fireOnSuccessfulGeneratedEvent(event);
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    private IntactCvTerm getIntActCVTermRemarkInternal(int tries) {
        IntactCvTerm intactCvTerm = intactDao.getCvTermDao().getByAc(CV_TERM_REMARK_INTERNAL_AC);
        if (intactCvTerm == null && tries > 0) {
            tries--;
            intactCvTerm = getIntActCVTermRemarkInternal(tries);
        } else if (intactCvTerm == null && tries == 0) {
            ObjRetrieveErrorEvent event = new ObjRetrieveErrorEvent(null, null, ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_CV_REMARK_INTERNAL);
            manager.fireOnRetrieveObjErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }
        return intactCvTerm;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    private IntactCvTerm getIntActCVTermNoMutationUpdate(int tries) {
        IntactCvTerm intactCvTerm = intactDao.getCvTermDao().getByAc(CV_TERM_NO_MUTATION_UP);
        if (intactCvTerm == null && tries > 0) {
            tries--;
            intactCvTerm = getIntActCVTermRemarkInternal(tries);
        } else if (intactCvTerm == null && tries == 0) {
            ObjRetrieveErrorEvent event = new ObjRetrieveErrorEvent(null, null, ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_CV_NO_MUTATION_UPDATE);
            manager.fireOnRetrieveObjErrorEvent(event);
            throw new FeatureShortlabelGenerationException();
        }
        return intactCvTerm;
    }

    @Required
    public void setIntactDao(IntactDao intactDao) {
        this.intactDao = intactDao;
    }
}
