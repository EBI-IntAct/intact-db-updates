package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentBlastResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.BlastReportDao;
import uk.ac.ebi.intact.update.persistence.dao.protein.BlastResultsDao;

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
    @DirtiesContext
    public void search_all() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();
        Assert.assertEquals( 1, blastResultDao.countAll() );
    }

    @Test
    @DirtiesContext
    public void search_identity_superior_98_successful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> r = blastResultDao.getByIdentitySuperiorTo((float) 98);

        Assert.assertEquals(1, r.size());
        Assert.assertTrue(r.get(0).getIdentity() >= 98);
    }

    @Test
    @DirtiesContext
    public void search_identity_superior_98_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults("AXZ089", "P01234", 1, 198, 1, 198, (float) 96);

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> r = blastResultDao.getByIdentitySuperiorTo((float) 98);

        Assert.assertTrue(r.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_identity_superior_98_and_actionId_successful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        PersistentBlastReport r = getMockBuilder().createSwissprotRemappingReport();
        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<PersistentBlastResults> results = blastResultDao.getByActionIdAndIdentitySuperiorTo((float) 98, id);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getIdentity() >= 98);
        Assert.assertNotNull(results.get(0).getBlastReport());
        Assert.assertEquals(results.get(0).getBlastReport().getId(), id);
    }

    @Test
    @DirtiesContext
    public void search_identity_superior_98_and_actionId_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        PersistentBlastReport r = getMockBuilder().createSwissprotRemappingReport();
        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<PersistentBlastResults> results = blastResultDao.getByActionIdAndIdentitySuperiorTo((float) 98, 1);

        Assert.assertEquals(0, results.size());
    }

    @Test
    @DirtiesContext
    public void search_all_swissprot_remapping_results() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> results = blastResultDao.getAllSwissprotRemappingResults();

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession()!= null);
    }

    @Test
    @DirtiesContext
    public void search_all_swissprot_remapping_results_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createBlastResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> results = blastResultDao.getAllSwissprotRemappingResults();

        Assert.assertEquals(0, results.size());
    }

    @Test
    @DirtiesContext
    public void search_swissprotRemapping_resuts_and_actionId_successful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        PersistentBlastReport r = getMockBuilder().createSwissprotRemappingReport();
        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<PersistentBlastResults> results = blastResultDao.getAllSwissprotRemappingResultsFor(id);

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession() != null);
        Assert.assertNotNull(results.get(0).getBlastReport());
        Assert.assertEquals(results.get(0).getBlastReport().getId(), id);
    }

    @Test
    @DirtiesContext
    public void search_swissprotRemapping_resuts_and_actionId_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );
        Assert.assertEquals( 0, blastReportDao.countAll() );

        PersistentBlastReport r = getMockBuilder().createSwissprotRemappingReport();
        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();
        r.addBlastMatchingProtein(blastResults);

        blastReportDao.persist( r );
        blastResultDao.flush();

        Long id = r.getId();

        List<PersistentBlastResults> results = blastResultDao.getAllSwissprotRemappingResultsFor(1);

        Assert.assertEquals(0, results.size());
    }

    @Test
    @DirtiesContext
    public void search_swissprotRemapping_resuts_By_Trembl_ac_successful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> results = blastResultDao.getSwissprotRemappingResultsByTremblAc("Q8R3H6");

        Assert.assertEquals(1, results.size());
        Assert.assertTrue(results.get(0).getTremblAccession().equalsIgnoreCase("Q8R3H6"));
    }

    @Test
    @DirtiesContext
    public void search_swissprotRemapping_resuts_By_Trembl_ac_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        List<PersistentBlastResults> results = blastResultDao.getSwissprotRemappingResultsByTremblAc("XXXXX");

        Assert.assertEquals(0, results.size());
    }

    @Test
    @DirtiesContext
    public void search_blast_resuts_By_Id_successful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        long id = blastResults.getId();

        PersistentBlastResults results = blastResultDao.getById(id);

        Assert.assertNotNull(results);
        Assert.assertTrue(results.getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_blast_resuts_By_Id_Unsuccessful() throws Exception {
        final BlastResultsDao blastResultDao = getUpdateDaoFactory().getBlastResultsDao();
        Assert.assertEquals( 0, blastResultDao.countAll() );

        PersistentBlastResults blastResults = getMockBuilder().createSwissprotRemappingResults();

        blastResultDao.persist( blastResults );
        blastResultDao.flush();

        PersistentBlastResults results = blastResultDao.getById(1);

        Assert.assertNull(results);
    }
}
