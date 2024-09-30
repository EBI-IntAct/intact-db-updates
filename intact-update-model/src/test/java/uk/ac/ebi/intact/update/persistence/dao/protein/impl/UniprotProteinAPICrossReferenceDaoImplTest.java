package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import junit.framework.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentUniprotProteinAPIReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentUniprotProteinAPICrossReferences;
import uk.ac.ebi.intact.update.model.unit.UpdateBasicTestCase;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinAPICrossReferencesDao;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinAPIReportDao;

import java.util.List;

/**
 * Unit test for UniprotProteinAPICrossReferenceDaoImpl
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
public class UniprotProteinAPICrossReferenceDaoImplTest extends UpdateBasicTestCase {

    @Test
    @DirtiesContext
    public void search_all() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();

        uniprotProteinAPICrossReferenceDoa.persist( uniprotProteinAPIRefs );
        uniprotProteinAPICrossReferenceDoa.flush();
        Assert.assertEquals( 1, uniprotProteinAPICrossReferenceDoa.countAll() );
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ByDatabaseName_successful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();

        uniprotProteinAPICrossReferenceDoa.persist( uniprotProteinAPIRefs );
        uniprotProteinAPICrossReferenceDoa.flush();


        List<PersistentUniprotProteinAPICrossReferences> uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getAllCrossReferencesByDatabaseName("Ensembl");

        Assert.assertTrue(!uniprotProteinAPIResults.isEmpty());
        Assert.assertEquals("Ensembl", uniprotProteinAPIResults.iterator().next().getDatabase());
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ByDatabaseName_Unsuccessful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();

        uniprotProteinAPICrossReferenceDoa.persist( uniprotProteinAPIRefs );
        uniprotProteinAPICrossReferenceDoa.flush();


        List<PersistentUniprotProteinAPICrossReferences> uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getAllCrossReferencesByDatabaseName("Uniprot");

        Assert.assertTrue(uniprotProteinAPIResults.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ByDatabaseName_and_ActionId_successful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        final UniprotProteinAPIReportDao uniprotProteinAPIReportDao = getUpdateDaoFactory().getUniprotProteinAPIReportDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );
        Assert.assertEquals( 0, uniprotProteinAPIReportDao.countAll() );

        PersistentUniprotProteinAPIReport report = getMockBuilder().createUniprotProteinAPIReport();
        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();
        report.addUniprotProteinAPICrossReference(uniprotProteinAPIRefs);

        uniprotProteinAPIReportDao.persist( report );
        uniprotProteinAPIReportDao.flush();

        long id = report.getId();

        List<PersistentUniprotProteinAPICrossReferences> uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", id);

        Assert.assertTrue(!uniprotProteinAPIResults.isEmpty());
        Assert.assertNotNull(uniprotProteinAPIResults.get(0).getUniprotProteinAPIReport());
        Assert.assertTrue(uniprotProteinAPIResults.get(0).getUniprotProteinAPIReport().getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ByDatabaseName_and_ActionId_Unsuccessful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        final UniprotProteinAPIReportDao uniprotProteinAPIReportDao = getUpdateDaoFactory().getUniprotProteinAPIReportDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );
        Assert.assertEquals( 0, uniprotProteinAPIReportDao.countAll() );

        PersistentUniprotProteinAPIReport report = getMockBuilder().createUniprotProteinAPIReport();
        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();
        report.addUniprotProteinAPICrossReference(uniprotProteinAPIRefs);

        uniprotProteinAPIReportDao.persist( report );
        uniprotProteinAPIReportDao.flush();

        long id = report.getId();

        List<PersistentUniprotProteinAPICrossReferences> uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getCrossReferencesByDatabaseNameAndActionId("Ensembl", -1);

        Assert.assertTrue(uniprotProteinAPIResults.isEmpty());
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ById_successful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();

        uniprotProteinAPICrossReferenceDoa.persist( uniprotProteinAPIRefs );
        uniprotProteinAPICrossReferenceDoa.flush();

        long id = uniprotProteinAPIRefs.getId();

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getById(id);

        Assert.assertNotNull(uniprotProteinAPIResults);
        Assert.assertTrue(uniprotProteinAPIResults.getId() == id);
    }

    @Test
    @DirtiesContext
    public void search_UniprotProteinAPICrossReferences_ById_Unsuccessful() throws Exception {
        final UniprotProteinAPICrossReferencesDao uniprotProteinAPICrossReferenceDoa = getUpdateDaoFactory().getUniprotProteinAPICrossReferencesDao();
        Assert.assertEquals( 0, uniprotProteinAPICrossReferenceDoa.countAll() );

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIRefs = getMockBuilder().createUniprotProteinAPICrossReferences();

        uniprotProteinAPICrossReferenceDoa.persist( uniprotProteinAPIRefs );
        uniprotProteinAPICrossReferenceDoa.flush();

        PersistentUniprotProteinAPICrossReferences uniprotProteinAPIResults = uniprotProteinAPICrossReferenceDoa.getById(-1);

        Assert.assertNull(uniprotProteinAPIResults);
    }
}
