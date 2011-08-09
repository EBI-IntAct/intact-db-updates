package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.SequenceUpdateEvent;
import uk.ac.ebi.intact.update.persistence.protein.SequenceUpdateEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for sequence update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class SequenceUpdateEventDaoImpl extends ProteinEventDaoImpl<SequenceUpdateEvent> implements SequenceUpdateEventDao {

    public SequenceUpdateEventDaoImpl(){
        super(SequenceUpdateEvent.class, null);
    }
    public SequenceUpdateEventDaoImpl(EntityManager entityManager) {
        super( SequenceUpdateEvent.class, entityManager);
    }
}
