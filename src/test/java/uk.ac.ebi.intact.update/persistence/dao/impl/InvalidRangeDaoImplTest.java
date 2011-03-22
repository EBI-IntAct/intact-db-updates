package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.InvalidRangeDao;
import uk.ac.ebi.intact.update.persistence.UpdateProcessDao;

import java.util.Date;
import java.util.List;

/**
 * Unit test for InvalidRangeDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/03/11</pre>
 */

public class InvalidRangeDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void save_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());

        InvalidRange range = invalidRangeDao.getById(invalid.getId());
        Assert.assertEquals(1, range.getFeatureAnnotations().size());
    }

    @Test
    public void update_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());

        InvalidRange range = invalidRangeDao.getById(invalid.getId());
        Assert.assertEquals(1, range.getFeatureAnnotations().size());

        invalid.getFeatureAnnotations().clear();
        invalidRangeDao.update(invalid);

        InvalidRange range2 = invalidRangeDao.getById(invalid.getId());
        Assert.assertEquals(0, range2.getFeatureAnnotations().size());
    }

    @Test
    public void delete_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());

        invalidRangeDao.delete(invalid);
        Assert.assertEquals(0, invalidRangeDao.countAll());
    }

    @Test
    public void get_all_invalids(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(0, invalidRangeDao.getAllOutOfDateRanges().size());
    }

    @Test
    public void get_all_out_of_date(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createOutOfDateRangeWithoutSequenceVersion();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(0, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(1, invalidRangeDao.getAllOutOfDateRanges().size());
    }

    @Test
    public void get_all_out_of_date_sequence_version(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();

        InvalidRange invalid = getMockBuilder().createOutOfDateRangeWithoutSequenceVersion();
        InvalidRange invalid2 = getMockBuilder().createOutOfDateRangeWithSequenceVersion();

        invalidRangeDao.persist(invalid);
        invalidRangeDao.persist(invalid2);

        Long id1 = invalid.getId();
        Long id2 = invalid2.getId();

        List<InvalidRange> invalidRangesWithSequence = invalidRangeDao.getAllOutOfDateRangesWithSequenceVersion();
        List<InvalidRange> invalidRangesWithoutSequence = invalidRangeDao.getAllOutOfDateRangesWithoutSequenceVersion();

        Assert.assertEquals(1, invalidRangesWithSequence.size());
        Assert.assertEquals(1, invalidRangesWithoutSequence.size());
        Assert.assertEquals(id2, invalidRangesWithSequence.iterator().next().getId());
        Assert.assertEquals(id1, invalidRangesWithoutSequence.iterator().next().getId());
    }

    @Test
    public void get_invalid_ranges_using_processId(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();
        UpdateProcessDao processDao = getDaoFactory().getUpdateProcessDao();

        UpdateProcess process1 = getMockBuilder().createUpdateProcess();
        UpdateProcess process2 = getMockBuilder().createUpdateProcess();

        InvalidRange invalid = getMockBuilder().createOutOfDateRangeWithoutSequenceVersion();
        InvalidRange invalid2 = getMockBuilder().createOutOfDateRangeWithSequenceVersion();
        InvalidRange invalid3 = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        invalidRangeDao.persist(invalid2);
        invalidRangeDao.persist(invalid3);

        process1.addRangeUpdate(invalid);
        process2.addRangeUpdate(invalid2);
        process2.addRangeUpdate(invalid3);

        processDao.persist(process1);
        processDao.persist(process2);

        Long id1 = process1.getId();
        Long id2 = process2.getId();

        List<InvalidRange> invalidRanges1 = invalidRangeDao.getInvalidRanges(id1);
        List<InvalidRange> outOfDateRanges1 = invalidRangeDao.getOutOfDateRanges(id1);
        List<InvalidRange> invalidRangesWithSequence1 = invalidRangeDao.getOutOfDateRangesWithSequenceVersion(id1);
        List<InvalidRange> invalidRangesWithoutSequence1 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersion(id1);

        List<InvalidRange> invalidRanges2 = invalidRangeDao.getInvalidRanges(id2);
        List<InvalidRange> outOfDateRanges2 = invalidRangeDao.getOutOfDateRanges(id2);
        List<InvalidRange> invalidRangesWithSequence2 = invalidRangeDao.getOutOfDateRangesWithSequenceVersion(id2);
        List<InvalidRange> invalidRangesWithoutSequence2 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersion(id2);

        Assert.assertEquals(0, invalidRanges1.size());
        Assert.assertEquals(1, invalidRanges2.size());
        Assert.assertEquals(1, outOfDateRanges1.size());
        Assert.assertEquals(1, outOfDateRanges2.size());
        Assert.assertEquals(1, invalidRangesWithoutSequence1.size());
        Assert.assertEquals(0, invalidRangesWithoutSequence2.size());
        Assert.assertEquals(0, invalidRangesWithSequence1.size());
        Assert.assertEquals(1, invalidRangesWithSequence2.size());
    }

    @Test
    public void get_invalid_ranges_using_date(){
        InvalidRangeDao invalidRangeDao = getDaoFactory().getInvalidRangeDao();
        UpdateProcessDao processDao = getDaoFactory().getUpdateProcessDao();

        UpdateProcess process1 = getMockBuilder().createUpdateProcess();
        UpdateProcess process2 = getMockBuilder().createUpdateProcess();
        process2.setDate(new Date(1));

        InvalidRange invalid = getMockBuilder().createOutOfDateRangeWithoutSequenceVersion();
        InvalidRange invalid2 = getMockBuilder().createOutOfDateRangeWithSequenceVersion();
        InvalidRange invalid3 = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        invalidRangeDao.persist(invalid2);
        invalidRangeDao.persist(invalid3);

        process1.addRangeUpdate(invalid);
        process2.addRangeUpdate(invalid2);
        process2.addRangeUpdate(invalid3);

        processDao.persist(process1);
        processDao.persist(process2);

        Date date1 = process1.getDate();
        Date date2 = process2.getDate();

        Assert.assertTrue(process1.getDate().after(process2.getDate()));

        List<InvalidRange> invalidRanges1 = invalidRangeDao.getInvalidRanges(date1);
        List<InvalidRange> outOfDateRanges1 = invalidRangeDao.getOutOfDateRanges(date1);
        List<InvalidRange> invalidRangesWithSequence1 = invalidRangeDao.getOutOfDateRangesWithSequenceVersion(date1);
        List<InvalidRange> invalidRangesWithoutSequence1 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersion(date1);

        List<InvalidRange> invalidRanges2 = invalidRangeDao.getInvalidRanges(date2);
        List<InvalidRange> outOfDateRanges2 = invalidRangeDao.getOutOfDateRanges(date2);
        List<InvalidRange> invalidRangesWithSequence2 = invalidRangeDao.getOutOfDateRangesWithSequenceVersion(date2);
        List<InvalidRange> invalidRangesWithoutSequence2 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersion(date2);

        List<InvalidRange> invalidRanges3 = invalidRangeDao.getInvalidRangesBefore(date1);
        List<InvalidRange> outOfDateRanges3 = invalidRangeDao.getOutOfDateRangesBefore(date1);
        List<InvalidRange> invalidRangesWithSequence3 = invalidRangeDao.getOutOfDateRangesWithSequenceVersionBefore(date1);
        List<InvalidRange> invalidRangesWithoutSequence3 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersionBefore(date1);

        List<InvalidRange> invalidRanges4 = invalidRangeDao.getInvalidRangesAfter(date1);
        List<InvalidRange> outOfDateRanges4 = invalidRangeDao.getOutOfDateRangesAfter(date1);
        List<InvalidRange> invalidRangesWithSequence4 = invalidRangeDao.getOutOfDateRangesWithSequenceVersionAfter(date1);
        List<InvalidRange> invalidRangesWithoutSequence4 = invalidRangeDao.getOutOfDateRangesWithoutSequenceVersionAfter(date1);

        Assert.assertEquals(0, invalidRanges1.size());
        Assert.assertEquals(1, invalidRanges2.size());
        Assert.assertEquals(1, invalidRanges3.size());
        Assert.assertEquals(0, invalidRanges4.size());
        Assert.assertEquals(1, outOfDateRanges1.size());
        Assert.assertEquals(1, outOfDateRanges2.size());
        Assert.assertEquals(2, outOfDateRanges3.size());
        Assert.assertEquals(1, outOfDateRanges4.size());
        Assert.assertEquals(1, invalidRangesWithoutSequence1.size());
        Assert.assertEquals(0, invalidRangesWithoutSequence2.size());
        Assert.assertEquals(1, invalidRangesWithoutSequence3.size());
        Assert.assertEquals(1, invalidRangesWithoutSequence4.size());
        Assert.assertEquals(0, invalidRangesWithSequence1.size());
        Assert.assertEquals(1, invalidRangesWithSequence2.size());
        Assert.assertEquals(1, invalidRangesWithSequence3.size());
        Assert.assertEquals(0, invalidRangesWithSequence4.size());
    }
}
