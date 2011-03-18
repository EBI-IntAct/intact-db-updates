package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for the basic update of a uniprot proteinAc
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("UniprotUpdateEvent")
public class UniprotUpdateEvent extends ProteinEvent{

    String shortLabel;
    String fullName;

    public UniprotUpdateEvent(){
        super();
        this.shortLabel = null;
        this.fullName = null;
    }

    public UniprotUpdateEvent(UpdateProcess updateProcess, Protein protein, int index, String shortlabel, String fullname){
        super(updateProcess, EventName.uniprot_update, protein, index);
        this.shortLabel = shortlabel;
        this.fullName = fullname;
    }

    @Column(name = "shortlabel", nullable = false)
    public String getShortLabel() {
        return shortLabel;
    }

    public void setShortLabel(String shortLabel) {
        this.shortLabel = shortLabel;
    }

    @Column(name = "fullname", nullable = true)
    public String getFullName() {
        return fullName;
    }

    public void setFullName(String fullName) {
        this.fullName = fullName;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( shortLabel != null ) {
            if (!shortLabel.equals( event.getShortLabel())){
                return false;
            }
        }
        else if (event.getShortLabel()!= null){
            return false;
        }

        if ( fullName != null ) {
            if (!fullName.equals( event.getFullName())){
                return false;
            }
        }
        else if (event.getFullName()!= null){
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

        if ( shortLabel != null ) {
            code = 29 * code + shortLabel.hashCode();
        }

        if ( fullName != null ) {
            code = 29 * code + fullName.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UniprotUpdateEvent event = ( UniprotUpdateEvent ) o;

        if ( shortLabel != null ) {
            if (!shortLabel.equals( event.getShortLabel())){
                return false;
            }
        }
        else if (event.getShortLabel()!= null){
            return false;
        }

        if ( fullName != null ) {
            if (!fullName.equals( event.getFullName())){
                return false;
            }
        }
        else if (event.getFullName()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Uniprot update event : [ shortlabel = " + shortLabel != null ? shortLabel : "none" + ", fullname = " + fullName != null ? fullName : "none");

        return buffer.toString();
    }

}
