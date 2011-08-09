package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.OutOfDateParticipantEvent;

/**
 * Dao for out of date participants events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface OutOfDateParticipantEventDao extends ProteinEventDao<OutOfDateParticipantEvent> {
}
