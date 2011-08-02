package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for secondary proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorValue("SecondaryProteinEvent")
public class SecondaryProteinEvent extends PersistentProteinEvent {

    String updatedPrimaryAc;

    public SecondaryProteinEvent(){
        super();
        this.updatedPrimaryAc = null;

    }

    public SecondaryProteinEvent(ProteinUpdateProcess updateProcess, Protein protein, String originalUniprotAc, String updatedPrimaryAc){
        super(updateProcess, ProteinEventName.secondary_protein, protein, originalUniprotAc);
        this.updatedPrimaryAc = updatedPrimaryAc;
    }

    @Column(name = "updated_primary_ac")
    public String getUpdatedPrimaryAc() {
        return updatedPrimaryAc;
    }

    public void setUpdatedPrimaryAc(String updatedAc) {
        this.updatedPrimaryAc = updatedAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( updatedPrimaryAc != null ) {
            if (!updatedPrimaryAc.equals( event.getUpdatedPrimaryAc())){
                return false;
            }
        }
        else if (event.getUpdatedPrimaryAc()!= null){
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

        if ( updatedPrimaryAc != null ) {
            code = 29 * code + updatedPrimaryAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( updatedPrimaryAc != null ) {
            if (!updatedPrimaryAc.equals( event.getUpdatedPrimaryAc())){
                return false;
            }
        }
        else if (event.getUpdatedPrimaryAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Secondary proteinAc event : [ updated primary ac = " + updatedPrimaryAc != null ? updatedPrimaryAc : "none");

        return buffer.toString();
    }
}
