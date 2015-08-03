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
     * The updateProcess process
     */
    protected UpdateProcess updateProcess;
    protected Date eventDate;

    public UpdateEventImpl(){
        super();
        this.updateProcess = null;
        this.eventDate = new Date(System.currentTimeMillis());
    }

    public UpdateEventImpl(UpdateProcess process){
        super();
        this.updateProcess = process;
        this.eventDate = new Date(System.currentTimeMillis());
    }

    @Transient
    public UpdateProcess getUpdateProcess() {
        return this.updateProcess;
    }

    public void setUpdateProcess(UpdateProcess updateProcess) {
        this.updateProcess = updateProcess;
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
