package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.IdentificationResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.IdentificationResultsDao;
import uk.ac.ebi.intact.update.persistence.PICRReportDao;

import java.util.List;

/**
 * Unit test for PICRReportDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class PICRReportDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void search_all() throws Exception {
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        Assert.assertEquals( 0, picrReportDao.countAll() );

        PICRReport report = getMockBuilder().createPICRReport();

        picrReportDao.persist( report );
        picrReportDao.flush();
        Assert.assertEquals( 1, picrReportDao.countAll() );
    }

    @Test
    public void search_PICRReport_ByResultId_successful() throws Exception {
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, picrReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        IdentificationResults results = getMockBuilder().createUpdateResult();
        PICRReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PICRReport> r = picrReportDao.getPICRReportsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    public void search_PICRReport_ByResultId_Unsuccessful() throws Exception {
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        final IdentificationResultsDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, picrReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        IdentificationResults results = getMockBuilder().createUpdateResult();
        MappingReport report = getMockBuilder().createPICRReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PICRReport> r = picrReportDao.getPICRReportsByResultsId(1);

        Assert.assertTrue(r.isEmpty());
    }
}
