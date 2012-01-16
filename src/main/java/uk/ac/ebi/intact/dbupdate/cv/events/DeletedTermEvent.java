package uk.ac.ebi.intact.dbupdate.cv.events;

import java.util.EventObject;

/**
 * Event for deleted terms
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/01/12</pre>
 */

public class DeletedTermEvent extends EventObject {

    private String termAc;
    private String intactAc;
    private String shortLabel;

    private String message;

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public DeletedTermEvent(Object source, String termAc, String label, String intactAc, String message) {
        super(source);
        this.termAc = termAc;
        this.intactAc = intactAc;
        this.shortLabel = label;
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

    public String getMessage() {
        return message;
    }
}
