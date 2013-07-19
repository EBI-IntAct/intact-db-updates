package uk.ac.ebi.intact.dbupdate.gene.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.gene.parser.*;

import javax.xml.bind.JAXBContext;
import javax.xml.bind.JAXBException;
import javax.xml.bind.Unmarshaller;
import java.io.Reader;
import java.util.ArrayList;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: noedelta
 * Date: 19/07/2013
 * Time: 08:49
 */
public class UniProtParserXML implements UniProtParser {

	public static final Log log = LogFactory.getLog(UniProtParserXML.class);


	@Override
	public List<UniProtResult> parseUniProtQuery(Reader result) {

		List<UniProtResult> list = new ArrayList<UniProtResult>();

		try {

			JAXBContext jaxbContext = JAXBContext.newInstance(Uniprot.class);

			Unmarshaller unmarshaller = jaxbContext.createUnmarshaller();
			Uniprot doc = (Uniprot) unmarshaller.unmarshal(result);

			for (Entry entry : doc.getEntry()) {
				UniProtResult uniGene = new UniProtResult();

				uniGene.setEntryName(generateEntryName(entry.getName()));

				//Exist, so we fill the rest of the info
				if (uniGene.getEntryName() != null) {

					//Reviewed or not
					if(entry.getDataset().equals("Swiss-Prot")){
						uniGene.setStatus("reviewed");
					} else if(entry.getDataset().equals("TrEMBL")){
						uniGene.setStatus("unreviewed");
					}

					//Full Name
					uniGene.setRecommendedName(entry.getProtein().getRecommendedName().getFullName().getValue());

					//AlternativeNames
					List<String> synonyms = new ArrayList<String>();
					for (ProteinType.AlternativeName name : entry.getProtein().getAlternativeName()) {
						synonyms.add(name.getFullName().getValue());
					}

					if (!synonyms.isEmpty()) {
						uniGene.setAlternativeNames(synonyms);
					}

					//Gene name
					List<String> geneNames = new ArrayList<String>();

					//Gene synonyms
					List<String> geneSynonyms = new ArrayList<String>();

					for (GeneType geneType : entry.getGene()) {
						for (GeneNameType geneNameType : geneType.getName()) {
							if (geneNameType.getType().equals("primary")) {
								geneNames.add(geneNameType.getValue());
							} else if (geneNameType.getType().equals("synonym")) {
								geneSynonyms.add(geneNameType.getValue());
							}
						}
					}
					if (!geneNames.isEmpty()) {
						uniGene.setGeneName(geneNames.get(0));

						if (geneNames.size() > 1) {
							//More that one
							geneNames.remove(0);
							geneSynonyms.addAll(geneNames);
						}
					}

					if (!geneSynonyms.isEmpty()) {
						uniGene.setGeneNameSynonyms(geneSynonyms);
					}

					// Organism
					String scientificName = null;
					String fullName = null;

					for (OrganismNameType organismNameType : entry.getOrganism().getName()) {
						if (organismNameType.getType().equals("common")) {
							uniGene.setOrganism(organismNameType.getValue());
						}
						if (organismNameType.getType().equals("scientific")) {
							scientificName = organismNameType.getValue();
						}
						if (organismNameType.getType().equals("full")) {
							fullName = organismNameType.getValue();
						}
					}

					if (uniGene.getOrganism() == null)
						if (scientificName != null) {
							uniGene.setOrganism(scientificName);
						} else if (fullName != null) {
							uniGene.setOrganism(fullName);
						}

					//We assume only one organism of NCBI Taxonomy type
					for (DbReferenceType dbReferenceType : entry.getOrganism().getDbReference()) {
						if (dbReferenceType.getType().equals("NCBI Taxonomy")) {
							uniGene.setOrganismId(dbReferenceType.getId());
						}
					}


					list.add(uniGene);

				}
			}
		} catch (JAXBException e) {
			e.printStackTrace();
		}

		return list;
	}

	private static String generateEntryName(List<String> name) {

		if (name != null && !name.isEmpty()) {
			return name.get(0); //If we have more that one name, we use only the firs one
		}
		return null;  //To change body of created methods use File | Settings | File Templates.
	}
}
