package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.OutOfDateParticipantEvent;
import uk.ac.ebi.intact.update.persistence.protein.OutOfDateParticipantEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for out of date participant events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class OutOfDateParticipantEventDaoImpl extends ProteinEventDaoImpl<OutOfDateParticipantEvent> implements OutOfDateParticipantEventDao {

    public OutOfDateParticipantEventDaoImpl(){
        super(OutOfDateParticipantEvent.class, null);
    }
    public OutOfDateParticipantEventDaoImpl(EntityManager entityManager) {
        super( OutOfDateParticipantEvent.class, entityManager);
    }
}
