package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.range.UpdatedRange;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * Represents the update steps applied to proteins in IntAct
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/03/11</pre>
 */
@Entity
@Table(name = "ia_update_process")
public class UpdateProcess extends HibernatePersistentImpl{

    private Date date;

    private Collection<ProteinEvent> events;
    private Collection<UpdatedRange> rangeUpdates;

    public UpdateProcess(){

        this.events = new ArrayList<ProteinEvent>();
        this.rangeUpdates = new ArrayList<UpdatedRange>();
    }

    public UpdateProcess(Protein protein){
        setDate(new Date(System.currentTimeMillis()));

        this.events = new ArrayList<ProteinEvent>();
        this.rangeUpdates = new ArrayList<UpdatedRange>();
    }

    @OneToMany(mappedBy="parent")
    public Collection<ProteinEvent> getEvents() {
        return events;
    }

    public void setEvents(Collection<ProteinEvent> events) {
        this.events = events;
    }

    @OneToMany(mappedBy="parent")
    public Collection<UpdatedRange> getRangeUpdates() {
        return rangeUpdates;
    }

    public void setRangeUpdates(Collection<UpdatedRange> rangeUpdates) {
        this.rangeUpdates = rangeUpdates;
    }

    @Temporal( value = TemporalType.TIMESTAMP )
    @Column(name = "date", nullable = false)
    public Date getDate() {
        return date;
    }

    public void setDate(Date updateDate) {
        this.date = updateDate;
    }
}
