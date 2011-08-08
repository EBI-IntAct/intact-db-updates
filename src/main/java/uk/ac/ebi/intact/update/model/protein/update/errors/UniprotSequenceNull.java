package uk.ac.ebi.intact.update.model.protein.update.errors;

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


    private String intactSequence;
    private String uniprotAc;
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
    @Column(name = "intact_sequence")
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
}
