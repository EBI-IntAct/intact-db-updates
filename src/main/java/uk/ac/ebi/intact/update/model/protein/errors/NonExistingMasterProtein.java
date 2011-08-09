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

    private String transcriptUniprotAc;
    private String masterIntactAc;

    public NonExistingMasterProtein(){
        super(null, UpdateError.dead_protein_with_transcripts_not_dead, null, null);
        this.transcriptUniprotAc = null;
        this.masterIntactAc = null;
    }

    public NonExistingMasterProtein(ProteinUpdateProcess process, String proteinAc, String deadMasterAc, String transcriptUniprotAc, String masterIntactAc) {
        super(process, UpdateError.dead_protein_with_transcripts_not_dead, proteinAc, deadMasterAc);

        this.masterIntactAc = masterIntactAc;
        this.transcriptUniprotAc = transcriptUniprotAc;
    }

    @Column(name = "transcript_ac")
    public String getTranscriptUniprotAc() {
        return transcriptUniprotAc;
    }

    @Column(name = "master_intact_ac")
    public String getMasterIntactAc() {
        return masterIntactAc;
    }

    public void setTranscriptUniprotAc(String transcriptUniprotAc) {
        this.transcriptUniprotAc = transcriptUniprotAc;
    }

    public void setMasterIntactAc(String masterIntactAc) {
        this.masterIntactAc = masterIntactAc;
    }
}
