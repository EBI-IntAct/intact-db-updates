package uk.ac.ebi.intact.update.persistence.dao;

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

    List<T> getByProcessId(long processId);
    List<T> getByDate(Date updatedDate);

    List<T> getBeforeDate(Date updatedDate);

    List<T> getAfterDate(Date updatedDate);
}
