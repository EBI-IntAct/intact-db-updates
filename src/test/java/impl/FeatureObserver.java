package impl;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGeneratorObserver;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureObserver implements ShortlabelGeneratorObserver {
    @Override
    public void onRangeErrorEvent(RangeErrorEvent event) {
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
    public void onSuccessfulGeneratedEvent(SuccessfulGeneratedEvent event) {
        System.out.println("Everything seems fine about " + event.getFeatureAc() + "OS: " + event.getOriginalShortlabel() + " -> " + event.getFeatureEvidence().getShortName());
    }

    @Override
    public void onRetrieveObjErrorEvent(ObjRetrieveErrorEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onFeatureAnnotationFoundEvent(FeatureAnnotationFoundEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onSeqErrorEvent(SequenceErrorEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onResSeqChangedEvent(ResultingSequenceChangedEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onTypeErrorEvent(TypeErrorEvent event) {
        System.out.println(event.getMessage());
    }

    @Override
    public void onUndefinedMutation(UndefinedMutationEvent undefinedMutationEvent) {

    }
}
