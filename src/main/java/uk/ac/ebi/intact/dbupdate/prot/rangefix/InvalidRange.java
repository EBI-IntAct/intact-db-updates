package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import uk.ac.ebi.intact.model.Range;
import uk.ac.ebi.intact.model.util.FeatureUtils;

/**
 * This class contains informations about the invalid range
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Aug-2010</pre>
 */

public class InvalidRange extends UpdatedRange{

    /**
     * The new sequence of the protein
     */
    String sequence;

    /**
     * The version of the protein sequence for what this range was valid
     */
    int validSequenceVersion;

    /**
     * The uniprot ac associated with the sequence version
     */
    String uniprotAc;

    /**
     * The message to add at the feature level
     */
    String message;

    boolean isOutOfDate = false;

    private String fromStatus;
    private String toStatus;

    String oldPositions;
    String newRangePositions;

    public InvalidRange(Range range, Range newRange, String sequence, String message, String fromStatus, String toStatus, boolean outOfDate) {
        super(range, newRange);

        this.sequence = sequence;
        this.message = message;
        this.validSequenceVersion = -1;
        this.uniprotAc = null;

        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.isOutOfDate = outOfDate;

        if (range != null){
            oldPositions = FeatureUtils.convertRangeIntoString(range);
        }
        else{
            oldPositions = null;
        }

        if (newRange != null){
            newRangePositions = FeatureUtils.convertRangeIntoString(newRange);
        }
        else{
            newRangePositions = null;
        }
    }

    public InvalidRange(Range range, Range newRange, String sequence, String message, int sequenceVersion, String fromStatus, String toStatus, boolean outOfDate) {
        super(range, newRange);

        this.sequence = sequence;
        this.message = message;
        this.validSequenceVersion = sequenceVersion;
        this.uniprotAc = null;

        this.fromStatus = fromStatus;
        this.toStatus = toStatus;
        this.isOutOfDate = outOfDate;

        if (range != null){
            oldPositions = FeatureUtils.convertRangeIntoString(range);
        }
        else{
            oldPositions = null;
        }

        if (newRange != null){
            newRangePositions = FeatureUtils.convertRangeIntoString(newRange);
        }
        else{
            newRangePositions = null;
        }
    }

    public String getSequence() {
        return sequence;
    }

    public void setSequence(String sequence) {
        this.sequence = sequence;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public int getValidSequenceVersion() {
        return validSequenceVersion;
    }

    public void setValidSequenceVersion(int validSequenceVersion) {
        this.validSequenceVersion = validSequenceVersion;
    }

    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    public boolean isOutOfDate() {
        return isOutOfDate;
    }

    public String getOldPositions() {
        return oldPositions;
    }

    public String getNewRangePositions() {
        return newRangePositions;
    }
}
