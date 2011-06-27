package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.persistence.ProteinEventDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of ProteinEventDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class ProteinEventDaoImpl<T extends PersistentProteinEvent> extends UpdateBaseDaoImpl<T> implements ProteinEventDao<T> {
    public ProteinEventDaoImpl() {
        super((Class<T>) PersistentProteinEvent.class);
    }

    public ProteinEventDaoImpl(Class<PersistentProteinEvent> entityClass, EntityManager entityManager) {
        super((Class<T>) entityClass, entityManager);
    }

    @Override
    public List<T> getAllProteinEventsByName(EventName name) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAc(String proteinAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getAllProteinEventsByProcessId(long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p").add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getAllProteinEventsByDate(Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1))).list();
    }

    @Override
    public List<T> getAllProteinEventsByNameAndProteinAc(EventName name, String proteinAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProcessId(EventName name, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getProteinEventsByProteinAcAndProcessId(String proteinAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("proteinAc", proteinAc)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndDate(EventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getProteinEventsByProteinAcAndDate(String proteinAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(date, -1)))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameBeforeDate(EventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getProteinEventsByProteinAcBeforeDate(String proteinAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcBefore(EventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAfterDate(EventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("name", name)).list();
    }

    @Override
    public List<T> getProteinEventsByProteinAcAfterDate(String proteinAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcAfter(EventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }
}
