package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.SequenceUpdateEvent;

/**
 * Dao for sequence update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface SequenceUpdateEventDao extends ProteinEventDao<SequenceUpdateEvent> {
}
