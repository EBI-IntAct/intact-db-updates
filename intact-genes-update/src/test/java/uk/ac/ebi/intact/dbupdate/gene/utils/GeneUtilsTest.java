package uk.ac.ebi.intact.dbupdate.gene.utils;

import junit.framework.Assert;
import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.TestPropertySource;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import uk.ac.ebi.intact.model.*;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 17/07/2013
 * Time: 11:16
 * To change this template use File | Settings | File Templates.
 */
@RunWith(SpringJUnit4ClassRunner.class)
@TestPropertySource(locations="classpath:/retry.properties")
@ContextConfiguration(locations = {
        "classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml"
})
public class GeneUtilsTest {

    @Test
    public void testGetInstitution() throws Exception {
        Institution institution = GeneUtils.getInstitution();
        Assert.assertEquals(institution.getShortLabel(), "unknown");
    }

    @Test
    public void testGetEnsemblDatabase() throws Exception {
        CvDatabase cv = GeneUtils.getEnsemblDatabase();
        Assert.assertEquals(cv.getShortLabel(), CvDatabase.ENSEMBL);
    }

    @Test
    public void testGetGeneType() throws Exception {
        CvInteractorType cv = GeneUtils.getGeneType();
        Assert.assertEquals(cv.getShortLabel(), CvInteractorType.GENE);
    }

    @Test
    public void testGetPrimaryIDQualifier() throws Exception {
        CvXrefQualifier cv = GeneUtils.getPrimaryIDQualifier();
        Assert.assertEquals(cv.getShortLabel(), CvXrefQualifier.IDENTITY);
    }

    @Test
    public void testGetSynonymAliasType() throws Exception {
        CvAliasType cv = GeneUtils.getSynonymAliasType();
        Assert.assertEquals(cv.getShortLabel(), CvAliasType.SYNONYM);
    }

    @Test
    public void testGenerateShortLabel() throws Exception {
        Assert.assertEquals("entry_name_gene", GeneUtils.generateShortLabel("ENTRY_NAME"));
    }
}
