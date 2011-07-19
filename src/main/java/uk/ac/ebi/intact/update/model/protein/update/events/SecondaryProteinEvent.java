package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

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
@DiscriminatorFormula("objclass")
@DiscriminatorValue("SecondaryProteinEvent")
public class SecondaryProteinEvent extends PersistentProteinEvent {

    String secondaryAc;
    String primaryAc;

    public SecondaryProteinEvent(){
        super();
        this.secondaryAc = null;
        this.primaryAc = null;

    }

    public SecondaryProteinEvent(ProteinUpdateProcess updateProcess, Protein protein, String secondaryAc, String primaryAc){
        super(updateProcess, ProteinEventName.secondary_protein, protein);
        this.primaryAc = primaryAc;
        this.secondaryAc = secondaryAc;
    }

    @Column(name = "secondary_ac")
    public String getSecondaryAc() {
        return secondaryAc;
    }

    public void setSecondaryAc(String secondaryAc) {
        this.secondaryAc = secondaryAc;
    }

    @Column(name = "primary_ac")
    public String getPrimaryAc() {
        return primaryAc;
    }

    public void setPrimaryAc(String primaryAc) {
        this.primaryAc = primaryAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( primaryAc != null ) {
            if (!primaryAc.equals( event.getPrimaryAc())){
                return false;
            }
        }
        else if (event.getPrimaryAc()!= null){
            return false;
        }

        if ( secondaryAc != null ) {
            if (!secondaryAc.equals( event.getSecondaryAc())){
                return false;
            }
        }
        else if (event.getSecondaryAc()!= null){
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

        if ( primaryAc != null ) {
            code = 29 * code + primaryAc.hashCode();
        }

        if ( secondaryAc != null ) {
            code = 29 * code + secondaryAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( primaryAc != null ) {
            if (!primaryAc.equals( event.getPrimaryAc())){
                return false;
            }
        }
        else if (event.getPrimaryAc()!= null){
            return false;
        }

        if ( secondaryAc != null ) {
            if (!secondaryAc.equals( event.getSecondaryAc())){
                return false;
            }
        }
        else if (event.getSecondaryAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Secondary proteinAc event : [ primary ac = " + primaryAc != null ? primaryAc : "none" + ", secondary ac = "+ secondaryAc != null ? secondaryAc : "none");

        return buffer.toString();
    }
}
