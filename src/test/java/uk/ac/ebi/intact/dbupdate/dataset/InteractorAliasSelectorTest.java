package uk.ac.ebi.intact.dbupdate.dataset;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.BioSource;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.Protein;

import java.util.Set;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Jun-2010</pre>
 */
public class InteractorAliasSelectorTest extends IntactBasicTestCase{

    private InteractorAliasSelector selector;
    private IntactContext intactContext;

    public void createProteinsHumanMouseAndRat(){
        BioSource human = getMockBuilder().createBioSource(9606, "human");
        BioSource mouse = getMockBuilder().createBioSource(10090, "mouse");
        BioSource rat = getMockBuilder().createBioSource(10116, "rat");

        intactContext.getCorePersister().saveOrUpdate(human);
        intactContext.getCorePersister().saveOrUpdate(mouse);
        intactContext.getCorePersister().saveOrUpdate(rat);

        Protein prot1 = getMockBuilder().createProtein("P01234", "amph_human", human);
        Protein prot2 = getMockBuilder().createProtein("P01235", "amph_mouse", mouse);
        Protein prot3 = getMockBuilder().createProtein("P01236", "amph_rat", rat);
        Protein prot4 = getMockBuilder().createProtein("P01237", "apba1_human", human);
        Protein prot5 = getMockBuilder().createProtein("P01238", "apba1_mouse", mouse);
        Protein prot6 = getMockBuilder().createProtein("P01239", "apba2_human", human);

        prot1.getAliases().iterator().next().setName("AMPH");
        prot2.getAliases().iterator().next().setName("AMPH");
        prot3.getAliases().iterator().next().setName("AMPH");
        prot4.getAliases().iterator().next().setName("APBA1");
        prot5.getAliases().iterator().next().setName("APBA1");
        prot6.getAliases().iterator().next().setName("APBA2");

        intactContext.getCorePersister().saveOrUpdate(prot1);
        intactContext.getCorePersister().saveOrUpdate(prot2);
        intactContext.getCorePersister().saveOrUpdate(prot3);
        intactContext.getCorePersister().saveOrUpdate(prot4);
        intactContext.getCorePersister().saveOrUpdate(prot5);
        intactContext.getCorePersister().saveOrUpdate(prot6);

        Assert.assertEquals(intactContext.getDaoFactory().getAliasDao(InteractorAlias.class).countAll(), 6);
    }

    @Before
    public void createBlastProcess(){
        this.intactContext = IntactContext.getCurrentInstance();
        this.selector = new InteractorAliasSelector();
        this.selector.setIntactContext(intactContext);

        createProteinsHumanMouseAndRat();
    }

    @Test
    public void test_ValidConfigFile_synapseTest(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Assert.assertEquals("Interactions of proteins with an established role in the presynapse.", selector.getDatasetValueToAdd());
            Assert.assertEquals(3, selector.getListOfPossibleTaxId().size());
            Assert.assertEquals(2, selector.getPublicationsIdToExclude().size());
            Assert.assertEquals(1, selector.getListOfProteins().keySet().size());
            Assert.assertEquals(3, ((Set) selector.getListOfProteins().get(selector.getListOfProteins().keySet().iterator().next())).size());

        } catch (ProteinSelectorException e) {
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

        } catch (ProteinSelectorException e) {
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

        } catch (ProteinSelectorException e) {
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

        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }

    @Test
    public void test_select_list_proteins(){
        try {
            this.selector.readDatasetFromResources("/dataset/synapseTest.csv");

            Set<String> listOfAc = this.selector.getSelectionOfProteinAccessionsInIntact();

            Assert.assertFalse(listOfAc.isEmpty());
            Assert.assertEquals(6, listOfAc.size());

        } catch (ProteinSelectorException e) {
            e.printStackTrace();
            Assert.assertFalse(true);
        }
    }
}
