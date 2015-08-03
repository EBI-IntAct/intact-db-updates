package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.SequenceUpdateEvent;

import java.util.List;

/**
 * Dao for sequence update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface SequenceUpdateEventDao extends ProteinEventDao<SequenceUpdateEvent> {

    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservation(long processId, double cons);
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservationInferiorTo(long processId, double cons);
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservationSuperiorTo(long processId, double cons);
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithoutOldSequence(long processId);
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithOldSequence(long processId);
}
