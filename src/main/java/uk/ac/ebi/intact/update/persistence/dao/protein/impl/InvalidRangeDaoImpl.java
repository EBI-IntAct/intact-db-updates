package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.persistence.dao.protein.InvalidRangeDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of InvalidRangeDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class InvalidRangeDaoImpl extends UpdatedRangeDaoImpl<PersistentInvalidRange> implements InvalidRangeDao {

    public InvalidRangeDaoImpl() {
        super(PersistentInvalidRange.class, null);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public InvalidRangeDaoImpl(EntityManager entityManager) {
        super(PersistentInvalidRange.class, entityManager);
    }

    @Override
    public List<PersistentInvalidRange> getAllInvalidRanges() {
        return getSession().createCriteria(PersistentInvalidRange.class)
                .add(Restrictions.eq("sequenceVersion", -1)).list();
    }

    @Override
    public List<PersistentInvalidRange> getAllOutOfDateRanges() {
        return getSession().createCriteria(PersistentInvalidRange.class)
                .add(Restrictions.ne("sequenceVersion", -1)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRanges(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.eq("sequenceVersion", -1)).add(Restrictions.eq("p2.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRanges(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.ne("sequenceVersion", -1)).add(Restrictions.eq("p2.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRanges(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.lt("p2.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p2.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRanges(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.lt("p2.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p2.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRangesBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.le("p2.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.le("p2.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRangesAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.ge("p2.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("parent", "p").createAlias("p.updateProcess", "p2")
                .add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.ge("p2.date", updateddate)).list();
    }
}
