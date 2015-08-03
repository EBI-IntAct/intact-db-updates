package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.MappingReportDao;

import java.util.List;

/**
 * Unit test for MappingReportDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class MappingReportDaoImplTest extends UpdateBasicTestCase {

    @Test
    @DirtiesContext
    public void search_all() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();
        Assert.assertEquals( 1, actionReportDao.countAll() );
    }

    @Test
    @DirtiesContext
    public void search_ActionReportWithId_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        long id = report.getId();

        PersistentMappingReport r = actionReportDao.getById(id);

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_ActionReportWithId_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        PersistentMappingReport r = actionReportDao.getById(1);

        Assert.assertNull(r);
    }

    @Test
    @DirtiesContext
    public void search_ActionReportByName_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByActionName(ActionName.BLAST_uniprot);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getName().equals(ActionName.BLAST_uniprot));
    }
}
