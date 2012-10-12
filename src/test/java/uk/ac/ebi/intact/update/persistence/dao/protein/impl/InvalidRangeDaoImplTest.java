package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.events.OutOfDateParticipantEvent;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.InvalidRangeDao;
import uk.ac.ebi.intact.update.persistence.dao.protein.ProteinUpdateProcessDao;

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
    @DirtiesContext
    public void save_all_invalids(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();

        PersistentInvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
//        Assert.assertEquals(1, invalidRangeDao.countAll());

        PersistentInvalidRange range = invalidRangeDao.getById(invalid.getId());
        Assert.assertEquals(invalid.getId(), range.getId());
    }

    @Test
    @DirtiesContext
    public void delete_all_invalids(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();

        PersistentInvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.countAll());

        invalidRangeDao.delete(invalid);
        Assert.assertEquals(0, invalidRangeDao.countAll());
    }

    @Test
    @DirtiesContext
    public void get_all_invalids(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();

        PersistentInvalidRange invalid = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(1, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(0, invalidRangeDao.getAllOutOfDateRanges().size());
    }

    @Test
    @DirtiesContext
    public void get_all_out_of_date(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();

        PersistentInvalidRange invalid = getMockBuilder().createOutOfDateRangeWithSequenceVersion();

        invalidRangeDao.persist(invalid);
        Assert.assertEquals(0, invalidRangeDao.getAllInvalidRanges().size());
        Assert.assertEquals(1, invalidRangeDao.getAllOutOfDateRanges().size());
    }

    @Test
    @DirtiesContext
    public void get_invalid_ranges_using_processId(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();
        ProteinUpdateProcessDao processDao = getUpdateDaoFactory().getProteinUpdateProcessDao();

        ProteinUpdateProcess process1 = getMockBuilder().createUpdateProcess();
        ProteinUpdateProcess process2 = getMockBuilder().createUpdateProcess();

        PersistentInvalidRange invalid2 = getMockBuilder().createOutOfDateRangeWithSequenceVersion();
        PersistentInvalidRange invalid3 = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid2);
        invalidRangeDao.persist(invalid3);

        OutOfDateParticipantEvent proteinEvent = getMockBuilder().createOutOfDateParticipantProcess();

        proteinEvent.setUpdateProcess(process2);
        getUpdateDaoFactory().getProteinEventDao(OutOfDateParticipantEvent.class).persist(proteinEvent);

        proteinEvent.addRangeUpdate(invalid2);
        proteinEvent.addRangeUpdate(invalid3);

        processDao.persist(process1);

        Long id1 = process1.getId();
        Long id2 = process2.getId();

        List<PersistentInvalidRange> invalidRanges1 = invalidRangeDao.getInvalidRanges(id1);
        List<PersistentInvalidRange> outOfDateRanges1 = invalidRangeDao.getOutOfDateRanges(id1);

        List<PersistentInvalidRange> invalidRanges2 = invalidRangeDao.getInvalidRanges(id2);
        List<PersistentInvalidRange> outOfDateRanges2 = invalidRangeDao.getOutOfDateRanges(id2);

        Assert.assertEquals(0, invalidRanges1.size());
        Assert.assertEquals(1, invalidRanges2.size());
        Assert.assertEquals(0, outOfDateRanges1.size());
        Assert.assertEquals(1, outOfDateRanges2.size());
    }

    @Test
    @DirtiesContext
    public void get_invalid_ranges_using_date(){
        InvalidRangeDao invalidRangeDao = getUpdateDaoFactory().getInvalidRangeDao();
        ProteinUpdateProcessDao processDao = getUpdateDaoFactory().getProteinUpdateProcessDao();

        ProteinUpdateProcess process1 = getMockBuilder().createUpdateProcess();
        ProteinUpdateProcess process2 = getMockBuilder().createUpdateProcess();
        process2.setDate(new Date(1));

        PersistentInvalidRange invalid2 = getMockBuilder().createOutOfDateRangeWithSequenceVersion();
        PersistentInvalidRange invalid3 = getMockBuilder().createInvalidRange();

        invalidRangeDao.persist(invalid2);
        invalidRangeDao.persist(invalid3);

        OutOfDateParticipantEvent proteinEvent = getMockBuilder().createOutOfDateParticipantProcess();
        proteinEvent.setUpdateProcess(process2);
        getUpdateDaoFactory().getProteinEventDao(OutOfDateParticipantEvent.class).persist(proteinEvent);

        proteinEvent.addRangeUpdate(invalid2);
        proteinEvent.addRangeUpdate(invalid3);

        processDao.persist(process1);

        Date date1 = process1.getDate();
        Date date2 = process2.getDate();

        Assert.assertTrue(process1.getDate().after(process2.getDate()));

        List<PersistentInvalidRange> invalidRanges1 = invalidRangeDao.getInvalidRanges(date1);
        List<PersistentInvalidRange> outOfDateRanges1 = invalidRangeDao.getOutOfDateRanges(date1);

        List<PersistentInvalidRange> invalidRanges2 = invalidRangeDao.getInvalidRanges(date2);
        List<PersistentInvalidRange> outOfDateRanges2 = invalidRangeDao.getOutOfDateRanges(date2);

        List<PersistentInvalidRange> invalidRanges3 = invalidRangeDao.getInvalidRangesBefore(date1);
        List<PersistentInvalidRange> outOfDateRanges3 = invalidRangeDao.getOutOfDateRangesBefore(date1);

        List<PersistentInvalidRange> invalidRanges4 = invalidRangeDao.getInvalidRangesAfter(date1);
        List<PersistentInvalidRange> outOfDateRanges4 = invalidRangeDao.getOutOfDateRangesAfter(date1);

        Assert.assertEquals(0, invalidRanges1.size());
        Assert.assertEquals(1, invalidRanges2.size());
        Assert.assertEquals(1, invalidRanges3.size());
        Assert.assertEquals(0, invalidRanges4.size());
        Assert.assertEquals(0, outOfDateRanges1.size());
        Assert.assertEquals(1, outOfDateRanges2.size());
        Assert.assertEquals(1, outOfDateRanges3.size());
        Assert.assertEquals(0, outOfDateRanges4.size());
    }
}
