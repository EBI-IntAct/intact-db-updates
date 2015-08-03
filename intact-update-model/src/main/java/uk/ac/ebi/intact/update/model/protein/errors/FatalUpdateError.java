package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UniprotUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.Lob;

/**
 * This class is for fatal errors while updating a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/08/11</pre>
 */
@Entity
@DiscriminatorValue("fatal_error")
public class FatalUpdateError extends DefaultPersistentUpdateError implements IntactUpdateError, UniprotUpdateError{

    /**
     * The uniprot ac
     */
    private String proteinAc;

    /**
     * The protein ac
     */
    private String uniprotAc;

    /**
     * The exception in details
     */
    private String exception;

    public FatalUpdateError() {
        super(null, UpdateError.fatal_error_during_update, null);
        this.proteinAc = null;
        this.uniprotAc = null;
        this.exception = null;
    }

    public FatalUpdateError(ProteinUpdateProcess process, String proteinAc, String uniprotAc, String exception) {
        super(process, UpdateError.fatal_error_during_update, null);
        this.proteinAc = proteinAc;
        this.uniprotAc = uniprotAc;
        this.exception = exception;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    @Override
    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return this.uniprotAc;
    }

    @Lob
    @Column(name = "exception")
    public String getException() {
        return exception;
    }

    public void setException(String exception) {
        this.exception = exception;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    @Override
    public String toString(){
        if ((this.proteinAc == null && this.uniprotAc == null) || this.reason == null){
            return super.getReason();
        }

        StringBuffer error = new StringBuffer();
        if (proteinAc != null){
            error.append("Impossible to update the protein ");
            error.append(proteinAc);
        }
        else{
            error.append("Impossible to update the proteins ");
        }

        if (uniprotAc != null){
            error.append("attached to the uniprot entry ");
            error.append(uniprotAc);
        }

        error.append(" because an Exception was thrown : ");

        error.append(this.exception);

        return error.toString();
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final FatalUpdateError event = (FatalUpdateError) o;

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
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

        if ( exception != null ) {
            if (!exception.equals( event.getException())){
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

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
        }

        if ( exception != null ) {
            code = 29 * code + exception.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final FatalUpdateError event = (FatalUpdateError) o;

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
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

        if ( exception != null ) {
            if (!exception.equals( event.getException())){
                return false;
            }
        }
        else if (event.getUniprotAc()!= null){
            return false;
        }

        return true;
    }
}
