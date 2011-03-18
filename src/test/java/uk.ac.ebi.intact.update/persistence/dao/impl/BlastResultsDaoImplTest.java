package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.BlastResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.BlastReportDao;
import uk.ac.ebi.intact.update.persistence.BlastResultsDao;
import uk.ac.ebi.intact.update.persistence.UpdateMappingDao;

import java.util.List;

/**
 * Unit test for BlastResultsDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */

public class BlastResultsDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void search_all() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();
        Assert.assertEquals( 1, blastResultDao.countAll() );
    }

    @Test
    public void search_identity_superior_98_successful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> r = blastResultDao.getResultsByIdentitySuperior((float)98);

        Assert.assertEquals(1, r.size());
        Assert.assertTrue(r.get(0).getIdentity() >= 98);
    }

    @Test
    public void search_identity_superior_98_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults("AXZ089", "P01234", 1, 198, 1, 198, (float) 96);

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> r = blastResultDao.getResultsByIdentitySuperior((float)98);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    public void search_identity_superior_98_and_actionId_successful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        BlastReport r = getMockBuilder().createSwissprotRemappingReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<BlastResults> results = blastResultDao.getResultsByActionIdAndIdentitySuperior((float)98, id);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getIdentity() >= 98);
        Assert.assertNotNull(results.get(0).getBlastReport());
        Assert.assertEquals(results.get(0).getBlastReport().getId(), id);
    }

    @Test
    public void search_identity_superior_98_and_actionId_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        BlastReport r = getMockBuilder().createSwissprotRemappingReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<BlastResults> results = blastResultDao.getResultsByActionIdAndIdentitySuperior((float)98, 1);

        Assert.assertEquals(0, results.size());
    }

    @Test
    public void search_all_swissprot_remapping_results() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> results = blastResultDao.getAllSwissprotRemappingResults();

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession()!= null);
    }

    @Test
    public void search_all_swissprot_remapping_results_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createBlastResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> results = blastResultDao.getAllSwissprotRemappingResults();

        Assert.assertEquals(0, results.size());
    }

    @Test
    public void search_swissprotRemapping_resuts_and_actionId_successful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        BlastReport r = getMockBuilder().createSwissprotRemappingReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<BlastResults> results = blastResultDao.getAllSwissprotRemappingResultsFor(id);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession() != null);
        Assert.assertNotNull(results.get(0).getBlastReport());
        Assert.assertEquals(results.get(0).getBlastReport().getId(), id);
    }

    @Test
    public void search_swissprotRemapping_resuts_and_actionId_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        BlastReport r = getMockBuilder().createSwissprotRemappingReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<BlastResults> results = blastResultDao.getAllSwissprotRemappingResultsFor(1);

        Assert.assertEquals(0, results.size());
    }

    @Test
    public void search_swissprotRemapping_resuts_By_Trembl_ac_successful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> results = blastResultDao.getSwissprotRemappingResultsByTremblAc("Q8R3H6");

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession().equalsIgnoreCase("Q8R3H6"));
    }

    @Test
    public void search_swissprotRemapping_resuts_By_Trembl_ac_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<BlastResults> results = blastResultDao.getSwissprotRemappingResultsByTremblAc("XXXXX");

        Assert.assertEquals(0, results.size());
    }

    @Test
    public void search_blast_resuts_By_Id_successful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        long id = blastResults.getId();

        BlastResults results = blastResultDao.getById(id);

        Assert.assertNotNull(results);
        Assert.assertTrue(results.getId() == id);
    }

    @Test
    public void search_blast_resuts_By_Id_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        BlastResults results = blastResultDao.getById(1);

        Assert.assertNull(results);
    }

    @Test
    public void search_blast_results_By_ProteinAc_successful() throws Exception {
        final UpdateMappingDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );
        final BlastResultsDao blastResultsDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateMappingResults update = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createBlastReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        report.addBlastMatchingProtein(blastResults);
        update.addActionReport(report);

        updateResultsDao.persist( update );
        updateResultsDao.flush();

        List<BlastResults> results = blastResultsDao.getBlastResultsByProteinAc("EBI-0001001");

        Assert.assertTrue(!results.isEmpty());
    }

    @Test
    public void search_blast_results_By_ProteinAc_Unsuccessful() throws Exception {
        final UpdateMappingDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );
        final BlastResultsDao blastResultsDao = getDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        UpdateMappingResults update = getMockBuilder().createUpdateResult();
        BlastReport report = getMockBuilder().createBlastReport();
        BlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        report.addBlastMatchingProtein(blastResults);
        update.addActionReport(report);

        updateResultsDao.persist( update );
        updateResultsDao.flush();

        List<BlastResults> results = blastResultsDao.getBlastResultsByProteinAc("EBI-000123");

        Assert.assertTrue(results.isEmpty());
    }
}
