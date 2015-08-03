package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.DuplicatedProteinEvent;

import java.util.List;

/**
 * Dao for duplicated proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface DuplicatedProteinEventDao extends ProteinEventDao<DuplicatedProteinEvent> {

    public List<DuplicatedProteinEvent> getDuplicatedEventByOriginalProtein(long processId, String originalProtein);
    public List<DuplicatedProteinEvent> getDuplicatedEventWithMergeSuccessful(long processId);
    public List<DuplicatedProteinEvent> getDuplicatedEventWithMergeNotSuccessful(long processId);
    public List<DuplicatedProteinEvent> getDuplicatedEventWithSequenceUpdate(long processId);
    public List<DuplicatedProteinEvent> getDuplicatedEventWithoutSequenceUpdate(long processId);
}
