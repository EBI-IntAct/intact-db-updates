package uk.ac.ebi.intact.dbupdate.cv.events;

import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;

import java.util.EventObject;

/**
 * This event is fired when an update error has been found
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class UpdateErrorEvent extends EventObject {
    private CvUpdateError updateError;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public UpdateErrorEvent(Object source, CvUpdateError error) {
        super(source);
        this.updateError = error;
    }

    public CvUpdateError getUpdateError() {
        return updateError;
    }
}
