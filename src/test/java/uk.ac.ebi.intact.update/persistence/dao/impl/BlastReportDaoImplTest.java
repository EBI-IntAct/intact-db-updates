package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.BlastReportDao;
import uk.ac.ebi.intact.update.persistence.UpdateResultsDao;

import java.util.List;

/**
 * Unit test for BlastReportDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class BlastReportDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void search_all() throws Exception {
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        BlastReport report = getMockBuilder().createBlastReport();

        blastReportDao.persist( report );
        blastReportDao.flush();
        Assert.assertEquals( 1, blastReportDao.countAll() );
    }

    @Test
    public void search_BlastReport_ByResultId_successful() throws Exception {
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<BlastReport> r = blastReportDao.getBlastReportsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_BlastReport_ByResultId_Unsuccessful() throws Exception {
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        final UpdateResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        ActionReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        List<BlastReport> r = blastReportDao.getBlastReportsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void test_GetBlastReportByProteinAc_successful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<BlastReport> list = blastReportDao.getActionReportsWithBlastResultsByProteinAc("EBI-0001001");

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals("EBI-0001001", list.get(0).getUpdateResult().getIntactAccession());
    }

    @Test
    public void test_GetBlastReportByProteinAc_unsuccessful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<BlastReport> list = blastReportDao.getActionReportsWithBlastResultsByProteinAc("EBI-11212");

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetSwissprotRemappingReportByProteinAc_successful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createSwissprotRemappingReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<BlastReport> list = blastReportDao.getActionReportsWithSwissprotRemappingResultsByProteinAc("EBI-0001001");

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals("EBI-0001001", list.get(0).getUpdateResult().getIntactAccession());
    }

    @Test
    public void test_GetSwissprotRemappingReportByProteinAc_unsuccessful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createSwissprotRemappingReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<BlastReport> list = blastReportDao.getActionReportsWithSwissprotRemappingResultsByProteinAc("EBI-01234");

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetSwissprotRemappingReportByResultId_successful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createSwissprotRemappingReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        long id = results.getId();

        List<BlastReport> list = blastReportDao.getActionReportsWithSwissprotRemappingResultsByResultsId(id);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertTrue(list.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void test_GetSwissprotRemappingReportByResultId_unsuccessful() throws Exception {
        final UpdateResultsDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createSwissprotRemappingReport();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<BlastReport> list = blastReportDao.getActionReportsWithSwissprotRemappingResultsByResultsId(1);

        Assert.assertTrue(list.isEmpty());
    }
}
