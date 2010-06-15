package uk.ac.ebi.intact.dbupdate.dataset.selectors.component;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.dataset.BasicDatasetTest;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.*;

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

    private void createInterproXRefs(){
        Interaction i = getMockBuilder().createInteraction(this.prot1, this.prot2);
        i.getExperiments().clear();
        Component c = i.getComponents().iterator().next();
        c.setInteractor(prot1);
        Feature f = getMockBuilder().createFeatureRandom();
        f.getXrefs().clear();
        FeatureXref x = getMockBuilder().createXref(f, "IPR001564", this.intactContext.getDataContext().getDaoFactory().getCvObjectDao( CvXrefQualifier.class ).getByPsiMiRef( CvXrefQualifier.IDENTITY_MI_REF ), this.intactContext.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTERPRO_MI_REF ));
        f.addXref(x);
        c.addBindingDomain(f);

        this.intactContext.getCorePersister().saveOrUpdate(i);
        this.intactContext.getCorePersister().saveOrUpdate(c);
        this.intactContext.getCorePersister().saveOrUpdate(f);

        Assert.assertFalse(this.intactContext.getDataContext().getDaoFactory().getFeatureDao().getByXrefLike("IPR001564").isEmpty());
    }

    @Before
    public void setUpDatabase(){
        super.setUpDatabase();
        this.selector = new FeatureXRefSelector();
        this.selector.setIntactContext(intactContext);
        this.selector.setFileWriterEnabled(false);
        createInterproXRefs();
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
}
