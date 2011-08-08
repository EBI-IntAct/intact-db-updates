package uk.ac.ebi.intact.update.model.protein.update.errors;

import uk.ac.ebi.intact.dbupdate.prot.errors.IntactUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;
import java.util.HashSet;
import java.util.Set;

/**
 * Error for proteins having several uniprot identities
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08/08/11</pre>
 */
@Entity
@DiscriminatorValue("multi_uniprot")
public class MultiUniprotIdentities extends DefaultPersistentUpdateError  implements IntactUpdateError{

    protected Set<String> uniprotIdentities = new HashSet<String>();
    protected String proteinAc;

    public MultiUniprotIdentities(){
         super(null, UpdateError.multi_uniprot_identities, null);
        this.proteinAc = null;
    }

    public MultiUniprotIdentities(ProteinUpdateProcess process, String proteinAc) {
        super(process, UpdateError.multi_uniprot_identities, null);
        this.proteinAc = proteinAc;
    }

    public MultiUniprotIdentities(ProteinUpdateProcess process, String proteinAc, UpdateError errorLabel) {
        super(process, errorLabel, null);
        this.proteinAc = proteinAc;
    }

    @ElementCollection
    @JoinTable(name = "ia_error2uniprot", joinColumns = @JoinColumn(name="error_id"))
    @Column(name = "uniprot_identity", nullable = false)
    public Set<String> getUniprotIdentities() {
        return uniprotIdentities;
    }

    @Override
    @Column(name = "protein_ac")
    public String getProteinAc() {
        return this.proteinAc;
    }

    public void setUniprotIdentities(Set<String> uniprotIdentities) {
        this.uniprotIdentities = uniprotIdentities;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }
}
