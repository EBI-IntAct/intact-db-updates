package ebi.ac.uk.intact.update.model.range;

import ebi.ac.uk.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="ogjClass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("UpdateRange")
@Table(name = "ia_updated_range")
public class UpdatedRange extends HibernatePersistentImpl {
    private RangePositions oldPositions;
    private RangePositions newPositions;
    private String rangeAc;

    public UpdatedRange (){
        super();
        oldPositions = null;
        newPositions = null;
        rangeAc = null;
    }

    public UpdatedRange(String rangeAc, int oldFromStart, int oldFromEnd, int oldToStart, int oldToEnd, int newFromStart, int newFromEnd, int newToStart, int newToEnd){
        super();
        this.rangeAc = rangeAc;
        this.oldPositions = new RangePositions(oldFromStart, oldFromEnd, oldToStart, oldToEnd);
        this.newPositions = new RangePositions(newFromStart, newFromEnd, newToStart, newToEnd);
        setCreated(new Date(System.currentTimeMillis()));
    }

    @OneToOne
    @JoinColumn( name = "old_positions_ac", nullable = false)
    public RangePositions getOldPositions() {
        return oldPositions;
    }

    public void setOldPositions(RangePositions oldPositions) {
        this.oldPositions = oldPositions;
    }

    @OneToOne
    @JoinColumn( name = "new_positions_ac", nullable = true)
    public RangePositions getNewPositions() {
        return newPositions;
    }

    public void setNewPositions(RangePositions newPositions) {
        this.newPositions = newPositions;
    }

    @Column(name = "range_ac", nullable = false)
    public String getRangeAc() {
        return rangeAc;
    }

    public void setRangeAc(String rangeAc) {
        this.rangeAc = rangeAc;
    }
}
