package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.SequenceIdenticalToTranscriptEvent;
import uk.ac.ebi.intact.update.persistence.protein.SequenceIdenticalToTranscriptEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for sequence identical to protein transcript events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class SequenceIdenticalToTranscriptEventDaoImpl extends ProteinEventDaoImpl<SequenceIdenticalToTranscriptEvent> implements SequenceIdenticalToTranscriptEventDao {

    public SequenceIdenticalToTranscriptEventDaoImpl(){
        super(SequenceIdenticalToTranscriptEvent.class, null);
    }
    public SequenceIdenticalToTranscriptEventDaoImpl(EntityManager entityManager) {
        super( SequenceIdenticalToTranscriptEvent.class, entityManager);
    }
}
