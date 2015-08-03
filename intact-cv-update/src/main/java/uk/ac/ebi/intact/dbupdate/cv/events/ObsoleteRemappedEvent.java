package uk.ac.ebi.intact.dbupdate.cv.events;

import java.util.EventObject;

/**
 * This event is fired when an obsolete term could be remapped successfully to another term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class ObsoleteRemappedEvent extends EventObject{

    private String oldTerm;
    private String newTerm;
    private int numberOfUpdates;
    private String updateMessage;
    private String oldIntactAc;
    private String mergedIntactAc;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ObsoleteRemappedEvent(Object source, String oldTerm, String newTerm, String intactAc) {
        super(source);

        this.oldTerm = oldTerm;
        this.newTerm = newTerm;
        this.oldIntactAc = intactAc;
        this.mergedIntactAc = null;
        this.numberOfUpdates = 0;
        this.updateMessage = null;
    }

    public ObsoleteRemappedEvent(Object source, String oldTerm, String newTerm, String oldIntactAc, String newIntactAc, int numberOfUpdates, String updateMessage) {
        super(source);

        this.oldTerm = oldTerm;
        this.newTerm = newTerm;
        this.oldIntactAc = oldIntactAc;
        this.mergedIntactAc = newIntactAc;
        this.numberOfUpdates = numberOfUpdates;
        this.updateMessage = updateMessage;
    }

    public String getOldTerm() {
        return oldTerm;
    }

    public String getNewTerm() {
        return newTerm;
    }

    public int getNumberOfUpdates() {
        return numberOfUpdates;
    }

    public String getUpdateMessage() {
        return updateMessage;
    }

    public String getOldIntactAc() {
        return oldIntactAc;
    }

    public String getMergedIntactAc() {
        return mergedIntactAc;
    }
}
