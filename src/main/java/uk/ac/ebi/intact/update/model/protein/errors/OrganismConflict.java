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
}
