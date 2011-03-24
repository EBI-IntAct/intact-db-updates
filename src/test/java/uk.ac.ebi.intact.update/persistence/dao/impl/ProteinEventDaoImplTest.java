package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.events.DeadProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEvent;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.ProteinEventDao;

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
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        DeadProteinEvent evt2 = getMockBuilder().createDefaultDeadProteinEvent();
        eventDao.persist(evt);
        eventDao.persist(evt2);

        Assert.assertEquals(2, eventDao.countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinEventDao(DeadProteinEvent.class).countAll());
    }

    @Test
    public void updated_protein_event(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEventWithCollection();
        eventDao.persist(evt);

        Assert.assertEquals(1, eventDao.countAll());

        ProteinEvent evt2 = eventDao.getById(evt.getId());
        Assert.assertEquals(1, evt2.getUpdatedAliases().size());
        Assert.assertEquals(1, evt2.getUpdatedAnnotations().size());
        Assert.assertEquals(1, evt2.getUpdatedReferences().size());

        evt.getUpdatedAliases().clear();
        evt.getUpdatedAnnotations().clear();
        evt.getUpdatedReferences().clear();

        eventDao.update(evt);

        ProteinEvent evt3 = eventDao.getById(evt.getId());
        Assert.assertEquals(0, evt3.getUpdatedAliases().size());
        Assert.assertEquals(0, evt3.getUpdatedAnnotations().size());
        Assert.assertEquals(0, evt3.getUpdatedReferences().size());
    }

    @Test
    public void delete_protein_event(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        Assert.assertEquals(1, eventDao.countAll());

        eventDao.delete(evt);
        Assert.assertEquals(0, eventDao.countAll());
    }

    @Test
    public void search_protein_event_by_name_and_protein_ac(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        EventName name = evt.getName();
        String proteinAc = evt.getProteinAc();

        Assert.assertEquals(1, eventDao.getAllProteinEventsByName(name).size());
        Assert.assertEquals(1, eventDao.getAllProteinEventsByProteinAc(proteinAc).size());
        Assert.assertEquals(0, eventDao.getAllProteinEventsByName(EventName.non_uniprot_protein).size());
        Assert.assertEquals(0, eventDao.getAllProteinEventsByProteinAc("EBI").size());
        Assert.assertEquals(1, eventDao.getAllProteinEventsByNameAndProteinAc(name, proteinAc).size());
    }

    @Test
    public void search_protein_event_by_update_event(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getDaoFactory().getUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertEquals(1, eventDao.getAllProteinEventsByProcessId(id).size());
        Assert.assertEquals(1, eventDao.getAllProteinEventsByDate(date).size());
        Assert.assertEquals(0, eventDao.getAllProteinEventsByDate(oldDate).size());
        Assert.assertEquals(0, eventDao.getAllProteinEventsByProcessId(1).size());
    }

    @Test
    public void search_protein_event_by_update_event_and_event_name(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        EventName name = evt.getName();
        String proteinAc = evt.getProteinAc();

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getDaoFactory().getUpdateProcessDao().persist(process);

        Long id = process.getId();
        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(1, eventDao.getProteinEventsByProteinAcAndProcessId(proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProcessId(name, id).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAc(name, proteinAc, id).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByProteinAcAndDate(proteinAc, date).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByProteinAcAndDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndDate(name, date).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameAndDate(name, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAc(name, proteinAc, date).size());
    }

    @Test
    public void search_protein_event_by_update_event_and_event_name_before_after_date(){
        ProteinEventDao<ProteinEvent> eventDao = getDaoFactory().getProteinEventDao(ProteinEvent.class);

        ProteinEvent evt = getMockBuilder().createDefaultProteinEvent();
        eventDao.persist(evt);

        EventName name = evt.getName();
        String proteinAc = evt.getProteinAc();

        UpdateProcess process = getMockBuilder().createUpdateProcess();
        process.addEvent(evt);

        getDaoFactory().getUpdateProcessDao().persist(process);

        Date date = process.getDate();
        Date oldDate = new Date(1);

        Assert.assertTrue(oldDate.before(date));

        Assert.assertEquals(0, eventDao.getProteinEventsByProteinAcBeforeDate(proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByProteinAcAfterDate(proteinAc, oldDate).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameBeforeDate(name, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAfterDate(name, oldDate).size());
        Assert.assertEquals(0, eventDao.getProteinEventsByNameAndProteinAcBefore(name, proteinAc, oldDate).size());
        Assert.assertEquals(1, eventDao.getProteinEventsByNameAndProteinAcAfter(name, proteinAc, oldDate).size());
    }
}
