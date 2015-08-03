package uk.ac.ebi.intact.update.model.protein.events;

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
@DiscriminatorValue("secondary_protein")
public class SecondaryProteinEvent extends PersistentProteinEvent {

    /**
     * The old primary ac in uniprot which is now a secondary ac
     */
    String secondaryUniprotAc;

    public SecondaryProteinEvent(){
        super();
        this.secondaryUniprotAc = null;

    }

    public SecondaryProteinEvent(ProteinUpdateProcess updateProcess, Protein protein, String originalUniprotAc, String updatedPrimaryAc){
        super(updateProcess,protein, updatedPrimaryAc);
        this.secondaryUniprotAc = originalUniprotAc;
    }

    @Column(name = "secondary_uniprot")
    public String getSecondaryUniprotAc() {
        return secondaryUniprotAc;
    }

    public void setSecondaryUniprotAc(String updatedAc) {
        this.secondaryUniprotAc = updatedAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( secondaryUniprotAc != null ) {
            if (!secondaryUniprotAc.equals( event.getSecondaryUniprotAc())){
                return false;
            }
        }
        else if (event.getSecondaryUniprotAc()!= null){
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

        if ( secondaryUniprotAc != null ) {
            code = 29 * code + secondaryUniprotAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final SecondaryProteinEvent event = ( SecondaryProteinEvent ) o;

        if ( secondaryUniprotAc != null ) {
            if (!secondaryUniprotAc.equals( event.getSecondaryUniprotAc())){
                return false;
            }
        }
        else if (event.getSecondaryUniprotAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Secondary proteinAc event : [ updated primary ac = " + secondaryUniprotAc != null ? secondaryUniprotAc : "none");

        return buffer.toString();
    }
}
