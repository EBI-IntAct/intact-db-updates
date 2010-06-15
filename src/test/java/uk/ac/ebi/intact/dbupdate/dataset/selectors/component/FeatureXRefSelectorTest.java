package uk.ac.ebi.intact.dbupdate.dataset.selectors.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.dataset.BasicDatasetTest;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;

import java.util.Set;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public class FeatureXRefSelectorTest extends BasicDatasetTest {

    private FeatureXRefSelector selector;

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        this.selector = new FeatureXRefSelector();
        this.selector.setIntactContext(intactContext);
        this.selector.setFileWriterEnabled(false);
    }

    @Test
    public void test_ValidConfigFile_ndpkTest(){
        try {
            this.selector.readDatasetFromResources("/dataset/ndpk.csv");

            Assert.assertEquals("NDPK - Interactions involving proteins containing InterPro domain IPR001564, Nucleoside diphosphate kinase, core.", selector.getDatasetValueToAdd());
            Assert.assertEquals(0, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(0, selector.getPublicationsIdToExclude().size());
            Assert.assertEquals(CvDatabase.INTERPRO_MI_REF, selector.getDatabase().getIdentifier());
            Assert.assertEquals("IPR001564", selector.getDatabaseId());
            Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, selector.getQualifier().getIdentifier());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_components(){
        try {
            this.selector.readDatasetFromResources("/dataset/ndpk.csv");

            Set<String> listOfAc = this.selector.getSelectionOfComponentAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(1, listOfAc.size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_components_resid_Database(){
        try {
            CvDatabase resid = getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.RESID_MI_REF, CvDatabase.RESID);

            this.intactContext.getCorePersister().saveOrUpdate(resid);

            this.selector.readDatasetFromResources("/dataset/ndpk_otherDatabase.csv");

            Set<String> listOfAc = this.selector.getSelectionOfComponentAccessionsInIntact();

            Assert.assertTrue(listOfAc.isEmpty());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_components_see_also_qualifier(){
        try {
            CvXrefQualifier seeAlso = getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.SEE_ALSO_MI_REF, CvXrefQualifier.SEE_ALSO);

            this.intactContext.getCorePersister().saveOrUpdate(seeAlso);

            this.selector.readDatasetFromResources("/dataset/ndpk_otherQualifier.csv");

            Set<String> listOfAc = this.selector.getSelectionOfComponentAccessionsInIntact();

            Assert.assertTrue(listOfAc.isEmpty());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }
}
