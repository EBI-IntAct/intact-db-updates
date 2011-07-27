package uk.ac.ebi.intact.update.persistence.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
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
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        PersistentProteinEvent evt2 = getMockBuilder().createDefaultDeadProteinEvent();
        eventDao.persist(evt);
        eventDao.persist(evt2);

        Assert.assertEquals(2, eventDao.countAll());
        Assert.assertEquals(1, getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class).getAllProteinEventsByName(evt.getProteinEventName()).size());
    }

    @Test
    public void updated_protein_event(){
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEventWithCollection();
        eventDao.persist(evt);

        Assert.assertEquals(1, eventDao.countAll());

        PersistentProteinEvent evt2 = eventDao.getById(evt.getId());
        Assert.assertEquals(1, evt2.getUpdatedAliases().size());
        Assert.assertEquals(1, evt2.getUpdatedAnnotations().size());
        Assert.assertEquals(1, evt2.getUpdatedReferences().size());

        evt.getUpdatedAliases().clear();
        evt.getUpdatedAnnotations().clear();
        evt.getUpdatedReferences().clear();

        eventDao.update(evt);

        PersistentProteinEvent evt3 = eventDao.getById(evt.getId());
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
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        ProteinEventName name = evt.getProteinEventName();
        String proteinAc = evt.getProteinAc();

        Assert.assertEquals(1, eventDao.getAllProteinEventsByName(name).size());
        Assert.assertEquals(1, eventDao.getAllUpdateEventsByProteinAc(proteinAc).size());
        Assert.assertEquals(0, eventDao.getAllProteinEventsByName(ProteinEventName.non_uniprot_protein).size());
        Assert.assertEquals(0, eventDao.getAllUpdateEventsByProteinAc("EBI").size());
        Assert.assertEquals(1, eventDao.getAllProteinEventsByNameAndProteinAc(name, proteinAc).size());
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
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        ProteinEventName name = evt.getProteinEventName();
        String proteinAc = evt.getProteinAc();

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getUpdateDaoFactory().getProteinUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndProcessId(proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProcessId(name, id).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAc(name, proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAndDate(proteinAc, date).size());
        Assert.assertEquals(0, eventDao.getUpdateEventsByProteinAcAndDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndDate(name, date).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameAndDate(name, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAc(name, proteinAc, date).size());
    }

    @Test
    public void search_protein_event_by_update_event_and_event_name_before_after_date(){
        ProteinEventDao<PersistentProteinEvent> eventDao = getUpdateDaoFactory().getProteinEventDao(PersistentProteinEvent.class);

        PersistentProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        ProteinEventName name = evt.getProteinEventName();
        String proteinAc = evt.getProteinAc();

        ProteinUpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getUpdateDaoFactory().getProteinUpdateProcessDao().persist(process);

        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, eventDao.getUpdateEventsByProteinAcBeforeDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getUpdateEventsByProteinAcAfterDate(proteinAc, oldDate).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameBeforeDate(name, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAfterDate(name, oldDate).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameAndProteinAcBefore(name, proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAcAfter(name, proteinAc, oldDate).size());
    }
}
