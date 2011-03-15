package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * Event for updated sequences
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("SequenceUpdateEvent")
public class SequenceUpdateEvent extends ProteinEvent {

    private String newSequence;
    private String oldSequence;

    private double relativeConservation;

    public SequenceUpdateEvent(){
        super();
        this.newSequence = null;
        this.relativeConservation = 0;
        this.oldSequence = null;
    }

    public SequenceUpdateEvent(UpdateProcess updateProcess, String newSequence, String oldSequence, double relativeConservation, EventName name, Protein protein, int index){
        super(updateProcess, name, protein, index);
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

    @Column(name = "relative_conservation")
    public double getRelativeConservation() {
        return relativeConservation;
    }

    public void setRelativeConservation(double relativeConservation) {
        this.relativeConservation = relativeConservation;
    }
}
