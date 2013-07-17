package uk.ac.ebi.intact.dbupdate.gene.utils;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;

import java.io.UnsupportedEncodingException;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 17/07/2013
 * Time: 11:30
 */
public class UniProtRestQueryTest {
    @Test
    public void testQueryUniProt() throws Exception {

        ParameterNameValue[] parameters = null;

        try {

            parameters = new ParameterNameValue[]
                    {
                            new ParameterNameValue("query", "ENSMUSG00000034391"),
                            new ParameterNameValue("columns",
                                    "entry name,genes,organism,organism-id,id,reviewed,protein names"),
                            new ParameterNameValue("reviewed", "yes"), //By default only the swissprot entries (curators decision)
                            new ParameterNameValue("format", "tab")
                    };

            List<UniProtResult> list = UniProtRestQuery.queryUniProt("uniprot", parameters);

        } catch (UnsupportedEncodingException e) {
            throw new GeneServiceException("The parameters " + Arrays.toString(parameters) + " for the uniprot query can not be enconded: ", e);
        }


    }

    @Test
    public void testQueryURLGenerator() throws Exception {

        ParameterNameValue[] parameters = new ParameterNameValue[]
                {
                        new ParameterNameValue("query", "ENSMUSG00000034391"),
                        new ParameterNameValue("columns",
                                "entry name,genes,organism,organism-id,id,reviewed,protein names"),
                        new ParameterNameValue("reviewed", "yes"), //By default only the swissprot entries (curators decision)
                        new ParameterNameValue("format", "tab")
                };

        Assert.assertEquals(
                "http://www.uniprot.org/uniprot/?query=ENSMUSG00000034391&columns=entry+name%2Cgenes%2Corganism%2Corganism-id%2Cid%2Creviewed%2Cprotein+names&reviewed=yes&format=tab",
                UniProtRestQuery.queryURLGenerator("uniprot", parameters)
        );

    }

    @Test
    public void testParseUniProtQuery() throws Exception {

    }

    @Test
    public void testGenerateSynonyms() throws Exception {

        UniProtResult uniProtResult = new UniProtResult();
        uniProtResult.setGeneNames("AIFM2 AMID PRG3");

        List<String> geneNameList = new ArrayList<String>();
        geneNameList.add("AIFM2");
        geneNameList.add("AMID");
        geneNameList.add("PRG3");

        Assert.assertEquals(geneNameList, UniProtRestQuery.generateSynonyms(uniProtResult));

    }

    @Test
    public void testGenerateAlternativeNames() throws Exception {

        UniProtResult uniProtResult = new UniProtResult();
        uniProtResult.setProteinNames("Apoptosis-inducing factor 2 (EC 1.-.-.-)" +
                " (Apoptosis-inducing factor homologous mitochondrion-associated inducer of death)" +
                " (Apoptosis-inducing factor-like mitochondrion-associated inducer of death)" +
                " (p53-responsive gene 3 protein)");

        List<String> proteinNameList = new ArrayList<String>();
        proteinNameList.add("EC 1.-.-.-");
        proteinNameList.add("Apoptosis-inducing factor homologous mitochondrion-associated inducer of death");
        proteinNameList.add("Apoptosis-inducing factor-like mitochondrion-associated inducer of death");
        proteinNameList.add("p53-responsive gene 3 protein");

        Assert.assertEquals(proteinNameList, UniProtRestQuery.generateAlternativeNames(uniProtResult));

    }

    @Test
    public void testGenerateAlternativeNamesWithOutThem() throws Exception {

        UniProtResult uniProtResult = new UniProtResult();
        uniProtResult.setProteinNames("Apoptosis-inducing factor 2");

        Assert.assertEquals(null, UniProtRestQuery.generateAlternativeNames(uniProtResult));

    }

    @Test
    public void testGenerateRecommendedName() throws Exception {

        UniProtResult uniProtResult = new UniProtResult();
        uniProtResult.setProteinNames("Apoptosis-inducing factor 2 (EC 1.-.-.-)" +
                " (Apoptosis-inducing factor homologous mitochondrion-associated inducer of death)" +
                " (Apoptosis-inducing factor-like mitochondrion-associated inducer of death)" +
                " (p53-responsive gene 3 protein)");

        String recommendedName = "Apoptosis-inducing factor 2";
        Assert.assertEquals(recommendedName, UniProtRestQuery.generateRecommendedName(uniProtResult));

    }

    @Test
    public void testGenerateRecommendedNameWithoutAlternativeNames() throws Exception {

        UniProtResult uniProtResult = new UniProtResult();
        uniProtResult.setProteinNames("Apoptosis-inducing factor 2");

        Assert.assertEquals("Apoptosis-inducing factor 2", UniProtRestQuery.generateRecommendedName(uniProtResult));

    }

}
