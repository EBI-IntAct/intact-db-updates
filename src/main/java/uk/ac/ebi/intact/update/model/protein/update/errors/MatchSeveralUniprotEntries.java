package uk.ac.ebi.intact.update.model.protein.update.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.UniprotUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Error for proteins matching different uniprot entries
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("match_several_uniprot")
public class MatchSeveralUniprotEntries extends MultiUniprotIdentities implements UniprotUpdateError {

    protected String uniprotAc;
    protected String taxId;
    protected Set<String> uniprotFromDifferentOrganisms = new HashSet<String>();

    public MatchSeveralUniprotEntries(){
        super();
        this.uniprotAc = null;
        this.taxId = null;
    }

    public MatchSeveralUniprotEntries(ProteinUpdateProcess process, String proteinAc, String uniprotAc, String taxId, UpdateError errorLabel) {
        super(process, proteinAc, errorLabel);
        this.uniprotAc = uniprotAc;
        this.taxId = taxId;
    }

    @Column(name = "uniprot_ac")
    public String getUniprotAc() {
        return uniprotAc;
    }

    @Column(name = "taxId")
    public String getTaxId() {
        return taxId;
    }

    @ElementCollection
    @JoinTable(name = "ia_error2uniprot_diff_organism", joinColumns = @JoinColumn(name="error_id"))
    @Column(name = "uniprot_identity", nullable = false)
    public Set<String> getUniprotFromDifferentOrganisms() {
        return uniprotFromDifferentOrganisms;
    }

    public void setUniprotAc(String uniprotAc) {
        this.uniprotAc = uniprotAc;
    }

    public void setTaxId(String taxId) {
        this.taxId = taxId;
    }

    public void setUniprotFromDifferentOrganisms(Set<String> uniprotFromDifferentOrganisms) {
        this.uniprotFromDifferentOrganisms = uniprotFromDifferentOrganisms;
    }
}
