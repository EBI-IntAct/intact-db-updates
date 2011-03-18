package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.PICRCrossReferencesDao;
import uk.ac.ebi.intact.update.persistence.PICRReportDao;
import uk.ac.ebi.intact.update.persistence.UpdateMappingDao;

import java.util.List;

/**
 * Unit test for PICRCrossReferenceDaoImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */

public class PICRCrossReferenceDaoImplTest extends UpdateBasicTestCase {

    @Test
    public void search_all() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();
        Assert.assertEquals( 1, picrCrossReferenceDoa.countAll() );
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();


        List<PICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseName("Ensembl");

        Assert.assertTrue(!picrResults.isEmpty());
        Assert.assertEquals("Ensembl", picrResults.iterator().next().getDatabase());
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_Unsuccessful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();


        List<PICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseName("Uniprot");

        Assert.assertTrue(picrResults.isEmpty());
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_and_ActionId_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );
        Assert.assertEquals( 0, picrReportDao.countAll() );

        PICRReport report = getMockBuilder().createPICRReport();
        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);

        picrReportDao.persist( report );
        picrReportDao.flush();

        long id = report.getId();

        List<PICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", id);

        Assert.assertTrue(!picrResults.isEmpty());
        Assert.assertNotNull(picrResults.get(0).getPicrReport());
        Assert.assertTrue(picrResults.get(0).getPicrReport().getId() == id);
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_and_ActionId_Unsuccessful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );
        Assert.assertEquals( 0, picrReportDao.countAll() );

        PICRReport report = getMockBuilder().createPICRReport();
        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);

        picrReportDao.persist( report );
        picrReportDao.flush();

        long id = report.getId();

        List<PICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", 1);

        Assert.assertTrue(picrResults.isEmpty());
    }

    @Test
    public void search_PICRCrossReferences_ById_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();

        long id = picrRefs.getId();

        PICRCrossReferences picrResults = picrCrossReferenceDoa.getById(id);

        Assert.assertNotNull(picrResults);
        Assert.assertTrue(picrResults.getId() == id);
    }

    @Test
    public void search_PICRCrossReferences_ById_Unsuccessful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();

        PICRCrossReferences picrResults = picrCrossReferenceDoa.getById(1);

        Assert.assertNull(picrResults);
    }

    @Test
    public void search_Picr_references_By_ProteinAc_successful() throws Exception {
        final UpdateMappingDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );
        final PICRCrossReferencesDao picrCrossReferencesDao = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferencesDao.countAll() );

        UpdateMappingResults update = getMockBuilder().createUpdateResult();
        PICRReport report = getMockBuilder().createPICRReport();
        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);
        update.addActionReport(report);

        updateResultsDao.persist( update );
        updateResultsDao.flush();

        List<PICRCrossReferences> results = picrCrossReferencesDao.getCrossReferencesByProteinAc("EBI-0001001");

        Assert.assertTrue(!results.isEmpty());
    }

    @Test
    public void search_Picr_references_By_ProteinAc_Unsuccessful() throws Exception {
        final UpdateMappingDao updateResultsDao = getDaoFactory().getUpdateResultsDao();
        Assert.assertEquals( 0, updateResultsDao.countAll() );
        final PICRCrossReferencesDao picrCrossReferencesDao = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferencesDao.countAll() );

        UpdateMappingResults update = getMockBuilder().createUpdateResult();
        PICRReport report = getMockBuilder().createPICRReport();
        PICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);
        update.addActionReport(report);

        updateResultsDao.persist( update );
        updateResultsDao.flush();

        List<PICRCrossReferences> results = picrCrossReferencesDao.getCrossReferencesByProteinAc("EBI-01234");

        Assert.assertTrue(results.isEmpty());
    }
}
