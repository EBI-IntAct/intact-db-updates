package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for protein transcript which cannot be found in the uniprot entry of the parent protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class NonExistingProteinTranscript extends DeadUniprotAc {

    private String masterUniprotAc;
    private String masterIntactAc;

    public NonExistingProteinTranscript(String proteinAc, String deadTranscriptAc, String masterUniprotAc, String masterIntactAc) {
        super(UpdateError.not_matching_protein_transcript,proteinAc, deadTranscriptAc);

        this.masterIntactAc = masterIntactAc;
        this.masterUniprotAc = masterUniprotAc;
    }

    public String getMasterUniprotAc() {
        return masterUniprotAc;
    }

    public String getMasterIntactAc() {
        return masterIntactAc;
    }

    @Override
    public String getErrorMessage(){
        if (this.proteinAc == null || this.masterIntactAc == null || this.masterUniprotAc == null){
            return "";
        }

        StringBuffer error = new StringBuffer();
        error.append("The protein transcript ");
        error.append(proteinAc);
        error.append(" is attached to the parent protein ");
        error.append(masterIntactAc);

        if (deadUniprot != null){
            error.append(" but refers to a uniprot ac ");
            error.append(deadUniprot);
            error.append(" which is not present in the master uniprot entry ");
            error.append(masterUniprotAc);
        }
        else {
            error.append(" and does not have any uniprot identity which can be found in the uniprot entry ");
            error.append(masterUniprotAc);
        }

        return error.toString();
    }
}
