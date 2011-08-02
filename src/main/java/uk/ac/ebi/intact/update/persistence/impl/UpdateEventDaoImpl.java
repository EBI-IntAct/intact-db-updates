package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.persistence.UpdateEventDao;

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
    public List<T> getAllUpdateEventsByName(String name) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getAllUpdateEventsByProcessId(long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p").add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getAllUpdateEventsByDate(Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1))).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProcessId(String name, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndDate(String name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameBeforeDate(String name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAfterDate(String name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }
}
