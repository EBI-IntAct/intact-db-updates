package uk.ac.ebi.intact.dbupdate.gene.parser;

import junit.framework.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;
import uk.ac.ebi.intact.dbupdate.gene.utils.UniProtResult;

import java.io.BufferedReader;
import java.io.FileReader;
import java.net.URL;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: noedelta
 * Date: 19/07/2013
 * Time: 10:34
 */
public class UniProtParserTabTest {

	private static UniProtParser uniProtParser = new UniProtParserTab();

	@Test
	public void testParseUniProtQuery() throws Exception {
		List<UniProtResult> list = null;
		FileReader file = null;

		try {
			URL resource = UniProtParserXMLTest.class.getClassLoader().getResource("uniprot-example.tsv");
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
				Assert.assertEquals(5, aux.getAlternativeNames().size());
				Assert.assertEquals("reviewed", aux.getStatus());
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

	@Test
	public void testGenerateSynonyms() throws Exception {

		UniProtResult uniProtResult = new UniProtResult();
		uniProtResult.setGeneNames("AIFM2 AMID PRG3");

		List<String> geneNameList = new ArrayList<String>();
		//The first one is the gene Name
		geneNameList.add("AMID");
		geneNameList.add("PRG3");

		Assert.assertEquals(geneNameList, UniProtParserTab.generateGeneNameSynonyms(uniProtResult));

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

		Assert.assertEquals(proteinNameList, UniProtParserTab.generateAlternativeNames(uniProtResult));

	}

	@Test
	public void testGenerateAlternativeNamesWithOutThem() throws Exception {

		UniProtResult uniProtResult = new UniProtResult();
		uniProtResult.setProteinNames("Apoptosis-inducing factor 2");

		Assert.assertEquals(null, UniProtParserTab.generateAlternativeNames(uniProtResult));

	}

	@Test
	public void testGenerateRecommendedName() throws Exception {

		UniProtResult uniProtResult = new UniProtResult();
		uniProtResult.setProteinNames("Apoptosis-inducing factor 2 (EC 1.-.-.-)" +
				" (Apoptosis-inducing factor homologous mitochondrion-associated inducer of death)" +
				" (Apoptosis-inducing factor-like mitochondrion-associated inducer of death)" +
				" (p53-responsive gene 3 protein)");

		String recommendedName = "Apoptosis-inducing factor 2";
		Assert.assertEquals(recommendedName, UniProtParserTab.generateRecommendedName(uniProtResult));

	}

	@Test
	public void testGenerateRecommendedNameWithoutAlternativeNames() throws Exception {

		UniProtResult uniProtResult = new UniProtResult();
		uniProtResult.setProteinNames("Apoptosis-inducing factor 2");

		Assert.assertEquals("Apoptosis-inducing factor 2", UniProtParserTab.generateRecommendedName(uniProtResult));

	}

}
