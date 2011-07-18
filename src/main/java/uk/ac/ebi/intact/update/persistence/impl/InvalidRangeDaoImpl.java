package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.persistence.InvalidRangeDao;

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

    private final static String INVALID_RANGE_AC = "EBI-2907496";
    private final static String OUT_OF_DATE_RANGE_AC = "EBI-3058809";

    @Override
    public List<PersistentInvalidRange> getAllInvalidRanges() {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC)).list();
    }

    @Override
    public List<PersistentInvalidRange> getAllOutOfDateRanges() {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).list();
    }

    @Override
    public List<PersistentInvalidRange> getAllOutOfDateRangesWithoutSequenceVersion() {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1)).list();
    }

    @Override
    public List<PersistentInvalidRange> getAllOutOfDateRangesWithSequenceVersion() {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRanges(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRanges(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersion(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersion(long processId) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRanges(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRanges(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersion(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersion(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRangesBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersionBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.date", updateddate)).add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersionBefore(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.le("p.date", updateddate)).add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getInvalidRangesAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithoutSequenceVersionAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.date", updateddate)).add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<PersistentInvalidRange> getOutOfDateRangesWithSequenceVersionAfter(Date updateddate) {
        return getSession().createCriteria(PersistentInvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.le("p.date", updateddate)).add(Restrictions.ge("p.date", updateddate)).list();
    }
}
