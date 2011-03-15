package uk.ac.ebi.intact.update.model.protein.update.events.range;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.*;

/**
 * Represents an update of feature ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objClass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("UpdatedRange")
@Table(name = "ia_updated_range")
public class UpdatedRange extends HibernatePersistentImpl {
    private RangePositions oldPositions;
    private RangePositions newPositions;
    private String rangeAc;

    private String protein;
    UpdateProcess updateProcess;

    public UpdatedRange (){
        super();
        oldPositions = null;
        newPositions = null;
        rangeAc = null;
        this.protein = null;
    }

    public UpdatedRange(UpdateProcess updateProcess, Protein parent, String rangeAc, int oldFromStart, int oldFromEnd, int oldToStart, int oldToEnd, int newFromStart, int newFromEnd, int newToStart, int newToEnd){
        super();
        this.protein = parent != null ? parent.getAc() : null;
        this.rangeAc = rangeAc;
        this.oldPositions = new RangePositions(oldFromStart, oldFromEnd, oldToStart, oldToEnd);
        this.newPositions = new RangePositions(newFromStart, newFromEnd, newToStart, newToEnd);
        this.updateProcess = updateProcess;
    }

    @Column(name="protein_ac", nullable=false)
    public String getProtein() {
        return protein;
    }

    public void setProtein(String protein) {
        this.protein = protein;
    }

    @ManyToOne
    @JoinColumn(name="parent_id", nullable=false)
    public UpdateProcess getParent() {
        return this.updateProcess;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.updateProcess = updateProcess;
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
