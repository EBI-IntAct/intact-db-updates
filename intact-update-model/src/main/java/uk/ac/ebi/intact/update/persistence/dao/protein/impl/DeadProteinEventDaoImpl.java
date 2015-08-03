package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.DeadProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.DeadProteinEventDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default implementation of Dead protein event dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DeadProteinEventDaoImpl extends ProteinEventDaoImpl<DeadProteinEvent> implements DeadProteinEventDao {

    public DeadProteinEventDaoImpl(){
        super(DeadProteinEvent.class, null);
    }
    public DeadProteinEventDaoImpl(EntityManager entityManager) {
        super( DeadProteinEvent.class, entityManager);
    }

    @Override
    public List<DeadProteinEvent> getAllDeadProteinEventsHavingDeletedXrefs(long id) {
        return getSession().createCriteria(DeadProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", id)).
                add(Restrictions.isNotEmpty("deletedXrefs"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<DeadProteinEvent> getAllDeadProteinEventsWithoutDeletedXrefs(long id) {
        return getSession().createCriteria(DeadProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", id)).
                add(Restrictions.isEmpty("deletedXrefs"))
                .addOrder(Order.asc("eventDate")).list();
    }
}
