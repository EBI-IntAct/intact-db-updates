package uk.ac.ebi.intact.update.model.proteinupdate.range;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_invalid_range")
@DiscriminatorValue("InvalidRange")
public class InvalidRange extends UpdatedRange {

    private String fromStatus;
    private String toStatus;
    private String currentSequence;
    private String updatedSequence;
    private String errorMessage;

    public InvalidRange(){
        super();
        fromStatus = null;
        toStatus = null;
        currentSequence = null;
        updatedSequence = null;
        errorMessage = null;
    }

    public InvalidRange(String rangeAc, int oldFromStart, int oldFromEnd, int oldToStart, int oldToEnd, int newFromStart, int newFromEnd, int newToStart, int newToEnd, String currentSequence, String updatedSequence, String errorMessage){
        super(rangeAc, oldFromStart, oldFromEnd, oldToStart, oldToEnd, newFromStart, newFromEnd, newToStart, newToEnd);
        setCreated(new Date(System.currentTimeMillis()));
        this.currentSequence = currentSequence;
        this.updatedSequence = updatedSequence;
        this.errorMessage = errorMessage;
    }

    public InvalidRange(String rangeAc, int oldFromStart, int oldFromEnd, int oldToStart, int oldToEnd, String currentSequence, String errorMessage){
        super(rangeAc, oldFromStart, oldFromEnd, oldToStart, oldToEnd, 0, 0, 0, 0);
        setNewPositions(null);
        setCreated(new Date(System.currentTimeMillis()));
        this.currentSequence = currentSequence;
        this.updatedSequence = null;
        this.errorMessage = errorMessage;
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

    @Lob
    @Column(name = "current_sequence", nullable = true)
    public String getCurrentSequence() {
        return currentSequence;
    }

    public void setCurrentSequence(String currentSequence) {
        this.currentSequence = currentSequence;
    }

    @Lob
    @Column(name = "updated_sequence", nullable = true)
    public String getUpdatedSequence() {
        return updatedSequence;
    }

    public void setUpdatedSequence(String updatedSequence) {
        this.updatedSequence = updatedSequence;
    }

    @Column(name = "error_message", nullable = false)
    public String getErrorMessage() {
        return errorMessage;
    }

    public void setErrorMessage(String errorMessage) {
        this.errorMessage = errorMessage;
    }
}
