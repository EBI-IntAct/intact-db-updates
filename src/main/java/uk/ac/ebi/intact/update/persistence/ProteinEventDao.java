package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEvent;

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

public interface ProteinEventDao<T extends ProteinEvent> extends UpdateBaseDao<T>, Serializable {

    List<T> getAllProteinEventsByName(EventName name);
    List<T> getAllProteinEventsByProteinAc(String proteinAc);
    List<T> getAllProteinEventsByProcessId(long processId);
    List<T> getAllProteinEventsByDate(Date updatedDate);
    List<T> getAllProteinEventsByNameAndProteinAc(EventName name, String proteinAc);

    List<T> getAllProteinEventsByNameAndProcessId(EventName name, long processId);
    List<T> getAllProteinEventsByProteinAcAndProcessId(String proteinAc, long processId);
    List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, long processId);

    List<T> getAllProteinEventsByNameAndDate(EventName name, Date updatedDate);
    List<T> getAllProteinEventsByProteinAcAndDate(String proteinAc, Date updatedDate);
    List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, Date date);

    List<T> getAllProteinEventsByNameBeforeDate(EventName name, Date updatedDate);
    List<T> getAllProteinEventsByProteinAcBeforeDate(String proteinAc, Date updatedDate);
    List<T> getProteinEventsByNameAndProteinAcBefore(EventName name, String proteinAc, Date date);

    List<T> getAllProteinEventsByNameAfterDate(EventName name, Date updatedDate);
    List<T> getAllProteinEventsByProteinAcAfterDate(String proteinAc, Date updatedDate);
    List<T> getProteinEventsByNameAndProteinAcAfter(EventName name, String proteinAc, Date date);
}
