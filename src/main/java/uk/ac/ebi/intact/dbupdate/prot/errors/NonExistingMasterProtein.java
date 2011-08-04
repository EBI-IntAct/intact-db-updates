package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Errors for dead master proteins having protein transcript which exist in uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class NonExistingMasterProtein extends DeadUniprotAc {

    private String transcriptUniprotAc;
    private String masterIntactAc;

    public NonExistingMasterProtein(String proteinAc, String deadMasterAc, String transcriptUniprotAc, String masterIntactAc) {
        super(UpdateError.dead_protein_with_transcripts_not_dead, proteinAc, deadMasterAc);

        this.masterIntactAc = masterIntactAc;
        this.transcriptUniprotAc = transcriptUniprotAc;
    }

    public String getTranscriptUniprotAc() {
        return transcriptUniprotAc;
    }

    public String getMasterIntactAc() {
        return masterIntactAc;
    }

    @Override
    public String getErrorMessage(){
        if (this.deadUniprot == null || this.proteinAc == null || this.masterIntactAc == null || this.transcriptUniprotAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein transcript ");
        error.append(proteinAc);
        error.append(" refers to a valid uniprot entry ");
        error.append(transcriptUniprotAc);
        error.append(" but is attached to a parent protein ");
        error.append(masterIntactAc);
        error.append(" which is refers to an obsolete uniprot entry ");
        error.append(deadUniprot);

        return error.toString();
    }
}
