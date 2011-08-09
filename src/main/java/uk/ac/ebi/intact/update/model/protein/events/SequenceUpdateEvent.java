package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

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
@DiscriminatorValue("sequence_update")
public class SequenceUpdateEvent extends PersistentProteinEvent {

    /**
     * The new uniprot sequence
     */
    private String newSequence;

    /**
     * The old uniprot sequence
     */
    private String oldSequence;

    /**
     * The relative conservation of the protein sequence
     */
    private double relativeConservation;

    public SequenceUpdateEvent(){
        super();
        this.newSequence = null;
        this.relativeConservation = 0;
        this.oldSequence = null;
    }

    public SequenceUpdateEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprot, String newSequence, String oldSequence, double relativeConservation){
        super(updateProcess, protein, uniprot);
        this.newSequence = newSequence;
        this.relativeConservation = relativeConservation;
        this.oldSequence = oldSequence;
    }

    @Lob
    @Column(name = "new_seq")
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String newSequence) {
        this.newSequence = newSequence;
    }

    @Lob
    @Column(name = "old_seq")
    public String getOldSequence() {
        return oldSequence;
    }

    public void setOldSequence(String oldSequence) {
        this.oldSequence = oldSequence;
    }

    @Column(name = "rel_conservation")
    public double getRelativeConservation() {
        return relativeConservation;
    }

    public void setRelativeConservation(double relativeConservation) {
        this.relativeConservation = relativeConservation;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final SequenceUpdateEvent event = ( SequenceUpdateEvent ) o;

        if ( oldSequence != null ) {
            if (!oldSequence.equals( event.getOldSequence())){
                return false;
            }
        }
        else if (event.getOldSequence()!= null){
            return false;
        }

        if ( newSequence != null ) {
            if (!newSequence.equals( event.getNewSequence())){
                return false;
            }
        }
        else if (event.getNewSequence()!= null){
            return false;
        }

        if (relativeConservation != event.getRelativeConservation()){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( oldSequence != null ) {
            code = 29 * code + oldSequence.hashCode();
        }

        if ( newSequence != null ) {
            code = 29 * code + newSequence.hashCode();
        }

        code = 29 * code + Double.toString(relativeConservation).hashCode();

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final SequenceUpdateEvent event = ( SequenceUpdateEvent ) o;

        if ( oldSequence != null ) {
            if (!oldSequence.equals( event.getOldSequence())){
                return false;
            }
        }
        else if (event.getOldSequence()!= null){
            return false;
        }

        if ( newSequence != null ) {
            if (!newSequence.equals( event.getNewSequence())){
                return false;
            }
        }
        else if (event.getNewSequence()!= null){
            return false;
        }

        if (relativeConservation != event.getRelativeConservation()){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Sequence update event : \n");

        buffer.append("Relative conservation : " + relativeConservation);
        buffer.append("Old sequence : " + oldSequence != null ? oldSequence : "none");
        buffer.append("New sequence : " + newSequence != null ? newSequence : "none");

        return buffer.toString();
    }

}
