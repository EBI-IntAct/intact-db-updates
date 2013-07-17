package uk.ac.ebi.intact.dbupdate.gene.importer;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.model.Interactor;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 17:15
 * To change this template use File | Settings | File Templates.
 */
public class GeneServiceImplTest {

    private GeneServiceImpl geneService;

    @Before
    public void setUp() throws Exception {
        geneService = new GeneServiceImpl();
    }


    @After
    public void tearDown() {
        geneService = null;
    }

    @Test
    public void testGetGeneByEnsemblIdInSwissprot() throws Exception {
        //We expect only one entrance
        List<Interactor> candidatesList = geneService.getGeneByEnsemblIdInSwissprot("ENSG00000126001");
        Assert.assertEquals(1, candidatesList.size());
        Assert.assertEquals(7, candidatesList.get(0).getAliases().size());
        Assert.assertEquals(1, candidatesList.get(0).getXrefs().size());


    }

    @Test
    public void testGetGeneByEnsemblIdInUniProt() throws Exception {
        List<Interactor> candidatesList = geneService.getGeneByEnsemblIdInUniprot("ENSMUSG00000034391");
        Assert.assertEquals(2, candidatesList.size());
    }
}
