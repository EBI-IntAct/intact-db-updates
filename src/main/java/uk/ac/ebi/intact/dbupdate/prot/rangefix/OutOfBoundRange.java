package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import uk.ac.ebi.intact.model.Range;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Aug-2010</pre>
 */

public class OutOfBoundRange {

    String sequence;

    Range rangeOutOfBound;

    String message;

    public OutOfBoundRange(Range range, String sequence) {
        this(range, sequence, null);
    }

    public OutOfBoundRange(Range range, String sequence, String message) {
        this.rangeOutOfBound = range;
        this.sequence = sequence;
        this.message = message;
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public Range getOutOfBoundRange() {
        return rangeOutOfBound;
    }

    public void setRangeOutOfBound(Range rangeOutOfBound) {
        this.rangeOutOfBound = rangeOutOfBound;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }
}
