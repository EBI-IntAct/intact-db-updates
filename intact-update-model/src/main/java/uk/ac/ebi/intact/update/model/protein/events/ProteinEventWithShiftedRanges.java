package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;

import javax.persistence.CascadeType;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.Collection;

/**
 * This class is for protein events with shifted ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25/07/11</pre>
 */
@Entity
public abstract class ProteinEventWithShiftedRanges extends ProteinEventWithRangeUpdate<PersistentUpdatedRange> {

    public ProteinEventWithShiftedRanges() {
        super();
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, Protein protein) {
        super(process, protein);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, protein, uniprotAc);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, String proteinAc) {
        super(process, proteinAc);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, proteinAc, uniprotAc);
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentUpdatedRange> getUpdatedRanges(){
        return super.getUpdatedRanges();
    }
}
