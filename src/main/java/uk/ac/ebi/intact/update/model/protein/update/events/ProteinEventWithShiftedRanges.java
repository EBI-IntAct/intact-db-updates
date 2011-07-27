package uk.ac.ebi.intact.update.model.protein.update.events;

import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;

import javax.persistence.*;
import java.util.Collection;

/**
 * This class is for protein events with shifted ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25/07/11</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("ProteinEventWithShiftedRanges")
public abstract class ProteinEventWithShiftedRanges extends ProteinEventWithRangeUpdate<PersistentUpdatedRange> {

    public ProteinEventWithShiftedRanges() {
        super();
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, ProteinEventName name, Protein protein) {
        super(process, name, protein);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, ProteinEventName name, Protein protein, String uniprotAc) {
        super(process, name, protein, uniprotAc);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, ProteinEventName name, String proteinAc) {
        super(process, name, proteinAc);
    }

    public ProteinEventWithShiftedRanges(ProteinUpdateProcess process, ProteinEventName name, String proteinAc, String uniprotAc) {
        super(process, name, proteinAc, uniprotAc);
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentUpdatedRange> getUpdatedRanges(){
        return super.getUpdatedRanges();
    }
}
