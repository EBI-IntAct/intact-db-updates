package uk.ac.ebi.intact.update.model;

import java.util.Collection;
import java.util.Date;

/**
 * The interface for all update processes
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/07/11</pre>
 */
public interface UpdateProcess<T extends UpdateEventImpl> extends HibernateUpdatePersistent {

    /**
     *
     * @return the date when the process started
     */
    public Date getDate();

    /**
     *
     * @return the user which runs the update
     */
    public String getUserStamp();

    /**
     *
     * @return the name of the update process
     */
    public UpdateProcessName getUpdateName();

    /**
     *
     * @return the collection of update events which occurred during the update process
     */
    public Collection<T> getUpdateEvents();
}
