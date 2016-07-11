package impl;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener.ShortlabelGeneratorListener;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureListener implements ShortlabelGeneratorListener {
    @Override
    public void onRangeError(RangeErrorEvent event) {
        if (event.getErrorType().equals(RangeErrorEvent.ErrorType.RANGE_NULL)) {
            System.out.println(event.getFeatureAc() + "\t" + event.getMessage());
        } else if (event.getErrorType().equals(RangeErrorEvent.ErrorType.ORG_SEQ_NULL)) {
            System.out.println(event.getFeatureAc() + "\t" + event.getMessage());
        } else if (event.getErrorType().equals(RangeErrorEvent.ErrorType.RES_SEQ_NULL)) {
            System.out.println(event.getFeatureAc() + "\t" + event.getMessage());
        } else if (event.getErrorType().equals(RangeErrorEvent.ErrorType.START_POS_ZERO)) {
            System.out.println(event.getFeatureAc() + "\t" + event.getMessage());
        } else if (event.getErrorType().equals(RangeErrorEvent.ErrorType.START_POS_UNDETERMINED)) {
            System.out.println(event.getFeatureAc() + "\t" + event.getMessage());
        }
    }

    @Override
    public void onSuccessfulGenerated(SuccessfulGeneratedEvent event) {
        System.out.println("Everything seems fine about " + event.getFeatureAc() + "OS: " + event.getOriginalShortlabel() + " -> " + event.getFeatureEvidence().getShortName());
    }

    @Override
    public void onRetrieveObjectError(ObjRetrieveErrorEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onFeatureAnnotationFound(FeatureAnnotationFoundEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onSequenceError(SequenceErrorEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onResultingSequenceChanged(ResultingSequenceChangedEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onFeatureTypeError(TypeErrorEvent event) {
        System.out.println(event.getMessage());
    }
}
