package ebi.ac.uk.intact.update.model.proteinduplicates;

import uk.ac.ebi.intact.model.Feature;

import javax.persistence.CascadeType;
import javax.persistence.Column;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */

public class MergedProtein {

    private String proteinAc;
    private Collection<DuplicatedProtein> duplicatedProteins = new ArrayList<DuplicatedProtein>();
    private String uniprotId;

    @Column(name = "protein_ac", nullable = false)
    public String getProteinAc() {
        return proteinAc;
    }

    public void setProteinAc(String proteinAc) {
        this.proteinAc = proteinAc;
    }

    @OneToMany( mappedBy = "originalProtein", cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE})
    public Collection<DuplicatedProtein> getDuplicatedProteins() {
        return duplicatedProteins;
    }

    public void setDuplicatedProteins(Collection<DuplicatedProtein> duplicatedProteins) {
        this.duplicatedProteins = duplicatedProteins;
    }

    @Column(name = "uniprot_ac", nullable = false)
    public String getUniprotId() {
        return uniprotId;
    }

    public void setUniprotId(String uniprotId) {
        this.uniprotId = uniprotId;
    }
}
