package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateEvent;
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
public class UpdateEventDaoImpl<T extends UpdateEvent> extends UpdateBaseDaoImpl<T> implements UpdateEventDao<T> {
    public UpdateEventDaoImpl() {
        super((Class<T>) UpdateEvent.class);
    }

    public UpdateEventDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super( entityClass, entityManager);
    }

    @Override
    public List<T> getAllUpdateEventsByName(String name) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getAllUpdateEventsByIntactAc(String intactObjectAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
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
    public List<T> getAllUpdateEventsByNameAndIntactAc(String name, String proteinAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProcessId(String name, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getUpdateEventsByIntactAcAndProcessId(String intactObjectAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndIntactAc(String name, String proteinAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("intactObjectAc", proteinAc))
                .add(Restrictions.eq("p.id", processId)).list();
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
    public List<T> getUpdateEventsByIntactAcAndDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndIntactAc(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(date, -1)))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameBeforeDate(String name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getUpdateEventsByIntactAcBeforeDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndIntactAcBefore(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAfterDate(String name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getUpdateEventsByIntactAcAfterDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndIntactAcAfter(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("intactObjectAc", intactObjectAc)).list();
    }
}
