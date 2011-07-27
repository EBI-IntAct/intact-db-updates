package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.persistence.impl.UpdateEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.protein.ProteinEventDao;

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
public class ProteinEventDaoImpl<T extends PersistentProteinEvent> extends UpdateEventDaoImpl<T> implements ProteinEventDao<T> {

    public ProteinEventDaoImpl(){
        super();
    }
    public ProteinEventDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super( entityClass, entityManager);
    }

    @Override
    public List<T> getAllProteinEventsByName(ProteinEventName name) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name.toString())).list();
    }

    @Override
    public List<T> getAllProteinEventsByNameAndProteinAc(ProteinEventName name, String proteinAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name.toString()))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getAllUpdateEventsByProteinAc(String intactObjectAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getAllUpdateEventsByNameAndProteinAc(String name, String intactObjectAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProcessId(ProteinEventName name, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name.toString())).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(ProteinEventName name, String proteinAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name.toString()))
                .add(Restrictions.eq("proteinAc", proteinAc))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAndProcessId(String intactObjectAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("proteinAc", intactObjectAc)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProteinAc(String name, String intactObjectAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", intactObjectAc))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndDate(ProteinEventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("name", name.toString())).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(ProteinEventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(date, -1)))
                .add(Restrictions.eq("name", name.toString()))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAndDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProteinAc(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(date, -1)))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameBeforeDate(ProteinEventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("name", name.toString())).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcBefore(ProteinEventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", date))
                .add(Restrictions.eq("name", name.toString()))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcBeforeDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProteinAcBefore(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.le("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getProteinEventsByNameAfterDate(ProteinEventName name, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("name", name.toString())).list();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcAfter(ProteinEventName name, String proteinAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", date))
                .add(Restrictions.eq("name", name.toString()))
                .add(Restrictions.eq("proteinAc", proteinAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAfterDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }

    @Override
    public List<T> getUpdateEventsByNameAndProteinAcAfter(String name, String intactObjectAc, Date date) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.ge("p.date", date))
                .add(Restrictions.eq("name", name))
                .add(Restrictions.eq("proteinAc", intactObjectAc)).list();
    }
}
