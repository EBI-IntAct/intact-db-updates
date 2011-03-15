package uk.ac.ebi.intact.update.model.protein.update.events.range;

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
    private String oldSequence;
    private String newSequence;

    private String rangeAc;

    private String component;
    UpdateProcess updateProcess;

    public UpdatedRange (){
        super();
        oldPositions = null;
        newPositions = null;
        rangeAc = null;
        this.component = null;
        this.oldSequence = null;
        this.newSequence = null;
    }

    public UpdatedRange(UpdateProcess updateProcess, String componentAc, String rangeAc, String oldSequence, String newSequence, RangePositions oldRangePositions, RangePositions newRangePositions){
        super();
        this.component = componentAc;
        this.rangeAc = rangeAc;
        this.oldPositions = oldRangePositions;
        this.newPositions = newRangePositions;
        this.updateProcess = updateProcess;
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
    }

    @Column(name="component_ac", nullable=false)
    public String getComponent() {
        return component;
    }

    public void setComponent(String component) {
        this.component = component;
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

    @Lob
    @Column(name = "old_sequence", nullable = true)
    public String getOldSequence() {
        return oldSequence;
    }

    public void setOldSequence(String currentSequence) {
        this.oldSequence = currentSequence;
    }

    @Lob
    @Column(name = "new_sequence", nullable = true)
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String updatedSequence) {
        this.newSequence = updatedSequence;
    }

}
