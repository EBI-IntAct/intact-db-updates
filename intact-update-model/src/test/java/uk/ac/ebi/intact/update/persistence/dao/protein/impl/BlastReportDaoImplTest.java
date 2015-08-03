package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.BlastReportDao;
import uk.ac.ebi.intact.update.persistence.dao.protein.IdentificationResultsDao;

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
    @DirtiesContext
    public void search_all() throws Exception {
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );

        PersistentBlastReport report = getMockBuilder().createBlastReport();

        blastReportDao.persist( report );
        blastReportDao.flush();
        Assert.assertEquals( 1, blastReportDao.countAll() );
    }

    @Test
    @DirtiesContext
    public void search_BlastReport_ByResultId_successful() throws Exception {
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        final IdentificationResultsDao updateResultsDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentBlastReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        Assert.assertEquals( 1, blastReportDao.countAll() );

        long id = results.getId();

        List<PersistentBlastReport> r = blastReportDao.getByResultsId(id);

        Assert.assertFalse(r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_BlastReport_ByResultId_Unsuccessful() throws Exception {
        final BlastReportDao blastReportDao = getUpdateDaoFactory().getBlastReportDao();
        final IdentificationResultsDao updateResultsDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, blastReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createBlastReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        List<PersistentBlastReport> r = blastReportDao.getByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }
}
