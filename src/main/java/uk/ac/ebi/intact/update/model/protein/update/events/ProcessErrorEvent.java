package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for errors during proteinAc update process
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */
@Entity
@DiscriminatorValue("ProcessErrorEvent")
public class ProcessErrorEvent extends PersistentProteinEvent{

    private String type;

    public ProcessErrorEvent(){
        super();
        this.type = null;
    }

    public ProcessErrorEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprotAc, String type, String message ){
        super(updateProcess, protein, uniprotAc);
        this.type = type;
        setMessage(message);
    }

    @Column(name = "error_type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final ProcessErrorEvent event = ( ProcessErrorEvent ) o;

        if ( type != null ) {
            if (!type.equals( event.getType())){
                return false;
            }
        }
        else if (event.getType()!= null){
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

        if ( type != null ) {
            code = 29 * code + type.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProcessErrorEvent event = ( ProcessErrorEvent ) o;

        if ( type != null ) {
            if (!type.equals( event.getType())){
                return false;
            }
        }
        else if (event.getType()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Process error event : [type= " + (type != null ? type : "none") + "]");

        return buffer.toString();
    }
}
