package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.IdentificationResultsDao;
import uk.ac.ebi.intact.update.persistence.MappingReportDao;

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
    public void search_all() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();
        Assert.assertEquals( 1, actionReportDao.countAll() );
    }

    @Test
    public void search_ActionReportWithId_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
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
    public void search_ActionReportWithId_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        PersistentMappingReport r = actionReportDao.getById(1);

        Assert.assertNull(r);
    }

    @Test
    public void search_ActionReportByName_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByActionName(ActionName.BLAST_uniprot);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getName().equals(ActionName.BLAST_uniprot));
    }

    @Test
    public void search_ActionReportByName_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByActionName(ActionName.SEARCH_uniprot_name);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportByStatus_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByReportStatus(StatusLabel.TO_BE_REVIEWED);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getStatusLabel().equals(StatusLabel.TO_BE_REVIEWED));
    }

    @Test
    public void search_ActionReportByStatus_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getByReportStatus(StatusLabel.COMPLETED);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportWithWarnings_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getWarnings().isEmpty());
    }

    @Test
    public void search_ActionReportWithWartnings_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithoutWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportWithPossibleUniprot_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getPossibleAccessions().isEmpty());
    }

    @Test
    public void search_ActionReportWithPossibleUniprot_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithoutPossibleUniprot();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_PICRReport_successful() throws Exception {
        final MappingReportDao<PersistentPICRReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentPICRReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentPICRReport report = getMockBuilder().createPICRReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentPICRReport> r = actionReportDao.getAll();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_PICRReport_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentPICRReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentPICRReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        getDaoFactory().getMappingReportDao(PersistentMappingReport.class).persist( report );

        List<PersistentPICRReport> r = actionReportDao.getAll();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_BlastReport_successful() throws Exception {
        final MappingReportDao<PersistentBlastReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentBlastReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentBlastReport report = getMockBuilder().createSwissprotRemappingReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentBlastReport> r = actionReportDao.getAll();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_BlastReport_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentBlastReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentBlastReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        getDaoFactory().getMappingReportDao(PersistentMappingReport.class).persist( report );
        actionReportDao.flush();

        List<PersistentBlastReport> r = actionReportDao.getAll();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_SwissprotRemappingReport_successful() throws Exception {
        final MappingReportDao<PersistentBlastReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentBlastReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        PersistentBlastReport report = getMockBuilder().createSwissprotRemappingReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PersistentBlastReport> r = actionReportDao.getAll();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_ActionReport_WithWarnings_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getReportsWithWarningsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_WithWarnings_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getReportsWithWarningsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReport_WithPossibleUniprot_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getReportsWithSeveralPossibleUniprotByResultId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_WithPossibleUniprot_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getReportsWithSeveralPossibleUniprotByResultId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReport_ByResultId_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_ByResultId_Unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentMappingReport> r = actionReportDao.getAllReportsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void test_GetActionReportByNameAndResultId_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final IdentificationResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        long id = results.getId();

        List<PersistentMappingReport> list = actionReportDao.getActionReportsByNameAndResultId(ActionName.PICR_accession, id);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertNotNull(list.get(0).getUpdateResult());
        Assert.assertTrue(list.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void test_GetUActionReportsByNameAndResultId_unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final IdentificationResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<PersistentMappingReport> list = actionReportDao.getActionReportsByNameAndResultId(ActionName.PICR_accession, 1);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetActionReportByStatusAndResultId_successful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final IdentificationResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        long id = results.getId();

        List<PersistentMappingReport> list = actionReportDao.getActionReportsByStatusAndResultId(StatusLabel.COMPLETED, id);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertNotNull(list.get(0).getUpdateResult());
        Assert.assertTrue(list.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void test_GetUActionReportsByStatusAndResultId_unsuccessful() throws Exception {
        final MappingReportDao<PersistentMappingReport> actionReportDao = getDaoFactory().getMappingReportDao(PersistentMappingReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final IdentificationResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<PersistentMappingReport> list = actionReportDao.getActionReportsByStatusAndResultId(StatusLabel.COMPLETED, 1);

        Assert.assertTrue(list.isEmpty());
    }
}
