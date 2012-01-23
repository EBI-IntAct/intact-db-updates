package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.dataset.BasicDatasetTest;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;

import java.util.Set;

/**
 * Unit tester of uniprot keyword selector
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>23/01/12</pre>
 */

public class UniprotKeywordSelectorTest extends BasicDatasetTest{

    private UniprotKeywordSelector selector;

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        this.selector = new UniprotKeywordSelector();
        this.selector.setFileWriterEnabled(false);
    }

    @Test
    public void test_ValidConfigFile_apoptosisTest(){
        try {
            this.selector.readDatasetFromResources("/dataset/apoptosis.csv");

            Assert.assertEquals("Apoptosis - Interactions involving proteins with a function related to apoptosis", selector.getDatasetValueToAdd());
            Assert.assertEquals(0, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(0, selector.getPublicationsIdToExclude().size());
            Assert.assertNotNull(selector.getKeyword());
            Assert.assertEquals("Apoptosis", selector.getKeyword());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins_apoptosis(){
        try {
            this.selector.readDatasetFromResources("/dataset/apoptosis.csv");

            Set<String> listOfAc = this.selector.getSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(1, listOfAc.size());
            Assert.assertEquals(prot7.getAc(), listOfAc.iterator().next());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }
}
