package uk.ac.ebi.intact.dbupdate.gene.utils;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;
import uk.ac.ebi.intact.dbupdate.gene.parser.UniProtParser;
import uk.ac.ebi.intact.dbupdate.gene.parser.UniProtParserXML;

import java.io.UnsupportedEncodingException;
import java.util.Arrays;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 17/07/2013
 * Time: 11:30
 */
public class UniProtRestQueryTest {

    private UniProtParser parser = new UniProtParserXML();
    private UniProtRestQuery uniProtRestQuery = new UniProtRestQuery(parser);

    @Test
    public void testQueryUniProt() throws Exception {

        ParameterNameValue[] parameters = null;

        try {

            parameters = new ParameterNameValue[]
                    {
                            //By default only the swissprot entries (curators decision)
                            new ParameterNameValue("query", "ENSG00000126001 + reviewed:yes"),
                            new ParameterNameValue("columns",
                                    "entry name,genes,organism,organism-id,id,reviewed,protein names"),
                            new ParameterNameValue("format", "xml")
                    };

            List<UniProtResult> list = uniProtRestQuery.queryUniProt("uniprot", parameters);
            Assert.assertEquals(1, list.size());
            UniProtResult aux = list.get(0);
            Assert.assertEquals("CP250_HUMAN", aux.getEntryName());
            Assert.assertEquals("Centrosome-associated protein CEP250", aux.getRecommendedName());
            Assert.assertEquals("CEP250", aux.getGeneName());
            Assert.assertEquals("Human", aux.getOrganism());
            Assert.assertEquals("9606", aux.getOrganismId());
            Assert.assertEquals(2, aux.getGeneNameSynonyms().size());
            Assert.assertEquals(3, aux.getAlternativeNames().size());
            Assert.assertEquals("reviewed", aux.getStatus());

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
                        new ParameterNameValue("format", "xml")
                };

        Assert.assertEquals(
                "http://www.uniprot.org/uniprot/?query=ENSMUSG00000034391&columns=entry+name%2Cgenes%2Corganism%2Corganism-id%2Cid%2Creviewed%2Cprotein+names&reviewed=yes&format=xml",
                UniProtRestQuery.queryURLGenerator("uniprot", parameters)
        );

    }

}
