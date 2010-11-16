package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import uk.ac.ebi.intact.model.Range;

/**
 * This class contains informations about the invalid range
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Aug-2010</pre>
 */

public class InvalidRange {

    /**
     * The sequence of the protein
     */
    String sequence;

    /**
     * The new range positions
     */
    String newRanges;

    /**
     * The invalid range
     */
    Range invalidRange;

    /**
     * The message to add at the feature level
     */
    String message;

    public InvalidRange(Range range, String sequence, String message) {
        this.invalidRange = range;
        this.sequence = sequence;
        this.message = message;
    }

    public InvalidRange(Range range, String sequence, String message, String newRanges) {
        this.invalidRange = range;
        this.sequence = sequence;
        this.message = message;
        this.newRanges = newRanges;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public Range getInvalidRange() {
        return invalidRange;
    }

    public void setInvalidRange(Range invalidRange) {
        this.invalidRange = invalidRange;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public String getNewRanges() {
        return newRanges;
    }

    public void setNewRanges(String newRanges) {
        this.newRanges = newRanges;
    }
}
