package uk.ac.ebi.intact.update.model;

import java.util.Date;

/**
 * Interface for update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public interface UpdateEvent extends HibernateUpdatePersistent{

    /**
     *
     * @return the updateProcess process
     */
    public UpdateProcess getUpdateProcess();

    /**
     *
     * @return the date when the event started
     */
    public Date getEventDate();
}
