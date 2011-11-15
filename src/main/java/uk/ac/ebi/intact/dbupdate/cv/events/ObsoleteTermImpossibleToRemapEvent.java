package uk.ac.ebi.intact.dbupdate.cv.events;

import java.util.ArrayList;
import java.util.Collection;
import java.util.EventObject;

/**
 * Event fired when obsolete term cannot be remapped
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class ObsoleteTermImpossibleToRemapEvent extends EventObject {

    private String obsoleteId;
    private String cvIntactAc;
    private String cvLabel;

    private String message;

    private Collection<String> possibleTerms = new ArrayList<String>();

    /**
     * Constructs a prototypical Event.
     *
     * @param source The object on which the Event initially occurred.
     * @throws IllegalArgumentException if source is null.
     */
    public ObsoleteTermImpossibleToRemapEvent(Object source, String obsoleteId, String intactAc, String label, String message) {
        super(source);
        this.obsoleteId = obsoleteId;
        this.cvIntactAc = intactAc;
        this.cvLabel = label;
        this.message = message;
    }

    public String getObsoleteId() {
        return obsoleteId;
    }

    public String getCvIntactAc() {
        return cvIntactAc;
    }

    public String getCvLabel() {
        return cvLabel;
    }

    public String getMessage() {
        return message;
    }

    public Collection<String> getPossibleTerms() {
        return possibleTerms;
    }
}
