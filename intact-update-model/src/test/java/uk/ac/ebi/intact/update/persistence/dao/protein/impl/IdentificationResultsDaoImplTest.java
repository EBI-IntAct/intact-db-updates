package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.IdentificationResultsDao;

import java.util.List;

/**
 * Unit test for IdentificationResultsDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class IdentificationResultsDaoImplTest extends UpdateBasicTestCase{

    @Test
    @DirtiesContext
    public void search_all() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();
        Assert.assertEquals( 1, updateResultDao.countAll() );
    }

    @Test
    @DirtiesContext
    public void test_GetUpdateResultsWithId_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        long id = results.getId();

        PersistentIdentificationResults r = updateResultDao.getById(id);

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    @DirtiesContext
    public void test_GetUpdateResultsWithId_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        PersistentIdentificationResults r = updateResultDao.getById(1);

        Assert.assertNull(r);
    }

    @Test
    @DirtiesContext
    public void test_GetUpdateResults_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();

        updateResultDao.persist( results );
        updateResultDao.flush();

        long id = results.getId();

        PersistentIdentificationResults r = updateResultDao.getById(results.getId());

        Assert.assertNotNull(r);
        Assert.assertTrue(r.getId() == id);
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingActionWithSpecificName_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getResultsContainingAction(ActionName.BLAST_uniprot);

        Assert.assertTrue(!list.isEmpty());
        Assert.assertEquals(list.get(0).getActionsByName(ActionName.BLAST_uniprot).size(), 1);
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingActionWithSpecificName_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getResultsContainingAction(ActionName.BLAST_Swissprot_Total_Identity);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingActionWithSpecificLabel_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getResultsContainingActionWithLabel(StatusLabel.TO_BE_REVIEWED);

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingActionWithSpecificLabel_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getResultsContainingActionWithLabel(StatusLabel.COMPLETED);

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingSwissprotRemapping_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithSwissprotRemapping();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsContainingSwissprotRemapping_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithSwissprotRemapping();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResults_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getSuccessfulUpdateResults();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_NoSuccessfulresults() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getSuccessfulUpdateResults();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsToBeReviewed_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentMappingReport report = getMockBuilder().createActionReportWithWarning();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);
        results.addActionReport(picr);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsToBeReviewedByACurator();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsToBeReviewed_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsToBeReviewedByACurator();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsWithNoSequenceNoIdentityXRefs_successful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentMappingReport report = getMockBuilder().createUpdateReportWithNoSequenceNoIdentityXRef();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(report);
        results.addActionReport(remapping);
        results.addActionReport(picr);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsWithNoSequenceNoIdentityXRefs_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<PersistentIdentificationResults> list = updateResultDao.getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetUnsuccessfulResults() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        PersistentMappingReport report = getMockBuilder().createReportWithStatusFailed();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUnsuccessfulUpdateResults();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetNoUnsuccessfulResults() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<PersistentIdentificationResults> list = updateResultDao.getUnsuccessfulUpdateResults();

        Assert.assertTrue(list.isEmpty());
    }


    @Test
    @DirtiesContext
    public void test_GetResultsWithConflictSequenceIdentityXRefs() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        PersistentMappingReport report = getMockBuilder().createUpdateReportWithConflict();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithConflictsBetweenActions();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsWithConflictSequenceIdentityXRefs_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithConflictsBetweenActions();

        Assert.assertTrue(list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsWithConflictSequencefeatureRange() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUnsuccessfulUpdateResult();
        PersistentMappingReport report = getMockBuilder().createFeatureRangeCheckingReportWithConflict();

        results.addActionReport(report);

        updateResultDao.persist( results );
        updateResultDao.flush();

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();

        Assert.assertTrue(!list.isEmpty());
    }

    @Test
    @DirtiesContext
    public void test_GetResultsWithConflictSequenceFeatureRange_unsuccessful() throws Exception {
        final IdentificationResultsDao updateResultDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport picr = getMockBuilder().createPICRReport();
        PersistentBlastReport remapping = getMockBuilder().createSwissprotRemappingReport();

        results.addActionReport(remapping);
        results.addActionReport(picr);

        List<PersistentIdentificationResults> list = updateResultDao.getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();

        Assert.assertTrue(list.isEmpty());
    }
}
