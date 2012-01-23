package uk.ac.ebi.intact.dbupdate.dataset.selectors.protein;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.dataset.BasicDatasetTest;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.BioSource;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.Protein;

import java.util.Set;

/**
 * InteractorAliasSelector tester
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Jun-2010</pre>
 */
public class InteractorAliasSelectorTest extends BasicDatasetTest {

    private InteractorAliasSelector selector;

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        this.selector = new InteractorAliasSelector();
        this.selector.setFileWriterEnabled(false);
    }

    @Test
    public void test_ValidConfigFile_synapseTest(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Assert.assertEquals("Synapse - Interactions of proteins with an established role in the presynapse.", selector.getDatasetValueToAdd());
            Assert.assertEquals(3, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(4, selector.getPublicationsIdToExclude().size());
            Assert.assertEquals(1, selector.getListOfProteins().keySet().size());
            Assert.assertEquals(3, ((Set) selector.getListOfProteins().get(selector.getListOfProteins().keySet().iterator().next())).size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_ValidConfigFile_synapseTest_OneOrganism(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest_oneOrganism.csv");

            Assert.assertEquals("Interactions of proteins with an established role in the presynapse.", selector.getDatasetValueToAdd());
            Assert.assertEquals(1, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(1, selector.getListOfProteins().keySet().size());
            Assert.assertEquals(3, ((Set) selector.getListOfProteins().get(selector.getListOfProteins().keySet().iterator().next())).size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_ValidConfigFile_synapseTest_NoOrganism(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest_noOrganism.csv");

            Assert.assertEquals("Interactions of proteins with an established role in the presynapse.", selector.getDatasetValueToAdd());
            Assert.assertEquals(0, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(1, selector.getListOfProteins().keySet().size());
            Assert.assertEquals(3, ((Set) selector.getListOfProteins().get(selector.getListOfProteins().keySet().iterator().next())).size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_ValidConfigFile_synapseTest_MissingMiNumbers(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest_MiMissing.csv");

            Assert.assertEquals("Interactions of proteins with an established role in the presynapse.", selector.getDatasetValueToAdd());
            Assert.assertEquals(3, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(1, selector.getListOfProteins().keySet().size());
            Assert.assertEquals(3, ((Set) selector.getListOfProteins().get(selector.getListOfProteins().keySet().iterator().next())).size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(6, listOfAc.size());

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins_excluded_organism(){
        try {
            BioSource yeast = getMockBuilder().createBioSource(4932, "yeast");
            intactContext.getCorePersister().saveOrUpdate(yeast);

            Protein prot = getMockBuilder().createProtein("P01244", "amph_yeast", yeast);
            prot.getAliases().iterator().next().setName("AMPH");
            intactContext.getCorePersister().saveOrUpdate(prot);
            String yeastProt = prot.getAc();

            Assert.assertNotNull(yeastProt);

            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(6, listOfAc.size());
            Assert.assertFalse(listOfAc.contains(yeastProt));

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins_excluded_geneName(){
        try {
            BioSource human = getMockBuilder().createBioSource(9606, "human");

            Protein prot = getMockBuilder().createProtein("P01246", "amph_human", human);
            prot.getAliases().iterator().next().setName("AMPH2");
            intactContext.getCorePersister().saveOrUpdate(prot);
            String humanProt = prot.getAc();

            Assert.assertNotNull(humanProt);

            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(6, listOfAc.size());
            Assert.assertFalse(listOfAc.contains(humanProt));

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins_excluded_aliasType(){
        try {
            BioSource human = getMockBuilder().createBioSource(9606, "human");

            Protein prot = getMockBuilder().createProtein("P01250", "amph_human", human);
            InteractorAlias alias = prot.getAliases().iterator().next();
            alias.setName("AMPH");
            alias.setCvAliasType(getMockBuilder().createCvObject(CvAliasType.class, "MI:xxxx", "orf"));
            intactContext.getCorePersister().saveOrUpdate(prot);
            String humanProt = prot.getAc();

            Assert.assertNotNull(humanProt);

            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Set<String> listOfAc = this.selector.collectSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(6, listOfAc.size());
            Assert.assertFalse(listOfAc.contains(humanProt));

        } catch (DatasetException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

}
