package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Super class for events having messages
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/03/11</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("ProteinEventWithMessage")
public class ProteinEventWithMessage extends PersistentProteinEvent {

    String message;

    public ProteinEventWithMessage(){
        super();
        this.message = null;

    }

    public ProteinEventWithMessage(ProteinUpdateProcess updateProcess, ProteinEventName name, Protein protein, String message ){
        super(updateProcess, name, protein);
        this.message = message;
    }

    @Column(name = "message")
    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ProteinEventWithMessage event = ( ProteinEventWithMessage ) o;

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
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

        if ( message != null ) {
            code = 29 * code + message.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProteinEventWithMessage event = ( ProteinEventWithMessage ) o;

        if ( message != null ) {
            if (!message.equals( event.getMessage())){
                return false;
            }
        }
        else if (event.getMessage()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Message : " + message != null ? message : "none");
        buffer.append(" \n");

        return buffer.toString();
    }
}
