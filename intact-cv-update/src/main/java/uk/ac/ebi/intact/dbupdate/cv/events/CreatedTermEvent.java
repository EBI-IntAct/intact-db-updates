package uk.ac.ebi.intact.dbupdate.cv.events;

import java.util.EventObject;

/**
 * This event is fired when a new CvTerm has been created
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class CreatedTermEvent extends EventObject {

    private String termAc;
    private String intactAc;
    private String shortLabel;

    private boolean isHidden;
    private String message;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public CreatedTermEvent(Object source, String termAc, String label, String intactAc, boolean isHidden, String message) {
        super(source);
        this.termAc = termAc;
        this.intactAc = intactAc;
        this.shortLabel = label;
        this.isHidden = isHidden;
        this.message = message;
    }

    public String getTermAc() {
        return termAc;
    }

    public String getIntactAc() {
        return intactAc;
    }

    public String getShortLabel() {
        return shortLabel;
    }

    public boolean isHidden() {
        return isHidden;
    }

    public String getMessage() {
        return message;
    }
}
