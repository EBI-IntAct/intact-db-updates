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

    List<T> getAllUpdateEventsByName(String name);
    List<T> getAllUpdateEventsByProcessId(long processId);
    List<T> getAllUpdateEventsByDate(Date updatedDate);

    List<T> getUpdateEventsByNameAndProcessId(String name, long processId);

    List<T> getUpdateEventsByNameAndDate(String name, Date updatedDate);

    List<T> getUpdateEventsByNameBeforeDate(String name, Date updatedDate);

    List<T> getUpdateEventsByNameAfterDate(String name, Date updatedDate);
}
