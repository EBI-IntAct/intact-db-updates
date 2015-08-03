package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.events.SequenceIdenticalToTranscriptEvent;

import java.util.List;

/**
 * Dao for proteins having sequence identical to one of the transcripts
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Mockable
public interface SequenceIdenticalToTranscriptEventDao extends ProteinEventDao<SequenceIdenticalToTranscriptEvent> {

    public List<SequenceIdenticalToTranscriptEvent> getByUniprotTranscriptAc(long processId, String transcript);
}
