package uk.ac.ebi.intact.tools.feature.shortlabel.generator;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorManager {

    protected ShortlabelGeneratorObserver shortlabelGeneratorObserver;

    public void fireOnRangeErrorEvent(RangeErrorEvent event) {
        shortlabelGeneratorObserver.onRangeErrorEvent(event);
    }

    public void fireOnSuccessfulGeneratedEvent(SuccessfulGeneratedEvent event) {
        shortlabelGeneratorObserver.onSuccessfulGeneratedEvent(event);

    }
    
    public void fireOnRetrieveObjErrorEvent(ObjRetrieveErrorEvent event) {
        shortlabelGeneratorObserver.onRetrieveObjErrorEvent(event);

    }

    public void fireOnFeatureAnnotationFoundEvent(FeatureAnnotationFoundEvent event) {
        shortlabelGeneratorObserver.onFeatureAnnotationFoundEvent(event);

    }

    public void fireOnSeqErrorEvent(SequenceErrorEvent event) {
        shortlabelGeneratorObserver.onSeqErrorEvent(event);

    }
    
    public void fireOnResSeqChangedEvent(ResultingSequenceChangedEvent event){
        shortlabelGeneratorObserver.onResSeqChangedEvent(event);
        
    }
    
    public void fireOnTypeErrorEvent(TypeErrorEvent event){
        shortlabelGeneratorObserver.onTypeErrorEvent(event);
        
    }

    public void setShortlabelGeneratorObserver(ShortlabelGeneratorObserver shortlabelGeneratorObserver) {
        this.shortlabelGeneratorObserver = shortlabelGeneratorObserver;
    }

    public void fireOnUndefinedMutationEvent(UndefinedMutationEvent event){
        shortlabelGeneratorObserver.onUndefinedMutation(event);
    }
}
