package uk.ac.ebi.intact.dbupdate.feature.mutation.listener;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener.ShortlabelGeneratorListener;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public abstract class AbstractShortlabelGeneratorListener implements ShortlabelGeneratorListener {

    public void onRangeError(RangeErrorEvent event) {
        //do nothing
    }

    public void onModifiedMutationShortlabel(ModifiedMutationShortlabelEvent event){
        //do nothing
    }

    public void onUnmodifiedMutationShortlabel(UnmodifiedMutationShortlabelEvent event){
        //do nothing
    }

    public void onRetrieveObjectError(ObjRetrieveErrorEvent event) {
        //do nothing
    }

    public void onAnnotationFound(AnnotationFoundEvent event) {
        //do nothing
    }

    public void onSequenceError(SequenceErrorEvent event) {
        //do nothing
    }

    public void onResultingSequenceChanged(ResultingSequenceChangedEvent event) {
        //do nothing
    }

    public void onObjectTypeError(TypeErrorEvent event) {
        //do nothing
    }
}
