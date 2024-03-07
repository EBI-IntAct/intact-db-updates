package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentUniprotProteinAPIReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.IdentificationResultsDao;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinAPIReportDao;

import java.util.List;

/**
 * Unit test for UniprotProteinAPIReportDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */
@ContextConfiguration(locations = {
        "classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/update-jpa.spring.xml",
        "classpath*:/META-INF/db-update-test.spring.xml"
})
public class UniprotProteinAPIReportDaoImplTest extends UpdateBasicTestCase {

    @Test
    @DirtiesContext
    public void search_all() throws Exception {
        final UniprotProteinAPIReportDao uniprotProteinAPIReportDao = getUpdateDaoFactory().getUniprotProteinAPIReportDao();
        Assert.assertEquals( 0, uniprotProteinAPIReportDao.countAll() );

        PersistentUniprotProteinAPIReport report = getMockBuilder().createUniprotProteinAPIReport();

        uniprotProteinAPIReportDao.persist( report );
        uniprotProteinAPIReportDao.flush();
        Assert.assertEquals( 1, uniprotProteinAPIReportDao.countAll() );
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPIReport_ByResultId_successful() throws Exception {
        final UniprotProteinAPIReportDao uniprotProteinAPIReportDao = getUpdateDaoFactory().getUniprotProteinAPIReportDao();
        final IdentificationResultsDao updateResultsDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, uniprotProteinAPIReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentUniprotProteinAPIReport report = getMockBuilder().createUniprotProteinAPIReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentUniprotProteinAPIReport> r = uniprotProteinAPIReportDao.getUniprotProteinAPIReportsByResultsId(id);

        Assert.assertTrue(!r.isEmpty());
        Assert.assertTrue(r.get(0).getUpdateResult() != null);
        Assert.assertTrue(r.get(0).getUpdateResult().getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPIReport_ByResultId_Unsuccessful() throws Exception {
        final UniprotProteinAPIReportDao uniprotProteinAPIReportDao = getUpdateDaoFactory().getUniprotProteinAPIReportDao();
        final IdentificationResultsDao updateResultsDao = getUpdateDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, uniprotProteinAPIReportDao.countAll() );
        Assert.assertEquals( 0, updateResultsDao.countAll() );

        PersistentIdentificationResults results = getMockBuilder().createUpdateResult();
        PersistentMappingReport report = getMockBuilder().createUniprotProteinAPIReport();
        results.addActionReport(report);

        updateResultsDao.persist( results );
        updateResultsDao.flush();

        long id = results.getId();

        List<PersistentUniprotProteinAPIReport> r = uniprotProteinAPIReportDao.getUniprotProteinAPIReportsByResultsId(-1);

        Assert.assertTrue(r.isEmpty());
    }
}
