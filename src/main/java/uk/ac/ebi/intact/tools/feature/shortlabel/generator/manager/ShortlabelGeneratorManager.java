package uk.ac.ebi.intact.tools.feature.shortlabel.generator.manager;

import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.listener.ShortlabelGeneratorListener;

import javax.swing.event.EventListenerList;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorManager {

    protected EventListenerList listenerList = new EventListenerList();

    public void fireOnRangeErrorEvent(RangeErrorEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onRangeError(event);
        }
    }

    public void fireOnUnmodifiedMutationShortlabelEvent (UnmodifiedMutationShortlabelEvent event){
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onUnmodifiedMutationShortlabel(event);
        }
    }

    public void fireOnModifiedMutationShortlabelEvent (ModifiedMutationShortlabelEvent event){
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onModifiedMutationShortlabel(event);
        }
    }

    public void fireOnRetrieveObjErrorEvent(ObjRetrieveErrorEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onRetrieveObjectError(event);
        }
    }

    public void fireOnAnnotationFoundEvent(AnnotationFoundEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onAnnotationFound(event);
        }
    }

    public void fireOnSeqErrorEvent(SequenceErrorEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onSequenceError(event);
        }
    }

    public void fireOnResSeqChangedEvent(ResultingSequenceChangedEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onResultingSequenceChanged(event);
        }
    }

    public void fireOnObjectTypeErrorEvent(TypeErrorEvent event) {
        for (ShortlabelGeneratorListener eventListener : getListeners(ShortlabelGeneratorListener.class)){
            eventListener.onObjectTypeError(event);
        }
    }


    public void addListener(ShortlabelGeneratorListener listener) {
        listenerList.add(ShortlabelGeneratorListener.class, listener);
    }

    protected <T> List<T> getListeners(Class<T> listenerClass) {
        List list = new ArrayList();

        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ShortlabelGeneratorListener.class) {
                if (listenerClass.isAssignableFrom(listeners[i + 1].getClass())) {
                    list.add(listeners[i + 1]);
                }
            }
        }
        return list;
    }
}
