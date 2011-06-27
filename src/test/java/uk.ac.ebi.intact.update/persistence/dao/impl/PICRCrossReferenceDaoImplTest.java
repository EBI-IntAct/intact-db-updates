package uk.ac.ebi.intact.update.persistence.dao.impl;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentPICRCrossReferences;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.PICRCrossReferencesDao;
import uk.ac.ebi.intact.update.persistence.PICRReportDao;

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

        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();
        Assert.assertEquals( 1, picrCrossReferenceDoa.countAll() );
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();


        List<PersistentPICRCrossReferences> picrResults = picrCrossReferenceDoa.getAllCrossReferencesByDatabaseName("Ensembl");

        Assert.assertTrue(!picrResults.isEmpty());
        Assert.assertEquals("Ensembl", picrResults.iterator().next().getDatabase());
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_Unsuccessful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();


        List<PersistentPICRCrossReferences> picrResults = picrCrossReferenceDoa.getAllCrossReferencesByDatabaseName("Uniprot");

        Assert.assertTrue(picrResults.isEmpty());
    }

    @Test
    public void search_PICRCrossReferences_ByDatabaseName_and_ActionId_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        final PICRReportDao picrReportDao = getDaoFactory().getPICRReportDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );
        Assert.assertEquals( 0, picrReportDao.countAll() );

        PersistentPICRReport report = getMockBuilder().createPICRReport();
        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);

        picrReportDao.persist( report );
        picrReportDao.flush();

        long id = report.getId();

        List<PersistentPICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", id);

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

        PersistentPICRReport report = getMockBuilder().createPICRReport();
        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();
        report.addPICRCrossReference(picrRefs);

        picrReportDao.persist( report );
        picrReportDao.flush();

        long id = report.getId();

        List<PersistentPICRCrossReferences> picrResults = picrCrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", 1);

        Assert.assertTrue(picrResults.isEmpty());
    }

    @Test
    public void search_PICRCrossReferences_ById_successful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();

        long id = picrRefs.getId();

        PersistentPICRCrossReferences picrResults = picrCrossReferenceDoa.getById(id);

        Assert.assertNotNull(picrResults);
        Assert.assertTrue(picrResults.getId() == id);
    }

    @Test
    public void search_PICRCrossReferences_ById_Unsuccessful() throws Exception {
        final PICRCrossReferencesDao picrCrossReferenceDoa = getDaoFactory().getPicrCrossReferencesDao();
        Assert.assertEquals( 0, picrCrossReferenceDoa.countAll() );

        PersistentPICRCrossReferences picrRefs = getMockBuilder().createPICRCrossReferences();

        picrCrossReferenceDoa.persist( picrRefs );
        picrCrossReferenceDoa.flush();

        PersistentPICRCrossReferences picrResults = picrCrossReferenceDoa.getById(1);

        Assert.assertNull(picrResults);
    }
}
