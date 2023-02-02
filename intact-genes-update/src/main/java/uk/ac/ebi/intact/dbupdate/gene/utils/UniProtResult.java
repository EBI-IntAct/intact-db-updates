package uk.ac.ebi.intact.dbupdate.gene.utils;

import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 12/07/2013
 * Time: 16:35
 * To change this template use File | Settings | File Templates.
 */
public class UniProtResult {

	//Direct parsing
    private String entry;
    private String entryName;
    private String geneNames;
    private String organism;
    private String commonOrganismName;
    private String scientificOrganismName;
    private String organismId;
    private String status;
    private String proteinNames;

	//Extra fields for helping
	private String geneName;
	private List<String> geneNameSynonyms;
    private String recommendedName;
    private List<String> alternativeNames;

	public UniProtResult() {
    }

    public String getEntryName() {
        return entryName;
    }

    public void setEntryName(String entryName) {
        this.entryName = entryName;
    }

    public String getGeneNames() {
        return geneNames;
    }

    public void setGeneNames(String geneNames) {
        this.geneNames = geneNames;
    }

    public String getOrganism() {
        return organism;
    }

    public void setOrganism(String organism) {
        this.organism = organism;
    }

    public String getCommonOrganismName() {
        return commonOrganismName;
    }

    public void setCommonOrganismName(String commonOrganismName) {
        this.commonOrganismName = commonOrganismName;
    }

    public String getScientificOrganismName() {
        return scientificOrganismName;
    }

    public void setScientificOrganismName(String scientificOrganismName) {
        this.scientificOrganismName = scientificOrganismName;
    }

    public String getOrganismId() {
        return organismId;
    }

    public void setOrganismId(String organismId) {
        this.organismId = organismId;
    }

    public String getEntry() {
        return entry;
    }

    public void setEntry(String entry) {
        this.entry = entry;
    }

    public String getStatus() {
        return status;
    }

    public void setStatus(String status) {
        this.status = status;
    }

    public String getProteinNames() {
        return proteinNames;
    }

    public void setProteinNames(String proteinNames) {
        this.proteinNames = proteinNames;
    }

	public void setGeneName(String geneName) {
		this.geneName = geneName;
	}

	public String getGeneName() {
		return geneName;
	}

	public void setGeneNameSynonyms(List<String> geneSynonyms) {
		this.geneNameSynonyms = geneSynonyms;
	}

	public List<String> getGeneNameSynonyms() {
		return geneNameSynonyms;
	}

    public String getRecommendedName() {
        return recommendedName;
    }

    public void setRecommendedName(String recommendedName) {
        this.recommendedName = recommendedName;
    }

    public List<String> getAlternativeNames() {
        return alternativeNames;
    }

    public void setAlternativeNames(List<String> alternativeNames) {
        this.alternativeNames = alternativeNames;
    }
}
