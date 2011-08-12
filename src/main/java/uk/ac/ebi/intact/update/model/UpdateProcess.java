package uk.ac.ebi.intact.update.model;

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
}
