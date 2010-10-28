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
@DiscriminatorValue("SequenceUpdateEvent")
@Table(name = "ia_updated_protein_sequence")
public class SequenceUpdateEvent extends ProteinEvent {

    private String newSequence;
    private String message;
    private double relativeConservation;

    public SequenceUpdateEvent(){
        super();
        this.newSequence = null;
        this.message = null;
        this.relativeConservation = 0;
    }

    public SequenceUpdateEvent(String newSequence, double relativeConservation, EventName name, Date created){
        super(name, created);
        this.newSequence = newSequence;
        this.relativeConservation = relativeConservation;
    }

    @Lob
    @Column(name = "new_sequence", nullable = false)
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String newSequence) {
        this.newSequence = newSequence;
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
