package uk.ac.ebi.intact.dbupdate.bioactiveentity.utils;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.model.*;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 24/05/2013
 * Time: 11:18
 */

public class BioActiveEntityUtilsTest {
    @Test
    public void testGetInstitution() throws Exception {
        Institution institution = BioActiveEntityUtils.getInstitution();
        Assert.assertEquals(institution.getShortLabel(), "unknown");
    }

    @Test
    public void testGetChEBIDatabase() throws Exception {
        CvDatabase cv = BioActiveEntityUtils.getChEBIDatabase();
        Assert.assertEquals(cv.getShortLabel(), CvDatabase.CHEBI);

    }

    @Test
    public void testGetSmallMoleculeType() throws Exception {
        CvInteractorType cv = BioActiveEntityUtils.getSmallMoleculeType();
        Assert.assertEquals(cv.getShortLabel(), CvInteractorType.SMALL_MOLECULE);
    }

    @Test
    public void testGetPolysaccharidesType() throws Exception {
        CvInteractorType cv = BioActiveEntityUtils.getPolysaccharidesType();
        Assert.assertEquals(cv.getShortLabel(), CvInteractorType.POLYSACCHARIDE);
    }

    @Test
    public void testGetPrimaryIDQualifier() throws Exception {
        CvXrefQualifier  cv = BioActiveEntityUtils.getPrimaryIDQualifier();
        Assert.assertEquals(cv.getShortLabel(), CvXrefQualifier.IDENTITY);
    }

    @Test
    public void testGetSecondaryIDQualifier() throws Exception {
        CvXrefQualifier  cv = BioActiveEntityUtils.getSecondaryIDQualifier();
        Assert.assertEquals(cv.getShortLabel(), CvXrefQualifier.SECONDARY_AC);
    }

    @Test
    public void testGetSynonymAliasType() throws Exception {
        CvAliasType cv = BioActiveEntityUtils.getSynonymAliasType();
        Assert.assertEquals(cv.getShortLabel(), CvAliasType.SYNONYM);
    }

    @Test
    public void testGetIupacAliasType() throws Exception {
        CvAliasType cv = BioActiveEntityUtils.getIupacAliasType();
        Assert.assertEquals(cv.getShortLabel(), CvAliasType.IUPAC_NAME);
    }

    @Test
    public void testGetInchiType() throws Exception {
        CvTopic cv = BioActiveEntityUtils.getInchiType();
        Assert.assertEquals(cv.getShortLabel(), CvTopic.INCHI_ID);
    }

    @Test
    public void testGetInchiKeyType() throws Exception {
        CvTopic cv = BioActiveEntityUtils.getInchiKeyType();
        Assert.assertEquals(cv.getShortLabel(), CvTopic.INCHI_KEY);
    }

    @Test
    public void testGetSmilesType() throws Exception {
        CvTopic cv = BioActiveEntityUtils.getSmilesType();
        Assert.assertEquals(cv.getShortLabel(), CvTopic.SMILES_STRING);
    }
}
