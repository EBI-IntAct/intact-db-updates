package uk.ac.ebi.intact.dbupdate.gene.parser;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;
import uk.ac.ebi.intact.dbupdate.gene.utils.UniProtResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: noedelta
 * Date: 19/07/2013
 * Time: 10:37
 */
public class UniProtParserXMLTest {

	private static UniProtParser uniProtParser = new UniProtParserXML();

	@Test
	public void testParseUniProtQuery() throws Exception {
		List<UniProtResult> list = null;
		FileReader file = null;

		try {
			URL resource = UniProtParserXMLTest.class.getClassLoader().getResource("uniprot-example.xml");
			if (resource != null) {
				file = new FileReader(resource.getFile());

				BufferedReader reader = new BufferedReader(file);
				list = uniProtParser.parseUniProtQuery(reader);

				Assert.assertEquals(1, list.size());
				UniProtResult aux = list.get(0);
				Assert.assertEquals("CP250_HUMAN", aux.getEntryName());
				Assert.assertEquals("Centrosome-associated protein CEP250", aux.getRecommendedName());
				Assert.assertEquals("CEP250", aux.getGeneName());
				Assert.assertEquals("Human", aux.getOrganism());
				Assert.assertEquals("9606", aux.getOrganismId());
				Assert.assertEquals(2, aux.getGeneNameSynonyms().size());
				Assert.assertEquals(3, aux.getAlternativeNames().size());
				Assert.assertEquals("reviewed",aux.getStatus());
			}
		} catch (Exception e) {
			throw new GeneServiceException("Error retrieving information from UniProt. The information can not be parse. "
					+ "Error message: " + e.getMessage(), e);
		} finally {
			if (file != null) {
				file.close();
			}
		}
	}
}
