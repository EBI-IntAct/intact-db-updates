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
 * Error for uniprot protein sequence null
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("uniprot_sequence_null")
public class UniprotSequenceNull extends DefaultPersistentUpdateError  implements IntactUpdateError, UniprotUpdateError {

    /**
     * The sequence in intact
     */
    private String intactSequence;

    /**
     * The uniprot ac
     */
    private String uniprotAc;

    /**
     * The intact protein ac
     */
    private String proteinAc;

    public UniprotSequenceNull(){
        super(null, UpdateError.uniprot_sequence_null, null);
        this.intactSequence = null;
        this.uniprotAc = null;
        this.proteinAc = null;
    }

    public UniprotSequenceNull(ProteinUpdateProcess process, String proteinAc, String uniprotAc, String intactSequence) {
        super(process, UpdateError.uniprot_sequence_null, null);
        this.uniprotAc = uniprotAc;
        this.intactSequence = intactSequence;
        this.proteinAc = proteinAc;
    }

    @Lob
    @Column(name = "intact_seq")
    public String getIntactSequence() {
        return intactSequence;
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

    public void setIntactSequence(String intactSequence) {
        this.intactSequence = intactSequence;
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

        final UniprotSequenceNull event = (UniprotSequenceNull) o;

        if ( intactSequence != null ) {
            if (!intactSequence.equals( event.getIntactSequence())){
                return false;
            }
        }
        else if (event.getIntactSequence()!= null){
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

        if ( intactSequence != null ) {
            code = 29 * code + intactSequence.hashCode();
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

        final UniprotSequenceNull event = (UniprotSequenceNull) o;

        if ( intactSequence != null ) {
            if (!intactSequence.equals( event.getIntactSequence())){
                return false;
            }
        }
        else if (event.getIntactSequence()!= null){
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

        if (this.proteinAc == null || this.uniprotAc == null){
            return super.getReason();
        }

        StringBuffer error = new StringBuffer();
        error.append("The uniprot entry ");
        error.append(uniprotAc);
        error.append(" does not have a sequence ");
        error.append(" and the sequence of the protein ");
        error.append(this.proteinAc);
        error.append(" cannot be updated.");

        if (intactSequence != null){
            error.append("The sequence of the protein in Intact is not null.");
        }

        return error.toString();
    }
}
