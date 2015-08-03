package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Errors for isoforms and feature chains referring to an invalid updateProcess
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("invalid_parent")
public class InvalidParentXref extends DefaultPersistentUpdateError implements IntactUpdateError {

    /**
     * the invalid updateProcess ac
     */
    private String invalidParent;

    /**
     * the intact protein ac of the transcript
     */
    private String proteinAc;

    public InvalidParentXref(){
        super(null, UpdateError.invalid_parent_xref, null);
        this.invalidParent = null;
        this.proteinAc = null;
    }

    public InvalidParentXref(ProteinUpdateProcess process, String proteinAc, String invalidParent, String reason) {
        super(process, UpdateError.invalid_parent_xref,reason);
        this.invalidParent = invalidParent;
        this.proteinAc = proteinAc;
    }

    @Column(name = "invalid_parent")
    public String getInvalidParent() {
        return invalidParent;
    }

    @Override
    @Column (name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    public void setInvalidParent(String invalidParent) {
        this.invalidParent = invalidParent;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final InvalidParentXref event = (InvalidParentXref) o;

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        if ( invalidParent != null ) {
            if (!invalidParent.equals( event.getInvalidParent())){
                return false;
            }
        }
        else if (event.getInvalidParent()!= null){
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

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        if ( invalidParent != null ) {
            code = 29 * code + invalidParent.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final InvalidParentXref event = (InvalidParentXref) o;

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        if ( invalidParent != null ) {
            if (!invalidParent.equals( event.getInvalidParent())){
                return false;
            }
        }
        else if (event.getInvalidParent()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        if (this.invalidParent.isEmpty() || this.proteinAc == null){
            return this.reason;
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" has a updateProcess cross reference to the protein ");
        error.append(invalidParent);
        error.append(" which is not valid because ");
        error.append(this.reason);

        return error.toString();
    }

}
