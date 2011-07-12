package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;
import uk.ac.ebi.intact.update.model.protein.update.events.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.UpdatedRangeDao;

import java.util.Date;

/**
 * Unit test for UpdateRangeDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/03/11</pre>
 */

public class UpdatedRangeDaoImplTest extends UpdateBasicTestCase{

    @Test
    public void create_updated_range(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.countAll());

        UpdatedRangeDao<InvalidRange> invalidRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(InvalidRange.class);
        Assert.assertEquals(0, invalidRangeDao.countAll());
    }

    @Test
    public void delete_updated_range(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.countAll());

        updatedRangeDao.delete(range);
        Assert.assertEquals(0, updatedRangeDao.countAll());
    }

    @Test
    public void search_by_rangeAc_component_Ac(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        String rangeAc = range.getRangeAc();
        String componentAc = range.getComponentAc();

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAc(rangeAc).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAc(componentAc).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByRangeAc(componentAc).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByComponentAc(rangeAc).size());
    }

    @Test
    public void search_by_processId_Date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addRangeUpdate(range);

        getUpdateDaoFactory().getUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();

        Date oldDate = new Date(1);
        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByUpdateProcessId(id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByUpdateDate(date).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesBeforeUpdateDate(oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesAfterUpdateDate(oldDate).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByUpdateProcessId(1).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByUpdateDate(oldDate).size());
    }

    @Test
    public void search_by_range_having_featureAnnotations(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createInvalidRange();

        Assert.assertEquals(1, range.getFeatureAnnotations().size());

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesHavingUpdatedFeatureAnnotations().size());
    }

    @Test
    public void search_by_rangeAc_component_Ac_process_id_and_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        String rangeAc = range.getRangeAc();
        String componentAc = range.getComponentAc();

        updatedRangeDao.persist(range);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addRangeUpdate(range);

        getUpdateDaoFactory().getUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndProcessId(rangeAc, id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndDate(rangeAc, date).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndProcessId(componentAc, id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndDate(componentAc, date).size());
    }

    @Test
    public void search_by_range_having_featureAnnotations_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createInvalidRange();

        Assert.assertEquals(1, range.getFeatureAnnotations().size());

        updatedRangeDao.persist(range);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addRangeUpdate(range);

        getUpdateDaoFactory().getUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesHavingUpdatedFeatureAnnotations(id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesHavingUpdatedFeatureAnnotations(date).size());
    }

    @Test
    public void search_by_rangeAc_component_Ac_process_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        String rangeAc = range.getRangeAc();
        String componentAc = range.getComponentAc();

        updatedRangeDao.persist(range);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addRangeUpdate(range);

        getUpdateDaoFactory().getUpdateProcessDao().persist(process);

        Date date = process.getDate();

        Date oldDate = new Date(1);
        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByRangeAcAndBeforeDate(rangeAc, oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndAfterDate(rangeAc, oldDate).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByComponentAcAndBeforeDate(componentAc, oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndAfterDate(componentAc, oldDate).size());
    }

    @Test
    public void search_by_range_having_featureAnnotations_process_id_and_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createInvalidRange();

        Assert.assertEquals(1, range.getFeatureAnnotations().size());

        updatedRangeDao.persist(range);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addRangeUpdate(range);

        getUpdateDaoFactory().getUpdateProcessDao().persist(process);

        Date date = process.getDate();

        Date oldDate = new Date(1);
        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesHavingUpdatedFeatureAnnotationsBefore(oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesHavingUpdatedFeatureAnnotationsAfter(oldDate).size());
    }
}
