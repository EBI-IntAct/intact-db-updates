package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.UpdateEventImpl;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get protein events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface UpdateEventDao<T extends UpdateEventImpl> extends UpdateBaseDao<T>, Serializable {

    List<T> getAllUpdateEventsByProcessId(long processId);
    List<T> getAllUpdateEventsByDate(Date updatedDate);

    List<T> getUpdateEventsBeforeDate(Date updatedDate);

    List<T> getUpdateEventsAfterDate(Date updatedDate);
}
