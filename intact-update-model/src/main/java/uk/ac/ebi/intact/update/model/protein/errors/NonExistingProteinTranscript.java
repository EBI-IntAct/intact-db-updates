package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Error for transcripts not in uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("non_existing_transcript")
public class NonExistingProteinTranscript extends DeadUniprotAc {

    /**
     * The uniprot ac of the master protein
     */
    private String masterUniprotAc;

    /**
     * The intact ac of the master protein
     */
    private String masterIntactAc;

    public NonExistingProteinTranscript(){
        super(null, UpdateError.not_matching_protein_transcript, null, null);
        this.masterIntactAc = null;
        this.masterUniprotAc = null;
    }

    public NonExistingProteinTranscript(ProteinUpdateProcess process, String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc) {
        super(process, UpdateError.not_matching_protein_transcript,proteinAc, deadTranscriptAc);

        this.masterIntactAc = masterIntactAc;
        this.masterUniprotAc = masterUniprotAc;
    }

    @Column(name = "uniprot_master")
    public String getMasterUniprotAc() {
        return masterUniprotAc;
    }

    @Column(name = "intact_master")
    public String getMasterIntactAc() {
        return masterIntactAc;
    }

    public void setMasterUniprotAc(String masterUniprotAc) {
        this.masterUniprotAc = masterUniprotAc;
    }

    public void setMasterIntactAc(String masterIntactAc) {
        this.masterIntactAc = masterIntactAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final NonExistingProteinTranscript event = (NonExistingProteinTranscript) o;

        if ( masterUniprotAc != null ) {
            if (!masterUniprotAc.equals( event.getMasterUniprotAc())){
                return false;
            }
        }
        else if (event.getMasterUniprotAc()!= null){
            return false;
        }

        if ( masterIntactAc != null ) {
            if (!masterIntactAc.equals( event.getMasterIntactAc())){
                return false;
            }
        }
        else if (event.getMasterIntactAc()!= null){
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

        if ( masterUniprotAc != null ) {
            code = 29 * code + masterUniprotAc.hashCode();
        }

        if ( masterIntactAc != null ) {
            code = 29 * code + masterIntactAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final NonExistingProteinTranscript event = (NonExistingProteinTranscript) o;

        if ( masterUniprotAc != null ) {
            if (!masterUniprotAc.equals( event.getMasterUniprotAc())){
                return false;
            }
        }
        else if (event.getMasterUniprotAc()!= null){
            return false;
        }

        if ( masterIntactAc != null ) {
            if (!masterIntactAc.equals( event.getMasterIntactAc())){
                return false;
            }
        }
        else if (event.getMasterIntactAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        if (this.proteinAc == null || this.masterIntactAc == null || this.masterUniprotAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein transcript ");
        error.append(proteinAc);
        error.append(" is attached to the updateProcess protein ");
        error.append(masterIntactAc);

        if (deadUniprot != null){
            error.append(" but refers to a uniprot ac ");
            error.append(deadUniprot);
            error.append(" which is not present in the master uniprot entry ");
            error.append(masterUniprotAc);
        }
        else {
            error.append(" and does not have a 'no-uniprot-update' and or any uniprot identity which can be found in the uniprot entry ");
            error.append(masterUniprotAc);
        }

        return error.toString();
    }
}
