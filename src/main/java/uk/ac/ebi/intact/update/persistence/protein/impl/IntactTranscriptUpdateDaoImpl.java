package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.IntactTranscriptUpdateEvent;
import uk.ac.ebi.intact.update.persistence.protein.IntactTranscriptUpdateEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for intact transcript update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class IntactTranscriptUpdateDaoImpl extends ProteinEventDaoImpl<IntactTranscriptUpdateEvent> implements IntactTranscriptUpdateEventDao {

    public IntactTranscriptUpdateDaoImpl(){
        super(IntactTranscriptUpdateEvent.class, null);
    }
    public IntactTranscriptUpdateDaoImpl(EntityManager entityManager) {
        super( IntactTranscriptUpdateEvent.class, entityManager);
    }
}
