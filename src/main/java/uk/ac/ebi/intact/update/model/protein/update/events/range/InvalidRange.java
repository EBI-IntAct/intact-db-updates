package uk.ac.ebi.intact.update.model.protein.update.events.range;

import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Class for invalid ranges found during the protein update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("InvalidRange")
public class InvalidRange extends UpdatedRange {

    private String fromStatus;
    private String toStatus;
    private String errorMessage;
    private int sequenceVersion;

    public InvalidRange(){
        super();
        fromStatus = null;
        toStatus = null;
        errorMessage = null;
        this.sequenceVersion = -1;
    }

    public InvalidRange(UpdateProcess updateProcess, String componentAc, String rangeAc, String oldSequence, String startStatus, String endStatus, String oldPositions, String error, int sequenceVersion){
        super(updateProcess, componentAc, rangeAc, oldSequence, null, oldPositions, null);
        this.errorMessage = error;
        this.sequenceVersion = sequenceVersion;
        this.fromStatus = startStatus;
        this.toStatus = endStatus;
    }

    public InvalidRange(UpdateProcess updateProcess, String componentAc, String rangeAc, String oldSequence, String newSequence, String startStatus, String endStatus, String oldPositions, String newPositions, String error, int sequenceVersion){
        super(updateProcess, componentAc, rangeAc, oldSequence, newSequence, oldPositions, newPositions);
        this.errorMessage = error;
        this.sequenceVersion = sequenceVersion;
        this.fromStatus = startStatus;
        this.toStatus = endStatus;
    }

    @Column(name = "from_range_status", nullable = true)
    public String getFromStatus() {
        return fromStatus;
    }

    public void setFromStatus(String fromStatus) {
        this.fromStatus = fromStatus;
    }

    @Column(name = "to_range_status", nullable = true)
    public String getToStatus() {
        return toStatus;
    }

    public void setToStatus(String toStatus) {
        this.toStatus = toStatus;
    }

    @Column(name = "error_message", nullable = false)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }

    @Column(name = "sequence_version")
    public int getSequenceVersion() {
        return sequenceVersion;
    }

    public void setSequenceVersion(int sequenceVersion) {
        this.sequenceVersion = sequenceVersion;
    }
}
