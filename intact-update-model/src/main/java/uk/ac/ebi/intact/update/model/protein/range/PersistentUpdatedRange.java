package uk.ac.ebi.intact.update.model.protein.range;

import uk.ac.ebi.intact.update.model.protein.events.ProteinEventWithRangeUpdate;
import uk.ac.ebi.intact.update.model.protein.events.ProteinEventWithShiftedRanges;

import javax.persistence.Entity;
import javax.persistence.JoinColumn;
import javax.persistence.ManyToOne;
import javax.persistence.Table;

/**
 * Represents an update of feature ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Table(name = "ia_updated_range")
public class PersistentUpdatedRange extends AbstractUpdatedRange {

    public PersistentUpdatedRange(){
        super();
    }

    public PersistentUpdatedRange(ProteinEventWithRangeUpdate proteinEvent, String componentAc, String featureAc, String interactionAc, String rangeAc, String oldSequence, String newSequence, String oldRangePositions, String newRangePositions){
        super(proteinEvent, componentAc, featureAc, interactionAc, rangeAc, oldSequence, newSequence, oldRangePositions, newRangePositions);
    }

    @ManyToOne(targetEntity = ProteinEventWithShiftedRanges.class)
    @JoinColumn(name="parent_id")
    public ProteinEventWithRangeUpdate getParent() {
        return super.getParent();
    }

    public void setParent(ProteinEventWithRangeUpdate proteinEvent) {
        super.setParent(proteinEvent);
    }
}
