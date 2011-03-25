package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;
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
public class InvalidRangeDaoImpl extends UpdatedRangeDaoImpl<InvalidRange> implements InvalidRangeDao {

    private final static String INVALID_RANGE_AC = "EBI-2907496";
    private final static String OUT_OF_DATE_RANGE_AC = "EBI-3058809";

    @Override
    public List<InvalidRange> getAllInvalidRanges() {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC)).list();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRanges() {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).list();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRangesWithoutSequenceVersion() {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1)).list();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRangesWithSequenceVersion() {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1)).list();
    }

    @Override
    public List<InvalidRange> getInvalidRanges(long processId) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRanges(long processId) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(long processId) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(long processId) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.eq("p.id", processId)).list();
    }

    @Override
    public List<InvalidRange> getInvalidRanges(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRanges(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.lt("p.date", DateUtils.addDays(updateddate, 1)))
                .add(Restrictions.gt("p.date", DateUtils.addDays(updateddate, -1))).list();
    }

    @Override
    public List<InvalidRange> getInvalidRangesBefore(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesBefore(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionBefore(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.date", updateddate)).add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionBefore(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.le("p.date", updateddate)).add(Restrictions.le("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getInvalidRangesAfter(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", INVALID_RANGE_AC))
                .add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesAfter(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC))
                .add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionAfter(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.eq("sequenceVersion", -1))
                .add(Restrictions.eq("p.date", updateddate)).add(Restrictions.ge("p.date", updateddate)).list();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionAfter(Date updateddate) {
        return getSession().createCriteria(InvalidRange.class).createAlias("featureAnnotations", "a").createAlias("parent", "p")
                .add(Restrictions.eq("a.topic", OUT_OF_DATE_RANGE_AC)).add(Restrictions.ne("sequenceVersion", -1))
                .add(Restrictions.le("p.date", updateddate)).add(Restrictions.ge("p.date", updateddate)).list();
    }
}
