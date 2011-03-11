package uk.ac.ebi.intact.update.model.protein.update.range;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;

import javax.persistence.*;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Table( name = "ia_range_positions" )
public class RangePositions extends HibernatePersistentImpl {

    private int fromIntervalStart;
    private int fromIntervalEnd;
    private int toIntervalStart;
    private int toIntervalEnd;

    private UpdatedRange parent;

    public RangePositions(){
        super();
        fromIntervalStart = 0;
        fromIntervalEnd = 0;
        toIntervalStart = 0;
        toIntervalEnd = 0;
    }

    public RangePositions(int fromStart, int fromEnd, int toStart, int toEnd){
        super();
        fromIntervalStart = fromStart;
        fromIntervalEnd = fromEnd;
        toIntervalStart = toStart;
        toIntervalEnd = toEnd;
    }

    @Column(name = "from_start", nullable = false)
    public int getFromIntervalStart() {
        return fromIntervalStart;
    }

    public void setFromIntervalStart(int fromIntervalStart) {
        this.fromIntervalStart = fromIntervalStart;
    }

    @Column(name = "from_end", nullable = false)
    public int getFromIntervalEnd() {
        return fromIntervalEnd;
    }

    public void setFromIntervalEnd(int fromIntervalEnd) {
        this.fromIntervalEnd = fromIntervalEnd;
    }

    @Column(name = "to_start", nullable = false)
    public int getToIntervalStart() {
        return toIntervalStart;
    }

    public void setToIntervalStart(int toIntervalStart) {
        this.toIntervalStart = toIntervalStart;
    }

    @Column(name = "to_end", nullable = false)
    public int getToIntervalEnd() {
        return toIntervalEnd;
    }

    public void setToIntervalEnd(int toIntervalEnd) {
        this.toIntervalEnd = toIntervalEnd;
    }

    @ManyToOne
    @JoinColumn( name = "updated_range_ac" )
    public UpdatedRange getParent() {
        return parent;
    }

    public void setParent(UpdatedRange parent) {
        this.parent = parent;
    }
}
