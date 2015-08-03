package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.UpdatedRangeDao;

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
    @DirtiesContext
    public void create_updated_range(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.countAll());

        UpdatedRangeDao<PersistentInvalidRange> invalidRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentInvalidRange.class);
        Assert.assertEquals(0, invalidRangeDao.countAll());
    }

    @Test
    @DirtiesContext
    public void delete_updated_range(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        Assert.assertEquals(1, updatedRangeDao.countAll());

        updatedRangeDao.delete(range);
        Assert.assertEquals(0, updatedRangeDao.countAll());
    }

    @Test
    @DirtiesContext
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
    @DirtiesContext
    public void search_by_processId_Date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        updatedRangeDao.persist(range);

        UniprotUpdateEvent proteinEvent = getMockBuilder().createDefaultUniprotProteinEvent();
        proteinEvent.addRangeUpdate(range);

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        proteinEvent.setUpdateProcess(process);

        getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class).persist(proteinEvent);

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
    @DirtiesContext
    public void search_by_rangeAc_component_Ac_process_id_and_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        String rangeAc = range.getRangeAc();
        String componentAc = range.getComponentAc();

        updatedRangeDao.persist(range);

        UniprotUpdateEvent proteinEvent = getMockBuilder().createDefaultUniprotProteinEvent();
        proteinEvent.addRangeUpdate(range);
        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        proteinEvent.setUpdateProcess(process);
        getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class).persist(proteinEvent);

        Long id = process.getId();
        Date date = process.getDate();

        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndProcessId(rangeAc, id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndDate(rangeAc, date).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndProcessId(componentAc, id).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndDate(componentAc, date).size());
    }

    @Test
    @DirtiesContext
    public void search_by_rangeAc_component_Ac_process_date(){
        UpdatedRangeDao<PersistentUpdatedRange> updatedRangeDao = getUpdateDaoFactory().getUpdatedRangeDao(PersistentUpdatedRange.class);

        PersistentUpdatedRange range = getMockBuilder().createUpdatedRange();

        String rangeAc = range.getRangeAc();
        String componentAc = range.getComponentAc();

        updatedRangeDao.persist(range);

        UniprotUpdateEvent proteinEvent = getMockBuilder().createDefaultUniprotProteinEvent();
        proteinEvent.addRangeUpdate(range);
        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        proteinEvent.setUpdateProcess(process);

        getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class).persist(proteinEvent);

        Date date = process.getDate();

        Date oldDate = new Date(1);
        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByRangeAcAndBeforeDate(rangeAc, oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByRangeAcAndAfterDate(rangeAc, oldDate).size());
        Assert.assertEquals(0, updatedRangeDao.getUpdatedRangesByComponentAcAndBeforeDate(componentAc, oldDate).size());
        Assert.assertEquals(1, updatedRangeDao.getUpdatedRangesByComponentAcAndAfterDate(componentAc, oldDate).size());
    }
}
