package uk.ac.ebi.intact.tools.feature.shortlabel.generator.manager;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener.ShortlabelGeneratorListener;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorManager {

    protected ShortlabelGeneratorListener shortlabelGeneratorListener;

    public void fireOnRangeErrorEvent(RangeErrorEvent event) {
        shortlabelGeneratorListener.onRangeError(event);
    }

    public void fireOnSuccessfulGeneratedEvent(SuccessfulGeneratedEvent event) {
        shortlabelGeneratorListener.onSuccessfulGenerated(event);
    }
    
    public void fireOnRetrieveObjErrorEvent(ObjRetrieveErrorEvent event) {
        shortlabelGeneratorListener.onRetrieveObjectError(event);
    }

    public void fireOnFeatureAnnotationFoundEvent(FeatureAnnotationFoundEvent event) {
        shortlabelGeneratorListener.onFeatureAnnotationFound(event);
    }

    public void fireOnSeqErrorEvent(SequenceErrorEvent event) {
        shortlabelGeneratorListener.onSequenceError(event);

    }
    
    public void fireOnResSeqChangedEvent(ResultingSequenceChangedEvent event){
        shortlabelGeneratorListener.onResultingSequenceChanged(event);
    }
    
    public void fireOnTypeErrorEvent(TypeErrorEvent event){
        shortlabelGeneratorListener.onFeatureTypeError(event);
    }

    public void setShortlabelGeneratorListener(ShortlabelGeneratorListener shortlabelGeneratorListener) {
        this.shortlabelGeneratorListener = shortlabelGeneratorListener;
    }
}
