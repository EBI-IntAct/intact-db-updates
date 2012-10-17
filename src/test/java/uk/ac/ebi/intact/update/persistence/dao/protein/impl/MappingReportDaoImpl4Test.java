package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.MappingReportDao;

import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/10/12</pre>
 */

public class MappingReportDaoImpl4Test extends UpdateBasicTestCase {

    @Test
    @DirtiesContext
    public void search_ActionReportWithWartnings_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals(0, actionReportDao.countAll());

        PersistentMappingReport report = getMockBuilder().createActionReportWithoutWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_ActionReportWithPossibleUniprot_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getPossibleAccessions().isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_ActionReportWithPossibleUniprot_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithoutPossibleUniprot();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_PICRReport_successful() throws Exception {
        final MappingReportDao<PersistentPICRReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentPICRReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentPICRReport report = getMockBuilder().createPICRReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentPICRReport> r = actionReportDao.getAll();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_PICRReport_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentPICRReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentPICRReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        getUpdateDaoFactory().getMappingReportDao(PersistentMappingReport.class).persist( report );

        List<PersistentPICRReport> r = actionReportDao.getAll();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_BlastReport_successful() throws Exception {
        final MappingReportDao<PersistentBlastReport> actionReportDao = getUpdateDaoFactory().getMappingReportDao(PersistentBlastReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentBlastReport report = getMockBuilder().createSwissprotRemappingReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentBlastReport> r = actionReportDao.getAll();

        Assert.assertTrue(!r.isEmpty());
    }
}
