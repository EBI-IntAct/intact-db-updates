package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.UpdateEvent;

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

public interface UpdateEventDao<T extends UpdateEvent> extends UpdateBaseDao<T>, Serializable {

    List<T> getAllUpdateEventsByName(String name);
    List<T> getAllUpdateEventsByIntactAc(String intactObjectAc);
    List<T> getAllUpdateEventsByProcessId(long processId);
    List<T> getAllUpdateEventsByDate(Date updatedDate);
    List<T> getAllUpdateEventsByNameAndIntactAc(String name, String intactObjectAc);

    List<T> getUpdateEventsByNameAndProcessId(String name, long processId);
    List<T> getUpdateEventsByIntactAcAndProcessId(String intactObjectAc, long processId);
    List<T> getUpdateEventsByNameAndIntactAc(String name, String intactObjectAc, long processId);

    List<T> getUpdateEventsByNameAndDate(String name, Date updatedDate);
    List<T> getUpdateEventsByIntactAcAndDate(String intactObjectAc, Date updatedDate);
    List<T> getUpdateEventsByNameAndIntactAc(String name, String intactObjectAc, Date date);

    List<T> getUpdateEventsByNameBeforeDate(String name, Date updatedDate);
    List<T> getUpdateEventsByIntactAcBeforeDate(String intactObjectAc, Date updatedDate);
    List<T> getUpdateEventsByNameAndIntactAcBefore(String name, String intactObjectAc, Date date);

    List<T> getUpdateEventsByNameAfterDate(String name, Date updatedDate);
    List<T> getUpdateEventsByIntactAcAfterDate(String intactObjectAc, Date updatedDate);
    List<T> getUpdateEventsByNameAndIntactAcAfter(String name, String intactObjectAc, Date date);
}
