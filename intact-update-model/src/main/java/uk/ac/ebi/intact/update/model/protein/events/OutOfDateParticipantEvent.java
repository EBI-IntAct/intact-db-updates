package uk.ac.ebi.intact.update.model.protein.events;

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
@DiscriminatorValue("out_of_date_participant")
public class OutOfDateParticipantEvent extends ProteinEventWithRangeUpdate<PersistentInvalidRange> {

    /**
     * The intact ac of the remapped protein
     */
    private String remappedProtein;

    /**
     * The intact ac of the remapped updateProcess
     */
    private String remappedParent;

    public OutOfDateParticipantEvent(){
        super();
        this.remappedProtein = null;
        this.remappedParent = null;

    }

    public OutOfDateParticipantEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprotAc, String fixedProtein, String remapped_parent){
        super(updateProcess, protein, uniprotAc);
        this.remappedProtein = fixedProtein;
        this.remappedParent = remapped_parent;
    }

    @Transient
    public Collection<PersistentInvalidRange> getInvalidRanges(){
        return super.getUpdatedRanges();
    }

    @Override
    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentInvalidRange> getUpdatedRanges(){
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

    @Column(name="remappedProtein")
    public String getRemappedProtein() {
        return remappedProtein;
    }

    public void setRemappedProtein(String remappedProtein) {
        this.remappedProtein = remappedProtein;
    }

    @Column(name="remappedParent")
    public String getRemappedParent() {
        return remappedParent;
    }

    public void setRemappedParent(String remapped_protein) {
        this.remappedParent = remapped_protein;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remappedProtein != null ) {
            if (!remappedProtein.equals( event.getRemappedProtein())){
                return false;
            }
        }
        else if (event.getRemappedProtein()!= null){
            return false;
        }

        if ( remappedParent != null ) {
            if (!remappedParent.equals( event.getRemappedParent())){
                return false;
            }
        }
        else if (event.getRemappedParent()!= null){
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

        if ( remappedProtein != null ) {
            code = 29 * code + remappedProtein.hashCode();
        }

        if ( remappedParent != null ) {
            code = 29 * code + remappedParent.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remappedProtein != null ) {
            if (!remappedProtein.equals( event.getRemappedProtein())){
                return false;
            }
        }
        else if (event.getRemappedProtein()!= null){
            return false;
        }

        if ( remappedParent != null ) {
            if (!remappedParent.equals( event.getRemappedParent())){
                return false;
            }
        }
        else if (event.getRemappedParent()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Out of date participant event : [Remapped proteinAc ac = " + (remappedProtein != null ? remappedProtein : "none"));
        buffer.append(", remapped updateProcess ac = "+(remappedProtein != null ? remappedProtein : "none")+"] \n");

        return buffer.toString();
    }
}
