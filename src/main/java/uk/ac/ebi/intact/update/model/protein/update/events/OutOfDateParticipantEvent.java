package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;

import javax.persistence.*;
import java.util.Collection;

/**
 * Event for participants having feature conflicts when trying to update the proteinAc sequence
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("OutOfDateParticipantEvent")
public class OutOfDateParticipantEvent extends ProteinEventWithRangeUpdate<PersistentInvalidRange> {

    private String remapped_protein;
    private String remapped_parent;

    public OutOfDateParticipantEvent(){
        super();
        this.remapped_protein = null;
        this.remapped_parent = null;

    }

    public OutOfDateParticipantEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprotAc, String fixedProtein, String remapped_parent){
        super(updateProcess, ProteinEventName.participant_with_feature_conflicts, protein, uniprotAc);
        this.remapped_protein = fixedProtein;
        this.remapped_parent = remapped_parent;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentInvalidRange> getInvalidRanges(){
        return super.getUpdatedRanges();
    }

    public void setInvalidRanges(Collection<PersistentInvalidRange> updatedRanges) {
        super.setUpdatedRanges(updatedRanges);
    }

    public boolean addInvalidRange(PersistentInvalidRange up){
        return super.addRangeUpdate(up);
    }

    public boolean removeInvalidRange(PersistentInvalidRange up){
        return super.removeRangeUpdate(up);
    }

    @Column(name="remapped_protein_ac")
    public String getRemapped_protein() {
        return remapped_protein;
    }

    public void setRemapped_protein(String remapped_protein) {
        this.remapped_protein = remapped_protein;
    }

    @Column(name="remapped_parent_ac")
    public String getRemapped_parent() {
        return remapped_parent;
    }

    public void setRemapped_parent(String remapped_protein) {
        this.remapped_parent = remapped_protein;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remapped_protein != null ) {
            if (!remapped_protein.equals( event.getRemapped_protein())){
                return false;
            }
        }
        else if (event.getRemapped_protein()!= null){
            return false;
        }

        if ( remapped_parent != null ) {
            if (!remapped_parent.equals( event.getRemapped_parent())){
                return false;
            }
        }
        else if (event.getRemapped_parent()!= null){
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

        if ( remapped_protein != null ) {
            code = 29 * code + remapped_protein.hashCode();
        }

        if ( remapped_parent!= null ) {
            code = 29 * code + remapped_parent.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remapped_protein != null ) {
            if (!remapped_protein.equals( event.getRemapped_protein())){
                return false;
            }
        }
        else if (event.getRemapped_protein()!= null){
            return false;
        }

        if ( remapped_parent != null ) {
            if (!remapped_parent.equals( event.getRemapped_parent())){
                return false;
            }
        }
        else if (event.getRemapped_parent()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Out of date participant event : [Remapped proteinAc ac = " + (remapped_protein != null ? remapped_protein : "none"));
        buffer.append(", remapped parent ac = "+(remapped_protein != null ? remapped_protein : "none")+"] \n");

        return buffer.toString();
    }
}
