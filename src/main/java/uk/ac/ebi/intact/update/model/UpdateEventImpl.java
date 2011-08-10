package uk.ac.ebi.intact.update.model;

import javax.persistence.*;
import java.util.Date;

/**
 * Abstract class for each updateEvent
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
@MappedSuperclass
public abstract class UpdateEventImpl extends HibernateUpdatePersistentImpl implements UpdateEvent {

    /**
     * The parent process
     */
    protected UpdateProcess parent;
    protected Date eventDate;

    public UpdateEventImpl(){
        super();
        this.parent = null;
        this.eventDate = null;
    }

    public UpdateEventImpl(UpdateProcess process){
        super();
        this.parent = process;
        this.eventDate = new Date(System.currentTimeMillis());
    }

    @Transient
    public UpdateProcess getParent() {
        return this.parent;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.parent = updateProcess;
    }

    @Temporal(TemporalType.TIMESTAMP)
    @Column(name = "event_date", nullable = false)
    public Date getEventDate() {
        return eventDate;
    }

    public void setEventDate(Date eventDate) {
        this.eventDate = eventDate;
    }
}
