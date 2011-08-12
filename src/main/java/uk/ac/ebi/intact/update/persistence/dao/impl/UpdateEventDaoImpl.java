package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.persistence.dao.UpdateEventDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of UpdateEventDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UpdateEventDaoImpl<T extends UpdateEventImpl> extends UpdateBaseDaoImpl<T> implements UpdateEventDao<T> {
    public UpdateEventDaoImpl() {
        super((Class<T>) UpdateEventImpl.class);
    }

    public UpdateEventDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super( entityClass, entityManager);
    }

    @Override
    public List<T> getByProcessId(long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getByDate(Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getBeforeDate(Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getAfterDate(Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .addOrder(Order.asc("eventDate")).list();
    }
}
