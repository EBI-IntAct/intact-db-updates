package uk.ac.ebi.intact.update.model;

import javax.persistence.MappedSuperclass;
import javax.persistence.Transient;

/**
 * Abstract class for each updateEvent
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
@MappedSuperclass
public abstract class UpdateEventImpl extends HibernateUpdatePersistentImpl implements UpdateEvent {

    UpdateProcess parent;

    public UpdateEventImpl(){
        super();
    }

    public UpdateEventImpl(UpdateProcess process){
        super();
        this.parent = process;
    }

    @Transient
    public UpdateProcess getParent() {
        return this.parent;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.parent = updateProcess;
    }
}
