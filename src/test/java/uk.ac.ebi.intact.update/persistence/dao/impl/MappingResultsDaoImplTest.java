package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.UpdateMappingDao;

import java.util.List;

/**
 * Unit test for UpdateMappingDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class MappingResultsDaoImplTest extends UpdateBasicTestCase{

    @Test
    public void search_all() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();
        Assert.assertEquals( 1, updateResultDao.countAll() );
    }

    @Test
    public void test_GetUpdateResultsWithId_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        long id = results.getId();

        UpdateMappingResults r = updateResultDao.getUpdateResultsWithId(id);

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    public void test_GetUpdateResultsWithId_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        UpdateMappingResults r = updateResultDao.getUpdateResultsWithId(1);

        Assert.assertNull(r);
    }

    @Test
    public void test_GetUpdateResultsForProteinAc_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        long id = results.getId();

        UpdateMappingResults r = updateResultDao.getUpdateResultsForProteinAc("EBI-0001001");

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    public void test_GetUpdateResultsForProteinAc_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        UpdateMappingResults r = updateResultDao.getUpdateResultsForProteinAc("ebi_268");

        Assert.assertNull(r);
    }

    @Test
    public void test_GetResultsContainingActionWithSpecificName_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getResultsContainingAction(ActionName.BLAST_uniprot);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals(list.get(0).getActionsByName(ActionName.BLAST_uniprot).size(), 1);
    }

    @Test
    public void test_GetResultsContainingActionWithSpecificName_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getResultsContainingAction(ActionName.BLAST_Swissprot_Total_Identity);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResultsContainingActionWithSpecificLabel_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getResultsContainingActionWithLabel(StatusLabel.TO_BE_REVIEWED);

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsContainingActionWithSpecificLabel_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getResultsContainingActionWithLabel(StatusLabel.COMPLETED);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResultsContainingSwissprotRemapping_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithSwissprotRemapping();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsContainingSwissprotRemapping_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithSwissprotRemapping();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResults_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getSuccessfulUpdateResults();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_NoSuccessfulresults() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getSuccessfulUpdateResults();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResultsToBeReviewed_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        MappingReport report = getMockBuilder().createActionReportWithWarning();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);
        results.addActionReport(picr);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsToBeReviewedByACurator();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsToBeReviewed_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsToBeReviewedByACurator();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResultsWithNoSequenceNoIdentityXRefs_successful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        MappingReport report = getMockBuilder().createUpdateReportWithNoSequenceNoIdentityXRef();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);
        results.addActionReport(picr);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsWithNoSequenceNoIdentityXRefs_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<UpdateMappingResults> list = updateResultDao.getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetUnsuccessfulResults() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        MappingReport report = getMockBuilder().createReportWithStatusFailed();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUnsuccessfulUpdateResults();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetNoUnsuccessfulResults() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<UpdateMappingResults> list = updateResultDao.getUnsuccessfulUpdateResults();

        Assert.assertTrue(list.isEmpty());
    }


    @Test
    public void test_GetResultsWithConflictSequenceIdentityXRefs() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        MappingReport report = getMockBuilder().createUpdateReportWithConflict();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithConflictsBetweenActions();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsWithConflictSequenceIdentityXRefs_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithConflictsBetweenActions();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    public void test_GetResultsWithConflictSequencefeatureRange() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        MappingReport report = getMockBuilder().createFeatureRangeCheckingReportWithConflict();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    public void test_GetResultsWithConflictSequenceFeatureRange_unsuccessful() throws Exception {
        final UpdateMappingDao updateResultDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        UpdateMappingResults results = getMockBuilder().createUpdateResult();
        MappingReport picr = getMockBuilder().createPICRReport();
        BlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<UpdateMappingResults> list = updateResultDao.getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();

        Assert.assertTrue(list.isEmpty());
    }
}
