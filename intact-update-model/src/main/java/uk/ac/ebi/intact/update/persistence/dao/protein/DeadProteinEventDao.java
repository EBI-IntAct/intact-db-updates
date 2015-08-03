package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.DeadProteinEvent;

import java.util.List;

/**
 * Dao for dead proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface DeadProteinEventDao extends ProteinEventDao<DeadProteinEvent> {

    public List<DeadProteinEvent> getAllDeadProteinEventsHavingDeletedXrefs(long processId);
    public List<DeadProteinEvent> getAllDeadProteinEventsWithoutDeletedXrefs(long processId);
}
