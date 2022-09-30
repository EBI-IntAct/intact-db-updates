package uk.ac.ebi.intact.dbupdate.gene.utils;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;
import uk.ac.ebi.intact.dbupdate.gene.parser.UniProtParser;
import uk.ac.ebi.intact.dbupdate.gene.parser.UniProtParserTab;
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

    private UniProtParser parser = new UniProtParserTab();
    private UniProtRestQuery uniProtRestQuery = new UniProtRestQuery(parser);

    @Test
    public void testQueryUniProt() throws Exception {

        ParameterNameValue[] parameters = null;

        try {

            parameters = new ParameterNameValue[]
                    {
                            //By default only the swissprot entries (curators decision)
                            new ParameterNameValue("query", "ENSG00000126001 AND (reviewed:true)"),
                            new ParameterNameValue("fields",
                                    "accession,id,gene_names,organism_name,organism_id,reviewed,protein_name"),
                            new ParameterNameValue("format", "tsv")
                    };

            List<UniProtResult> list = uniProtRestQuery.queryUniProt("uniprotkb", parameters);
            Assert.assertEquals(1, list.size());
            UniProtResult aux = list.get(0);
            Assert.assertEquals("CP250_HUMAN", aux.getEntryName());
            Assert.assertEquals("Centrosome-associated protein CEP250", aux.getRecommendedName());
            Assert.assertEquals("CEP250", aux.getGeneName());
            Assert.assertEquals("Homo sapiens", aux.getScientificOrganismName());
            Assert.assertEquals("9606", aux.getOrganismId());
            Assert.assertEquals(2, aux.getGeneNameSynonyms().size());
            Assert.assertEquals(5, aux.getAlternativeNames().size());
            Assert.assertEquals("reviewed", aux.getStatus());

        } catch (UnsupportedEncodingException e) {
            throw new GeneServiceException("The parameters " + Arrays.toString(parameters) + " for the uniprot query can not be enconded: ", e);
        }


    }

    @Test
    public void testQueryURLGenerator() throws Exception {

        ParameterNameValue[] parameters = new ParameterNameValue[]
                {
                        new ParameterNameValue("query", "ENSMUSG00000034391 AND (reviewed:true)"),
                        new ParameterNameValue("fields",
                        "accession,id,gene_names,organism_name,organism_id,reviewed,protein_name"),
                        new ParameterNameValue("format", "tsv")
                };

        Assert.assertEquals(
                "https://rest.uniprot.org/uniprotkb/stream?query=ENSMUSG00000034391+AND+%28reviewed%3Atrue%29&fields=accession%2Cid%2Cgene_names%2Corganism_name%2Corganism_id%2Creviewed%2Cprotein_name&format=tsv",
                UniProtRestQuery.queryURLGenerator("uniprotkb", parameters)
        );

    }

}
