package uk.ac.ebi.intact.dbupdate.feature.mutation.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class LoggingListener extends AbstractShortlabelGeneratorListener {
    private static final Log log = LogFactory.getLog(LoggingListener.class);

    public void onRangeError(RangeErrorEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getErrorType().equals(RangeErrorEvent.ErrorType.RES_SEQ_NULL)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(RangeErrorEvent.ErrorType.ORG_SEQ_NULL)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(RangeErrorEvent.ErrorType.START_POS_ZERO)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(RangeErrorEvent.ErrorType.START_POS_UNDETERMINED)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(RangeErrorEvent.ErrorType.RANGE_NULL)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
        }
    }

    public void onModifiedMutationShortlabel(ModifiedMutationShortlabelEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Successful generated shortlabel: " + event.getFeatureAc() + " old shortlabel was: " + event.getOriginalShortlabel() + " new shortlabel is: " + event.getFeatureEvidence().getShortName());
        }
    }

    public void onUnmodifiedMutationShortlabel(UnmodifiedMutationShortlabelEvent event) {
        if (log.isDebugEnabled()) {
            log.debug("Successful generated shortlabel: " + event.getFeatureAc() + ". Shortlabel has not been changed");
        }
    }

    public void onRetrieveObjectError(ObjRetrieveErrorEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getErrorType().equals(ObjRetrieveErrorEvent.ErrorType.UNABLE_RETRIEVE_INTERACTOR)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_FEATURE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_CV_NO_MUTATION_UPDATE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(ObjRetrieveErrorEvent.ErrorType.UNABLE_TO_RETRIEVE_CV_NO_MUTATION_EXPORT)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(ObjRetrieveErrorEvent.ErrorType.UNABLE_RETRIEVE_INTERACTOR_SEQUENCE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
        }
    }

    public void onAnnotationFound(AnnotationFoundEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getType().equals(AnnotationFoundEvent.AnnotationType.NO_MUTATION_EXPORT)) {
                log.debug("Feature contains 'no-mutation-export' annotation: " + event.getFeatureAc());
            }
            if (event.getType().equals(AnnotationFoundEvent.AnnotationType.NO_MUTATION_UPDATE)) {
                log.debug("Feature contains 'no-mutation-update' annotation: " + event.getFeatureAc());
            }
            if (event.getType().equals(AnnotationFoundEvent.AnnotationType.NO_UNIPROT_UPDATE)) {
                log.debug("Feature contains 'no-uniprot-update' annotation: " + event.getFeatureAc());
            }
        }
    }

    public void onSequenceError(SequenceErrorEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getErrorType().equals(SequenceErrorEvent.ErrorType.ORG_SEQ_WRONG)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(SequenceErrorEvent.ErrorType.RES_SEQ_CONTAINS_LOWER_CASE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(SequenceErrorEvent.ErrorType.UNABLE_CALCULATE_ORG_SEQ)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
        }
    }

    public void onResultingSequenceChanged(ResultingSequenceChangedEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getChangeType().equals(ResultingSequenceChangedEvent.ChangeType.DECREASE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getChangeType().equals(ResultingSequenceChangedEvent.ChangeType.INCREASE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getChangeType().equals(ResultingSequenceChangedEvent.ChangeType.DELETION)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getChangeType().equals(ResultingSequenceChangedEvent.ChangeType.STABLE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
        }
    }

    public void onObjectTypeError(TypeErrorEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getErrorType().equals(TypeErrorEvent.ObjTypeErrorType.WRONG_FEATURE_TYPE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
            if (event.getErrorType().equals(TypeErrorEvent.ObjTypeErrorType.WRONG_INTERACTOR_TYPE)) {
                log.debug(event.getMessage() + ": " + event.getFeatureAc());
            }
        }
    }

    public void onOtherErrorEvent(OtherErrorEvent event) {
        if (log.isDebugEnabled()) {
            if (event.getErrorType().equals(OtherErrorEvent.ErrorType.SHORT_LABEL_TOO_LONG)) {
                log.debug(event.getErrorType().getMessage() + ": " + event.getFeatureAc() + ". New shortlabel: " + event.getErrorDetails());
            }
        }
    }
}