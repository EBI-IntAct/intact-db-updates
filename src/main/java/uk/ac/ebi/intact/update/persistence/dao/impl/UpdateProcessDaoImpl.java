package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateProcessImpl;
import uk.ac.ebi.intact.update.persistence.dao.UpdateProcessDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of ProteinUpdateProcessDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UpdateProcessDaoImpl<T extends UpdateProcessImpl> extends UpdateBaseDaoImpl<T> implements UpdateProcessDao<T> {

    public UpdateProcessDaoImpl() {
        super((Class<T>) UpdateProcessImpl.class);
    }

    public UpdateProcessDaoImpl(Class<T> entityClass) {
        super(entityClass);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public UpdateProcessDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }

    @Override
    public List<T> getByDate(Date date) {
        return getSession().createCriteria(getEntityClass())
                .add(Restrictions.lt("date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("date", DateUtils.addDays(date, -1))).list();
    }

    @Override
    public List<T> getBeforeDate(Date date) {
        return getSession().createCriteria(getEntityClass())
                .add(Restrictions.le("date", date)).list();
    }

    @Override
    public List<T> getAfterDate(Date date) {
        return getSession().createCriteria(getEntityClass())
                .add(Restrictions.ge("date", date)).list();
    }
}
