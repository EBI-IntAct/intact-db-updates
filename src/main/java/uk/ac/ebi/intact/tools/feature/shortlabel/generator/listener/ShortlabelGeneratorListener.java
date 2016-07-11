package uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

import java.util.EventListener;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface ShortlabelGeneratorListener extends EventListener {
    void onRangeError(RangeErrorEvent event);
    
    void onSuccessfulGenerated(SuccessfulGeneratedEvent event);
    
    void onRetrieveObjectError(ObjRetrieveErrorEvent event);

    void onFeatureAnnotationFound(FeatureAnnotationFoundEvent event);

    void onSequenceError(SequenceErrorEvent event);

    void onResultingSequenceChanged(ResultingSequenceChangedEvent event);

    void onFeatureTypeError(TypeErrorEvent event);
}
