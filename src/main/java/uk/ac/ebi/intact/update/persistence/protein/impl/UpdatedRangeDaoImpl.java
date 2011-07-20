package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.persistence.protein.UpdatedRangeDao;
import uk.ac.ebi.intact.update.persistence.impl.UpdateBaseDaoImpl;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of UpdatedRangeDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UpdatedRangeDaoImpl<T extends PersistentUpdatedRange> extends UpdateBaseDaoImpl<T> implements UpdatedRangeDao<T> {

    public UpdatedRangeDaoImpl() {
        super((Class<T>) PersistentUpdatedRange.class, null);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public UpdatedRangeDaoImpl(EntityManager entityManager) {
        super((Class<T>) PersistentUpdatedRange.class, entityManager);
    }

    @Override
    public List<T> getUpdatedRangesByRangeAc(String rangeAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("rangeAc", rangeAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAc(String componentAc) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("componentAc", componentAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByUpdateProcessId(long processId) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.eq("p2.id", processId)).list();
    }

    @Override
    public List<T> getUpdatedRangesByUpdateDate(Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.lt("p2.date", DateUtils.addDays(updateDate, 1)))
                .add(Restrictions.gt("p2.date", DateUtils.addDays(updateDate, -1))).list();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndDate(String rangeAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.lt("p2.date", DateUtils.addDays(updateDate, 1)))
                .add(Restrictions.gt("p2.date", DateUtils.addDays(updateDate, -1)))
                .add(Restrictions.eq("rangeAc", rangeAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndDate(String componentAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.lt("p2.date", DateUtils.addDays(updateDate, 1)))
                .add(Restrictions.gt("p2.date", DateUtils.addDays(updateDate, -1)))
                .add(Restrictions.eq("componentAc", componentAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesBeforeUpdateDate(Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.le("p2.date", updateDate)).list();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndBeforeDate(String rangeAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.le("p2.date", updateDate))
                .add(Restrictions.eq("rangeAc", rangeAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndBeforeDate(String componentAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.le("p2.date", updateDate))
                .add(Restrictions.eq("componentAc", componentAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesAfterUpdateDate(Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.ge("p2.date", updateDate)).list();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndAfterDate(String rangeAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.ge("p2.date", updateDate))
                .add(Restrictions.eq("rangeAc", rangeAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndAfterDate(String componentAc, Date updateDate) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.ge("p2.date", updateDate))
                .add(Restrictions.eq("componentAc", componentAc)).list();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndProcessId(String rangeAc, long processId) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.eq("rangeAc", rangeAc))
                .add(Restrictions.eq("p2.id", processId)).list();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndProcessId(String componentAc, long processId) {
        return getSession().createCriteria(getEntityClass()).createAlias("parent", "p").createAlias("p.parent", "p2")
                .add(Restrictions.eq("componentAc", componentAc))
                .add(Restrictions.eq("p2.id", processId)).list();
    }
}
