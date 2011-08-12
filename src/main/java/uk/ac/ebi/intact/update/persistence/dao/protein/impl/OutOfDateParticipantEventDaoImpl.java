package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.OutOfDateParticipantEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.OutOfDateParticipantEventDao;

import javax.persistence.EntityManager;
import java.util.List;

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

    @Override
    public List<OutOfDateParticipantEvent> getOutOfDateEventImpossibleToFix(long processId) {
        return getSession().createCriteria(OutOfDateParticipantEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNull("remappedProtein"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossibleToFix(long processId) {
        return getSession().createCriteria(OutOfDateParticipantEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotNull("remappedProtein"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossiblePerRemappedProtein(long processId, String remappedProtein) {
        return getSession().createCriteria(OutOfDateParticipantEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("remappedProtein", remappedProtein))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<OutOfDateParticipantEvent> getOutOfDateEventPossiblePerRemappedParent(long processId, String remappedParent) {
        return getSession().createCriteria(OutOfDateParticipantEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("remappedParent", remappedParent))
                .addOrder(Order.asc("eventDate")).list();
    }
}
