package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

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
@DiscriminatorFormula("objclass")
@DiscriminatorValue("ProcessErrorEvent")
public class ProcessErrorEvent extends ProteinEventWithMessage{

    private String type;

    private String uniprotAc;

    public ProcessErrorEvent(){
        super();
        this.type = null;
        this.message = null;
        this.uniprotAc = null;
    }

    public ProcessErrorEvent(ProteinUpdateProcess updateProcess, Protein protein, String type, String message, String uniprotAc ){
        super(updateProcess, ProteinEventName.update_error, protein, message);
        this.type = type;
        this.uniprotAc = uniprotAc;
    }

    @Column(name = "error_type")
    public String getType() {
        return type;
    }

    public void setType(String type) {
        this.type = type;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
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

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
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

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
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

        if ( uniprotAc != null ) {
            if (!uniprotAc.equals( event.getUniprotAc())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Process error event : [uniprot ac = " + uniprotAc != null ? uniprotAc : "none");
        buffer.append("error type = "+type != null ? type : "none"+"] \n");

        return buffer.toString();
    }
}
