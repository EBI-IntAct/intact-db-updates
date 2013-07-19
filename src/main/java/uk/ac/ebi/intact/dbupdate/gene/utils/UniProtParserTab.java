package uk.ac.ebi.intact.dbupdate.gene.utils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.Reader;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: noedelta
 * Date: 19/07/2013
 * Time: 08:50
 */
public class UniProtParserTab implements UniProtParser {

	public static final Log log = LogFactory.getLog(UniProtParserTab.class);


	@Override
	public List<UniProtResult> parseUniProtQuery(Reader reader) {

		//We skip the header before start to read
		CSVReader csvReader = new CSVReader(reader, '\t', '\"', 1);

		ColumnPositionMappingStrategy strat = new ColumnPositionMappingStrategy();

		String[] columns = new String[]{
				"entryName",
				"geneNames",
				"organism",
				"organismId",
				"entry",
				"status",
				"proteinNames"
		}; // the fields to bind do in your JavaBean

		strat.setColumnMapping(columns);
		strat.setType(UniProtResult.class);

		CsvToBean csv = new CsvToBean();
		List<UniProtResult> list = csv.parse(strat, csvReader);

		//We complement the result

		for (UniProtResult element : list) {

			element.setGeneName(generateGeneName(element));
			element.setGeneNameSynonyms(generateGeneNameSynonyms(element));
			element.setRecommendedName(generateRecommendedName(element));
			element.setAlternativeNames(generateAlternativeNames(element));

			String commonName = element.getOrganism();

			if (commonName.indexOf("(") > 0) {
				commonName = commonName.substring(commonName.indexOf("(") + 1, commonName.indexOf(")"));
			}
			element.setOrganism(commonName);
		}

		return list;
	}

	protected static String generateGeneName(UniProtResult element) {

		String aux = element.getGeneNames();
		String geneName = null;

		if (aux != null && !aux.isEmpty()) {
			int index = aux.indexOf(" ");
			if (index > 0) {
				geneName = aux.substring(0, index).trim();
			} else { //If we do not have any whitespace we only have one name or any
				geneName = aux;
			}
		}

		log.debug("Gene Name: " + geneName);
		return geneName;
	}

	protected static List<String> generateGeneNameSynonyms(UniProtResult element) {

		List<String> synonyms = null;
		String aux = element.getGeneNames();

		if (aux != null && !aux.isEmpty()) {
			int index = aux.indexOf(" ");
			if (index > 0) {
				String geneNames = aux.substring(index + 1, aux.length());

				String[] geneNamesArray = geneNames.split("\\s");
				synonyms = Arrays.asList(geneNamesArray);

			}
		}

		log.debug("Synonyms: " + (synonyms != null ? synonyms.toString() : null));
		return synonyms;
	}

	protected static String generateRecommendedName(UniProtResult element) {

		String aux = element.getProteinNames();
		String recommendedName = null;

		if (aux != null && !aux.isEmpty()) {
			int index = aux.indexOf("(");
			if (index > 0) {
				recommendedName = aux.substring(0, index - 1).trim();
			} else { //If we do not have any whitespace we only have one name or any
				recommendedName = aux;
			}
		}

		log.debug("Recommended Name: " + recommendedName);
		return recommendedName;
	}

	protected static List<String> generateAlternativeNames(UniProtResult element) {

		String aux = element.getProteinNames();
		List<String> alternativeNamesList = null;

		if (aux != null && !aux.isEmpty()) {
			int index = aux.indexOf("(");
			if (index > 0) {
				String alternativeNames = aux.substring(index, aux.length());

				String[] alternativeNamesArray = alternativeNames.split("\\)");
				if (alternativeNamesArray != null && alternativeNamesArray.length > 0) {
					alternativeNamesList = new ArrayList<String>(alternativeNamesArray.length);
					for (String name : alternativeNamesArray) {
						if (!name.startsWith("[")) {
							//We need to remove the parenthesis
							alternativeNamesList.add(name.substring(name.indexOf("(") + 1, name.length()));
						}
					}
				}
			}
		}

		log.debug("Alternative Names: " + (alternativeNamesList != null ? alternativeNamesList.toString() : null));
		return alternativeNamesList;  //To change body of created methods use File | Settings | File Templates.
	}

}
