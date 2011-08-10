package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.UpdateEventDao;

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
@Mockable
public interface ProteinEventDao<T extends PersistentProteinEvent> extends UpdateEventDao<T>, Serializable {

    List<T> getAllUpdateEventsByProteinAc(String intactObjectAc);

    List<T> getUpdateEventsByProteinAcAndProcessId(String intactObjectAc, long processId);

    List<T> getUpdateEventsByProteinAcAndDate(String intactObjectAc, Date updatedDate);

    List<T> getUpdateEventsByProteinAcBeforeDate(String intactObjectAc, Date updatedDate);

    List<T> getUpdateEventsByProteinAcAfterDate(String intactObjectAc, Date updatedDate);
}
