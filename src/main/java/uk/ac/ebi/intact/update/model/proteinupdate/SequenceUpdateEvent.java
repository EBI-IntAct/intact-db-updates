package uk.ac.ebi.intact.update.model.proteinupdate;

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
@DiscriminatorValue("SequenceUpdateEventDao")
public class SequenceUpdateEvent extends ProteinEvent {

    private String newSequence;
    private String oldSequence;
    private String message;
    private double relativeConservation;

    public SequenceUpdateEvent(){
        super();
        this.newSequence = null;
        this.message = null;
        this.relativeConservation = 0;
        this.oldSequence = null;
    }

    public SequenceUpdateEvent(String newSequence, String oldSequence, double relativeConservation, EventName name, Date created, int index){
        super(name, created, index);
        this.newSequence = newSequence;
        this.relativeConservation = relativeConservation;
        this.oldSequence = oldSequence;
    }

    @Lob
    @Column(name = "new_sequence", nullable = false)
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String newSequence) {
        this.newSequence = newSequence;
    }

    @Lob
    @Column(name = "old_sequence", nullable = true)
    public String getOldSequence() {
        return oldSequence;
    }

    public void setOldSequence(String oldSequence) {
        this.oldSequence = oldSequence;
    }

    @Column(name = "message", nullable = true)
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Column(name = "relative_conservation")
    public double getRelativeConservation() {
        return relativeConservation;
    }

    public void setRelativeConservation(double relativeConservation) {
        this.relativeConservation = relativeConservation;
    }
}
