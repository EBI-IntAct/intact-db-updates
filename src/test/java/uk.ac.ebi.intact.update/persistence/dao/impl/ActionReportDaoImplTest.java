package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.ActionReportDao;
import uk.ac.ebi.intact.update.persistence.UpdateResultsDao;

import java.util.List;

/**
 * Unit test for ActionReportDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class ActionReportDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void search_all() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();
        Assert.assertEquals( 1, actionReportDao.countAll() );
    }

    @Test
    public void search_ActionReportWithId_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        long id = report.getId();

        ActionReport r = actionReportDao.getByReportId(id);

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    public void search_ActionReportWithId_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        ActionReport r = actionReportDao.getByReportId(1);

        Assert.assertNull(r);
    }

    @Test
    public void search_ActionReportByName_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getByActionName(ActionName.BLAST_uniprot);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getName().equals(ActionName.BLAST_uniprot));
    }

    @Test
    public void search_ActionReportByName_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getByActionName(ActionName.SEARCH_uniprot_name);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportByStatus_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getByReportStatus(StatusLabel.TO_BE_REVIEWED);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getStatusLabel().equals(StatusLabel.TO_BE_REVIEWED));
    }

    @Test
    public void search_ActionReportByStatus_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getByReportStatus(StatusLabel.COMPLETED);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportWithWarnings_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getWarnings().isEmpty());
    }

    @Test
    public void search_ActionReportWithWartnings_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithoutWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getAllReportsWithWarnings();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReportWithPossibleUniprot_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(!r.get(0).getPossibleAccessions().isEmpty());
    }

    @Test
    public void search_ActionReportWithPossibleUniprot_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithoutPossibleUniprot();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<ActionReport> r = actionReportDao.getAllReportsWithSeveralPossibleUniprot();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_PICRReport_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createPICRReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PICRReport> r = actionReportDao.getAllPICRReports();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_PICRReport_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<PICRReport> r = actionReportDao.getAllPICRReports();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_BlastReport_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createSwissprotRemappingReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<BlastReport> r = actionReportDao.getAllBlastReports();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_BlastReport_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createActionReportWithWarning();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<BlastReport> r = actionReportDao.getAllBlastReports();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_SwissprotRemappingReport_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createSwissprotRemappingReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<BlastReport> r = actionReportDao.getAllSwissprotRemappingReports();

        Assert.assertTrue(!r.isEmpty());
    }

    @Test
    public void search_SwissprotRemappingReport_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );

        ActionReport report = getMockBuilder().createBlastReport();

        actionReportDao.persist( report );
        actionReportDao.flush();

        List<BlastReport> r = actionReportDao.getAllSwissprotRemappingReports();

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReport_WithWarnings_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getReportsWithWarningsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_WithWarnings_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getReportsWithWarningsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReport_WithPossibleUniprot_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getReportsWithSeveralPossibleUniprotByResultId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_WithPossibleUniprot_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getReportsWithSeveralPossibleUniprotByResultId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_ActionReport_ByResultId_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getAllReportsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_ActionReport_ByResultId_Unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, actionReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<ActionReport> r = actionReportDao.getAllReportsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void test_GetActionReportByNameAndProteinAc_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByNameAndProteinAc(ActionName.PICR_accession, "EBI-0001001");

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals(ActionName.PICR_accession, list.get(0).getName());
    }

    @Test
    public void test_GetActionReportsByNameAndProteinAc_unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByNameAndProteinAc(ActionName.PICR_sequence_Swissprot, "EBI-0001001");

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetActionReportByNameAndResultId_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        long id = results.getId();

        List<ActionReport> list = actionReportDao.getActionReportsByNameAndResultId(ActionName.PICR_accession, id);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertNotNull(list.get(0).getUpdateResult());
        Assert.assertTrue(list.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void test_GetUActionReportsByNameAndResultId_unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByNameAndResultId(ActionName.PICR_accession, 1);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetActionReportByStatusAndProteinAc_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByStatusAndProteinAc(StatusLabel.COMPLETED, "EBI-0001001");

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals(StatusLabel.COMPLETED, list.get(0).getStatusLabel());
    }

    @Test
    public void test_GetUActionReportsByStatusAndProteinAc_unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByStatusAndProteinAc(StatusLabel.TO_BE_REVIEWED, "EBI-0001001");

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetActionReportByStatusAndResultId_successful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        long id = results.getId();

        List<ActionReport> list = actionReportDao.getActionReportsByStatusAndResultId(StatusLabel.COMPLETED, id);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertNotNull(list.get(0).getUpdateResult());
        Assert.assertTrue(list.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void test_GetUActionReportsByStatusAndResultId_unsuccessful() throws Exception {
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, actionReportDao.countAll() );
        final UpdateResultsDao updateResultsdao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsdao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsdao.persist( results );
        updateResultsdao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsByStatusAndResultId(StatusLabel.COMPLETED, 1);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetReportsWithWarningsByProteinAc_successful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsWithWarningsByProteinAc("EBI-0001001");

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals(list.get(0).getUpdateResult().getIntactAccession(), "EBI-0001001");
    }

    @Test
    public void test_GetReportsWithWarningsByProteinAc_unsuccessful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        final ActionReportDao<ActionReport> actionReportDao = getDaoFactory().getActionReportDao(ActionReport.class);
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createActionReportWithoutWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<ActionReport> list = actionReportDao.getActionReportsWithWarningsByProteinAc("EBI-0001001");

        Assert.assertTrue(list.isEmpty());
    }
}
