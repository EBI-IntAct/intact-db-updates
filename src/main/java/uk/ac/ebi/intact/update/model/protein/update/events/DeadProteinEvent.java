package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("DeadProteinEvent")
public class DeadProteinEvent extends PersistentProteinEvent {

    private String uniprotReference;

    public DeadProteinEvent(){
        super();
        this.uniprotReference = null;
    }

    public DeadProteinEvent(UpdateProcess process, Protein protein, InteractorXref uniprotRef){
        super(process, EventName.dead_protein, protein);

        setUniprotReference(uniprotRef);
    }

    @Column(name = "dead_uniprot")
    public String getUniprotReference() {
        return uniprotReference;
    }

    public void setUniprotReference(String uniprotReference) {
        this.uniprotReference = uniprotReference;
    }

    public void setUniprotReference(InteractorXref xRef){
        if (xRef != null){
            this.uniprotReference = xRef.getPrimaryId();
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final DeadProteinEvent event = ( DeadProteinEvent ) o;

        if ( uniprotReference != null ) {
            if (!uniprotReference.equals( event.getUniprotReference() )){
                return false;
            }
        }
        else if (event.getUniprotReference()!= null){
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

        if ( uniprotReference != null ) {
            code = 29 * code + uniprotReference.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final DeadProteinEvent event = ( DeadProteinEvent ) o;

        if ( uniprotReference != null ) {
            if (!uniprotReference.equals( event.getUniprotReference() )){
                return false;
            }
        }
        else if (event.getUniprotReference()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Dead event : [uniprotRef = " + uniprotReference != null ? uniprotReference : "none");
        buffer.append("] \n");

        return buffer.toString();
    }
}
