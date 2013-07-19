package uk.ac.ebi.intact.dbupdate.gene.utils;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;

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

}
