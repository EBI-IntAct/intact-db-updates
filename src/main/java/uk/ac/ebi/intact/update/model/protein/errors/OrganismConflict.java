package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UniprotUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Errors for organism conflicts with uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("invalid_parent")
public class OrganismConflict extends DefaultPersistentUpdateError  implements IntactUpdateError, UniprotUpdateError {

    private String wrongTaxId;
    private String uniprotTaxId;
    private String uniprotAc;
    private String proteinAc;

    public OrganismConflict(){
        super(null, UpdateError.organism_conflict_with_uniprot_protein, null);
        this.wrongTaxId = null;
        this.uniprotTaxId = null;
        this.uniprotAc = null;
        this.proteinAc = null;
    }

    public OrganismConflict(ProteinUpdateProcess process, String proteinAc, String wrongTaxId, String uniprotTaxId, String uniprotAc) {
        super(process, UpdateError.organism_conflict_with_uniprot_protein, null);
        this.wrongTaxId = wrongTaxId;
        this.uniprotTaxId = uniprotTaxId;
        this.uniprotAc = uniprotAc;
        this.proteinAc = proteinAc;
    }

    @Column(name = "invalid_taxid")
    public String getWrongTaxId() {
        return wrongTaxId;
    }

    @Column(name = "uniprot_taxid")
    public String getUniprotTaxId() {
        return uniprotTaxId;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    public void setWrongTaxId(String wrongTaxId) {
        this.wrongTaxId = wrongTaxId;
    }

    public void setUniprotTaxId(String uniprotTaxId) {
        this.uniprotTaxId = uniprotTaxId;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final OrganismConflict event = (OrganismConflict) o;

        if ( wrongTaxId != null ) {
            if (!wrongTaxId.equals( event.getWrongTaxId())){
                return false;
            }
        }
        else if (event.getWrongTaxId()!= null){
            return false;
        }

        if ( uniprotTaxId != null ) {
            if (!uniprotTaxId.equals( event.getUniprotTaxId())){
                return false;
            }
        }
        else if (event.getUniprotTaxId()!= null){
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

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
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

        if ( wrongTaxId != null ) {
            code = 29 * code + wrongTaxId.hashCode();
        }

        if ( uniprotTaxId != null ) {
            code = 29 * code + uniprotTaxId.hashCode();
        }

        if ( uniprotAc != null ) {
            code = 29 * code + uniprotAc.hashCode();
        }

        if ( proteinAc != null ) {
            code = 29 * code + proteinAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final OrganismConflict event = (OrganismConflict) o;

        if ( wrongTaxId != null ) {
            if (!wrongTaxId.equals( event.getWrongTaxId())){
                return false;
            }
        }
        else if (event.getWrongTaxId()!= null){
            return false;
        }

        if ( uniprotTaxId != null ) {
            if (!uniprotTaxId.equals( event.getUniprotTaxId())){
                return false;
            }
        }
        else if (event.getUniprotTaxId()!= null){
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

        if ( proteinAc != null ) {
            if (!proteinAc.equals( event.getProteinAc())){
                return false;
            }
        }
        else if (event.getProteinAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        if (this.proteinAc == null || this.wrongTaxId == null || this.uniprotTaxId == null || this.uniprotAc == null){
            return super.getErrorMessage();
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein ");
        error.append(proteinAc);
        error.append(" refers to taxId ");
        error.append(wrongTaxId);
        error.append(" but is associated with uniprot entry ");
        error.append(this.uniprotAc);
        error.append(" which refers to a different taxId ") ;
        error.append(this.uniprotTaxId);;

        return error.toString();
    }
}
