package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.ProteinEventDao;

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
        super((Class<T>) PersistentProteinEvent.class, null);
    }
    public ProteinEventDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super( entityClass, entityManager);
    }

    @Override
    public List<T> getAllUpdateEventsByProteinAc(String intactObjectAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("proteinAc", intactObjectAc))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAndProcessId(String intactObjectAc, long processId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.eq("proteinAc", intactObjectAc)).add(Restrictions.eq("p.id", processId))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAndDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.lt("p.date", DateUtils.addDays(updatedDate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updatedDate, -1)))
                .add(Restrictions.eq("proteinAc", intactObjectAc))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcBeforeDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.le("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", intactObjectAc))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getUpdateEventsByProteinAcAfterDate(String intactObjectAc, Date updatedDate) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateProcess", "p")
                .add(Restrictions.ge("p.date", updatedDate))
                .add(Restrictions.eq("proteinAc", intactObjectAc))
                .addOrder(Order.asc("eventDate")).list();
    }
}
