package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.SequenceUpdateEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.SequenceUpdateEventDao;

import javax.persistence.EntityManager;
import java.util.List;

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

    @Override
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservation(long processId, double cons) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("relativeConservation", cons))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservationInferiorTo(long processId, double cons) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.le("relativeConservation", cons)).list();
    }

    @Override
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithRelativeConservationSuperiorTo(long processId, double cons) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.ge("relativeConservation", cons))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithoutOldSequence(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNull("oldSequence"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<SequenceUpdateEvent> getSequenceUpdateEventWithOldSequence(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotNull("oldSequence"))
                .addOrder(Order.asc("eventDate")).list();
    }
}
