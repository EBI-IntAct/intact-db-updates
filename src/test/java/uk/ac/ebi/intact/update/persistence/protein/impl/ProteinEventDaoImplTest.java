package uk.ac.ebi.intact.update.persistence.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.events.DeadProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.protein.ProteinEventDao;

import java.util.Date;

/**
 * Unit test for ProteinEvemtDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/03/11</pre>
 */

public class ProteinEventDaoImplTest extends UpdateBasicTestCase{

    @Test
    public void create_protein_event(){
        ProteinEventDao<UniprotUpdateEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class);

        UniprotUpdateEvent evt = getMockBuilder().createDefaultProteinEvent();
        DeadProteinEvent evt2 = getMockBuilder().createDefaultDeadProteinEvent();
        eventDao.persist(evt);
        getUpdateDaoFactory().getProteinEventDao(DeadProteinEvent.class).persist(evt2);

        Assert.assertEquals(2, getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class).countAll());
        Assert.assertEquals(1, getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class).getAll().size());
    }

    @Test
    public void updated_protein_event(){
        ProteinEventDao<UniprotUpdateEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class);

        UniprotUpdateEvent evt = getMockBuilder().createDefaultProteinEventWithCollection();
        eventDao.persist(evt);

        Assert.assertEquals(1, eventDao.countAll());

        UniprotUpdateEvent evt2 = eventDao.getById(evt.getId());
        Assert.assertEquals(1, evt2.getUpdatedAliases().size());
        Assert.assertEquals(1, evt2.getUpdatedAnnotations().size());
        Assert.assertEquals(1, evt2.getUpdatedReferences().size());

        evt.getUpdatedAliases().clear();
        evt.getUpdatedAnnotations().clear();
        evt.getUpdatedReferences().clear();

        eventDao.update(evt);

        UniprotUpdateEvent evt3 = eventDao.getById(evt.getId());
        Assert.assertEquals(0, evt3.getUpdatedAliases().size());
        Assert.assertEquals(0, evt3.getUpdatedAnnotations().size());
        Assert.assertEquals(0, evt3.getUpdatedReferences().size());
    }

    @Test
    public void delete_protein_event(){
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        Assert.assertEquals(1, eventDao.countAll());

        eventDao.delete(evt);
        Assert.assertEquals(0, eventDao.countAll());
    }

    @Test
    public void search_protein_event_by_name_and_protein_ac(){
        ProteinEventDao<UniprotUpdateEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class);

        UniprotUpdateEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        String proteinAc = evt.getProteinAc();

        Assert.assertEquals(1, eventDao.getAll().size());
        Assert.assertEquals(1, eventDao.getAllUpdateEventsByProteinAc(proteinAc).size());
        Assert.assertEquals(0, getUpdateDaoFactory().getProteinEventDao(DeadProteinEvent.class).getAll().size());
        Assert.assertEquals(0, eventDao.getAllUpdateEventsByProteinAc("EBI").size());
    }

    @Test
    public void search_protein_event_by_update_event(){
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getUpdateDaoFactory().getProteinUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertEquals(1, eventDao.getAllUpdateEventsByProcessId(id).size());
        Assert.assertEquals(1, eventDao.getAllUpdateEventsByDate(date).size());
        Assert.assertEquals(0, eventDao.getAllUpdateEventsByDate(oldDate).size());
        Assert.assertEquals(0, eventDao.getAllUpdateEventsByProcessId(1).size());
    }

    @Test
    public void search_protein_event_by_update_event_and_event_name(){
        ProteinEventDao<UniprotUpdateEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(UniprotUpdateEvent.class);

        UniprotUpdateEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        String proteinAc = evt.getProteinAc();

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getUpdateDaoFactory().getProteinUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndProcessId(proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndProcessId(proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndDate(proteinAc, date).size());
        Assert.assertEquals(0, eventDao.getUpdateEventsByProteinAcAndDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getAllUpdateEventsByDate(date).size());
        Assert.assertEquals(0, eventDao.getAllUpdateEventsByDate(oldDate).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndDate(proteinAc, date).size());
    }

    @Test
    public void search_protein_event_by_update_event_and_event_name_before_after_date(){
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        String proteinAc = evt.getProteinAc();

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getUpdateDaoFactory().getProteinUpdateProcessDao().persist(process);

        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, eventDao.getUpdateEventsByProteinAcBeforeDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAfterDate(proteinAc, oldDate).size());
        Assert.assertEquals(0, eventDao.getUpdateEventsBeforeDate(oldDate).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsAfterDate(oldDate).size());
        Assert.assertEquals(0, eventDao.getUpdateEventsByProteinAcBeforeDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAfterDate(proteinAc, oldDate).size());
    }
}
