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

    private String masterUniprotAc;
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

    @Column(name = "master_ac")
    public String getMasterUniprotAc() {
        return masterUniprotAc;
    }

    @Column(name = "transcript_intact_ac")
    public String getMasterIntactAc() {
        return masterIntactAc;
    }

    public void setMasterUniprotAc(String masterUniprotAc) {
        this.masterUniprotAc = masterUniprotAc;
    }

    public void setMasterIntactAc(String masterIntactAc) {
        this.masterIntactAc = masterIntactAc;
    }
}
