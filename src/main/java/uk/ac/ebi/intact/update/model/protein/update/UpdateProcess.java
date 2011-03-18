package uk.ac.ebi.intact.update.model.protein.update;

import org.apache.commons.collections.CollectionUtils;
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
        setDate(new Date(System.currentTimeMillis()));

        this.events = new ArrayList<ProteinEvent>();
        this.rangeUpdates = new ArrayList<UpdatedRange>();
    }

    public UpdateProcess(Date date){
        setDate(date);

        this.events = new ArrayList<ProteinEvent>();
        this.rangeUpdates = new ArrayList<UpdatedRange>();
    }

    @OneToMany(mappedBy="parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<ProteinEvent> getEvents() {
        return events;
    }

    public void setEvents(Collection<ProteinEvent> events) {
        this.events = events;
    }

    public void addEvent(ProteinEvent event){
        event.setParent(this);
        events.add(event);
    }

    @OneToMany(mappedBy="parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedRange> getRangeUpdates() {
        return rangeUpdates;
    }

    public void setRangeUpdates(Collection<UpdatedRange> rangeUpdates) {
        this.rangeUpdates = rangeUpdates;
    }

    public void addRangeUpdate(UpdatedRange up){
        up.setParent(this);
        rangeUpdates.add(up);
    }

    @Temporal( value = TemporalType.TIMESTAMP )
    @Column(name = "date", nullable = false)
    public Date getDate() {
        return date;
    }

    public void setDate(Date updateDate) {
        this.date = updateDate;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final UpdateProcess process = ( UpdateProcess ) o;

        if ( date != null ) {
            if (!date.equals( process.getDate())){
                return false;
            }
        }
        else if (process.getDate()!= null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( date != null ) {
            code = 29 * code + date.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final UpdateProcess process = ( UpdateProcess ) o;

        if ( date != null ) {
            if (!date.equals( process.getDate())){
                return false;
            }
        }
        else if (process.getDate()!= null){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(events, process.getEvents())){
            return false;
        }

        return CollectionUtils.isEqualCollection(rangeUpdates, process.getRangeUpdates());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Update process : [ date = " + date != null ? date.toString() : "none" + "] \n");

        if (events.isEmpty()){
            buffer.append("Events : \n");

            for (ProteinEvent ev : events){
                buffer.append(ev.toString() + "\n");
            }
        }

        if (rangeUpdates.isEmpty()){
            buffer.append("Range updates : \n");

            for (UpdatedRange ev : rangeUpdates){
                buffer.append(ev.toString() + "\n");
            }
        }

        return buffer.toString();
    }
}
