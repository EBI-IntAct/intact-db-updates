package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.IntactTranscriptUpdateEvent;

import java.util.List;

/**
 * Dao for intact transcript update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
 @Mockable
public interface IntactTranscriptUpdateEventDao extends ProteinEventDao<IntactTranscriptUpdateEvent> {

    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithoutOldParent(long processId);
    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithOldParent(long processId, String oldParent);
    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithNewParent(long processId, String newParent);
}
