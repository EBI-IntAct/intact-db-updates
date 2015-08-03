package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.OutOfDateParticipantEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.ProteinEventDao;

import java.util.List;

/**
 * Dao for out of date participants events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface OutOfDateParticipantEventDao extends ProteinEventDao<OutOfDateParticipantEvent> {

    public List<OutOfDateParticipantEvent> getOutOfDateEventImpossibleToFix(long processId);
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossibleToFix(long processId);
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossiblePerRemappedProtein(long processId, String remappedProtein);
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossiblePerRemappedParent(long processId, String remappedParent);
}
