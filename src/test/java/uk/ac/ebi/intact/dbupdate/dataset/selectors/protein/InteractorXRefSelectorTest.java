package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.dataset.BasicDatasetTest;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvDatabase;

import java.util.Set;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public class InteractorXRefSelectorTest extends BasicDatasetTest {

    private InteractorXRefSelector selector;

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        this.selector = new InteractorXRefSelector();
        this.selector.setFileWriterEnabled(false);
    }

    @Test
    public void test_ValidConfigFile_ndpkTest(){
        try {
            this.selector.readDatasetFromResources("/dataset/ndpk.csv");

            Assert.assertEquals("NDPK - Interactions involving proteins containing InterPro domain IPR001564, Nucleoside diphosphate kinase, core.", selector.getDatasetValueToAdd());
            Assert.assertEquals(0, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(1, selector.getPublicationsIdToExclude().size());
            Assert.assertEquals(1, selector.getListOfXRefs().keySet().size());
            Assert.assertEquals(1, selector.getListOfXRefs().get(selector.getListOfXRefs().keySet().iterator().next()).size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_components(){
        try {
            this.selector.readDatasetFromResources("/dataset/ndpk.csv");

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(1, listOfAc.size());
            Assert.assertEquals(prot1.getAc(), listOfAc.iterator().next());

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

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertTrue(listOfAc.isEmpty());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }
}
