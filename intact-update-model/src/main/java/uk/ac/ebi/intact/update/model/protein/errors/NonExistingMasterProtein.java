package uk.ac.ebi.intact.update.model.protein.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Error for master protein which are not in uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("non_existing_master")
public class NonExistingMasterProtein extends DeadUniprotAc {

    /**
     * The uniprot ac of the protein transcript
     */
    private String transcriptUniprotAc;

    /**
     * The intact ac of the protein transcript
     */
    private String transcriptIntactAc;

    public NonExistingMasterProtein(){
        super(null, UpdateError.dead_protein_with_transcripts_not_dead, null, null);
        this.transcriptUniprotAc = null;
        this.transcriptIntactAc = null;
    }

    public NonExistingMasterProtein(ProteinUpdateProcess process, String proteinAc, String deadMasterAc, String transcriptUniprotAc, String transcriptIntactAc) {
        super(process, UpdateError.dead_protein_with_transcripts_not_dead, proteinAc, deadMasterAc);

        this.transcriptIntactAc = transcriptIntactAc;
        this.transcriptUniprotAc = transcriptUniprotAc;
    }

    @Column(name = "uniprot_transcript")
    public String getTranscriptUniprotAc() {
        return transcriptUniprotAc;
    }

    @Column(name = "intact_transcript")
    public String getTranscriptIntactAc() {
        return transcriptIntactAc;
    }

    public void setTranscriptUniprotAc(String transcriptUniprotAc) {
        this.transcriptUniprotAc = transcriptUniprotAc;
    }

    public void setTranscriptIntactAc(String transcriptIntactAc) {
        this.transcriptIntactAc = transcriptIntactAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final NonExistingMasterProtein event = (NonExistingMasterProtein) o;

        if ( transcriptUniprotAc != null ) {
            if (!transcriptUniprotAc.equals( event.getTranscriptUniprotAc())){
                return false;
            }
        }
        else if (event.getTranscriptUniprotAc()!= null){
            return false;
        }

        if ( transcriptIntactAc != null ) {
            if (!transcriptIntactAc.equals( event.getTranscriptIntactAc())){
                return false;
            }
        }
        else if (event.getTranscriptIntactAc()!= null){
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

        if ( transcriptUniprotAc != null ) {
            code = 29 * code + transcriptUniprotAc.hashCode();
        }

        if ( transcriptIntactAc != null ) {
            code = 29 * code + transcriptIntactAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final NonExistingMasterProtein event = (NonExistingMasterProtein) o;

        if ( transcriptUniprotAc != null ) {
            if (!transcriptUniprotAc.equals( event.getTranscriptUniprotAc())){
                return false;
            }
        }
        else if (event.getTranscriptUniprotAc()!= null){
            return false;
        }

        if ( transcriptIntactAc != null ) {
            if (!transcriptIntactAc.equals( event.getTranscriptIntactAc())){
                return false;
            }
        }
        else if (event.getTranscriptIntactAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {

        if (this.deadUniprot == null || this.proteinAc == null || this.transcriptIntactAc == null || this.transcriptUniprotAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein transcript ");
        error.append(proteinAc);
        error.append(" refers to a valid uniprot entry ");
        error.append(transcriptUniprotAc);
        error.append(" but is attached to a updateProcess protein ");
        error.append(transcriptIntactAc);
        error.append(" which is refers to an obsolete uniprot entry ");
        error.append(deadUniprot);

        return error.toString();
    }
}
