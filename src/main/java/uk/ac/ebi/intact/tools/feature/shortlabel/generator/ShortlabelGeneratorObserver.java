package uk.ac.ebi.intact.tools.feature.shortlabel.generator;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface ShortlabelGeneratorObserver {
    void onRangeErrorEvent(RangeErrorEvent event);
    
    void onSuccessfulGeneratedEvent(SuccessfulGeneratedEvent event);
    
    void onRetrieveObjErrorEvent(ObjRetrieveErrorEvent event);

    void onFeatureAnnotationFoundEvent(FeatureAnnotationFoundEvent event);

    void onSeqErrorEvent(SequenceErrorEvent event);

    void onResSeqChangedEvent(ResultingSequenceChangedEvent event);

    void onTypeErrorEvent(TypeErrorEvent event);
}
