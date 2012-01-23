package uk.ac.ebi.intact.dbupdate.dataset.selectors;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;
import uk.ac.ebi.intact.model.CvTopic;

import java.io.*;
import java.util.Calendar;
import java.util.HashSet;
import java.util.Set;

/**
 * The base implementation of DatasetSelector
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Jun-2010</pre>
 */

public abstract class DatasetSelectorImpl implements DatasetSelector {

    /**
     * The log for this class
     */
    protected static final Log log = LogFactory.getLog( DatasetSelectorImpl.class );

    /**
     * The column separator in the parameter file
     */
    protected static final String separator = "\t";

    /**
     * The name of the column for the organisms taxIds
     */
    protected static final String organism = "organism";

    /**
     * The name of the column for the publication excluded
     */
    protected static final String publication = "publication";

    /**
     * the value of the dataset the list of proteins are involved in
     */
    protected String datasetValue;

    /**
     * The list of possible taxIds
     */
    protected Set<String> listOfPossibleTaxId = new HashSet<String>();

    /**
     * The list of publications we want to remove
     */
    protected Set<String> listOfExcludedPublications = new HashSet<String>();

    /**
     * the maximum number of interactions per experiment which is accepted
     */
    protected static final int maxNumberOfInteractions = 100;

    /**
     * To know if a file should be written with the results of the selection
     */
    protected boolean isFileWriterEnabled = true;
    
    protected File report;

    /**
     * Create a new DatasetSelectorImpl with no dataset value and no intact context. These two variables must be initialised using the set methods
     */
    public DatasetSelectorImpl(){
        this.datasetValue = null;
        report = new File("proteins_selected_for_dataset_" + Calendar.getInstance().getTime().getTime()+".txt");
    }

    public DatasetSelectorImpl(String report){
        this.datasetValue = null;
        this.report = new File(report);
    }

    /**
     * Clear the list of intact objects we want to select, the list of possible taxIds, the list of publication excluded and the dataset value of this object
     */
    public void clearDatasetContent(){
        this.listOfPossibleTaxId.clear();
        this.listOfExcludedPublications.clear();
        this.datasetValue = null;
    }

    /**
     *
     * @return the list of possible taxIds
     */
    public Set<String> getListOfPossibleTaxId() {
        return listOfPossibleTaxId;
    }

    /**
     * Set the list of possible taxIds
     * @param listOfPossibleTaxId
     */
    public void setListOfPossibleTaxId(Set<String> listOfPossibleTaxId) {
        this.listOfPossibleTaxId = listOfPossibleTaxId;
    }

    /**
     * Extract the list of idsToExtract from a String. Each Id must be separated by semi-colon
     * @param idsToExtract
     */
    protected void extractListOfIdsFrom(String idsToExtract, Set<String> list){

        // the list of ids is not null
        if (idsToExtract != null){
            if (idsToExtract.length() > 0){
                // several ids are present in the String, we need to split them and collect them in a Set
                if (idsToExtract.contains(";")){
                    String [] splitIds = idsToExtract.split(";");

                    for (String o : splitIds){
                        list.add(o);
                    }
                }
                // the String is only composed of one taxId
                else {
                    list.add(idsToExtract);
                }
            }
        }
    }

    /**
     *
     * @param columns : the columns of a line in the file
     * @return true if the first column is 'organism'
     */
    protected boolean isOrganismLine(String[] columns){
        String o = columns[0];

        return organism.equalsIgnoreCase(o);
    }

    /**
     *
     * @param columns : the columns of a line in the file
     * @return true if the first column is 'publication'
     */
    protected boolean isPublicationLine(String[] columns){
        String o = columns[0];

        return publication.equalsIgnoreCase(o);
    }

    /**
     *
     * @param columns : the columns of a line in the file
     * @return true if the line contains the dataset information
     */
    protected boolean isDatasetLine(String[] columns){
        String mi = columns[1];
        String name = columns[0];

        // We have the Mi identifier, we look only at it
        if (mi != null && mi.length() > 0){
            return CvTopic.DATASET_MI_REF.equalsIgnoreCase(mi);
        }
        // we don't have a MI identifier, we will look at the name
        else {
            return CvTopic.DATASET.equalsIgnoreCase(name);
        }
    }

    /**
     * Remove the quote at the beginning and/or the end of the value if there are any quotes.
     * @param value : the String to check
     */
    protected String removeQuotes(String value){
        String newValue = value;

        // remove possible " at the beginning and the end of the value
        if (value.startsWith("\"")){
            newValue = value.substring(1);
        }
        if (value.endsWith("\"")){
            newValue = value.substring(0, value.length());
        }

        return newValue;
    }

    /**
     * Load the file containing the dataset information
     * @param file : the file containing the dataset information
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : if the file is null
     */
    protected void extractDatasetFromFile(File file) throws DatasetException {
        BufferedReader in = null;

        if (file == null){
            throw new IllegalArgumentException("The file containing the dataset is null and we can't read the dataset. Please set a valid datasetFile of this object.");
        }
        try {

            in = new BufferedReader( new FileReader( file ) );

            String str;
            while ( ( str = in.readLine() ) != null ) {
                // remove possible "
                str = removeQuotes(str);

                // we skip the comments and empty lines
                if ( str.startsWith( "#" ) || str.length() == 0 ) {
                    continue;
                }

                // the line contains columns
                if (str.contains(separator)){
                    final String[] columns = StringUtils.splitPreserveAllTokens(str,separator);

                    // we need 3 columns
                    if (columns.length != 3){
                        throw new IllegalArgumentException("The file containing the dataset is malformed : we found "+columns.length+" columns instead of 3. We need a column for the type of data (dataset, organism (can be empty if no organism limitations), gene name, protein name), a column containing the MI " +
                                "identifier of this type of data (for instance dataset has a MI number MI:0875, gene name has a MI number MI:0301)");
                    }
                    else {
                        // remove possible quotes
                        for (String c : columns){
                            c = removeQuotes(c);
                        }
                        // if the line contains dataset information, we initialises the dataset value of this object
                        if (isDatasetLine(columns)){
                            // if the dataset value is not null, it is replaced with the new one but it can be an error in the file
                            if (this.datasetValue != null && columns[2].length() > 0){
                                log.warn("The dataset value for the file was " + this.datasetValue + " but is replaced with the new value found in the file " + columns[2]);
                                this.datasetValue = columns[2];
                            }
                            else if (columns[2].length() > 0){
                                this.datasetValue = columns[2];
                            }
                        }
                        // if the line contains organism requirements, we initialise the list of organisms taxids
                        else if (isOrganismLine(columns)){
                            // if list of organism taxIds is not empty, it is totally cleared before adding the new organism requirements, but it can be an error in the file
                            if (!this.listOfPossibleTaxId.isEmpty()){
                                log.warn("The possible organisms for the file were " + this.listOfPossibleTaxId + " but are replaced with the new value(s) found in the file " + columns[2]);
                                this.listOfPossibleTaxId.clear();
                            }
                            extractListOfIdsFrom(columns[2], this.listOfPossibleTaxId);
                        }
                        // if the line contains publication exclusions, we initialise the list of publications to exclude
                        else if (isPublicationLine(columns)){
                            // if list of organism taxIds is not empty, it is totally cleared before adding the new organism requirements, but it can be an error in the file
                            if (!this.listOfExcludedPublications.isEmpty()){
                                log.warn("The publications to exclude for the file were " + this.listOfExcludedPublications + " but are replaced with the new value(s) found in the file " + columns[2]);
                                this.listOfExcludedPublications.clear();
                            }
                            extractListOfIdsFrom(columns[2], this.listOfExcludedPublications);
                        }
                        // we don't have neither dataset information, nor organism requirements, it is some specific info to load
                        else {
                            readSpecificContent(columns);
                        }
                    }
                }
                else {
                    throw new DatasetException("The file containing the dataset is malformed. We need a column for the type of data (dataset, organism (can be empty if no organism limitations), gene name, protein name), a column containing the MI " +
                            "identifier of this type of data (for instance dataset has a MI number MI:0875, gene name has a MI number MI:0301)");
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            throw new DatasetException("The file " + file.getAbsolutePath() + " has not been found.", e);
        } catch (IOException e) {
            throw new DatasetException("The file " + file.getAbsolutePath() + " couldn't be read.", e);
        }
    }

    /**
     * This method load columns containing specific information to retrieve intact objects. (interactor aliases, cross references, etc.)
     * @param columns
     * @throws DatasetException
     */
    protected abstract void readSpecificContent(String [] columns) throws DatasetException;

    /**
     * Read the file in the resources containing the dataset content
     * @param datasetFile : name of the dataset file
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException
     */
    public void readDatasetFromResources(String datasetFile) throws DatasetException {
        // always clear previous content
        clearDatasetContent();

        // The file should not be null
        if (datasetFile == null){
            throw new IllegalArgumentException("The name of the file containing the dataset is null and we can't read the dataset.");
        }

        File file = new File(DatasetSelectorImpl.class.getResource( datasetFile ).getFile());

        extractDatasetFromFile(file);
    }

    /**
     * Read the file containing the dataset content
     * @param datasetFile : name of the dataset file
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException
     */
    public void readDataset(String datasetFile) throws DatasetException {
        // always clear previous content
        clearDatasetContent();

        // The file should not be null
        if (datasetFile == null){
            throw new IllegalArgumentException("The name of the file containing the dataset is null and we can't read the dataset.");
        }

        File file = new File (datasetFile );

        extractDatasetFromFile(file);
    }

    /*private void addAllExperimentsFromSamePublication(List<Experiment> experiments){
        List<Experiment> otherExperiment = new ArrayList<Experiment>();
        ExperimentDao expDao = this.context.getDataContext().getDaoFactory().getExperimentDao();

        for (Experiment e : experiments){
            if (e.getPublication() != null){
                if (e.getPublication().getPublicationId() != null){
                    otherExperiment.addAll(expDao.getByPubId(e.getPublication().getPublicationId()));
                }
            }
        }

        experiments.addAll(otherExperiment);
    }*/

    /*private List<Experiment> getExperimentWithSamePubIdFrom(List<Experiment> experiments, String pubId){
        if (pubId == null){
            return null;
        }

        List<Experiment> expWithSamePubId = new ArrayList<Experiment>();

        for (Experiment e : experiments){
            if (e.getPublication() != null){
                if (pubId.equalsIgnoreCase(e.getPublication().getPublicationId())){
                    expWithSamePubId.add(e);
                }
            }
        }
        return expWithSamePubId;
    }*/

    /*private void removeExperimentsWithTooManyInteractions(List<Experiment> experiments){
        List<Experiment> experimentsToRemove = new ArrayList<Experiment>();

        for (Experiment exp : experiments){
            if (exp.getInteractions().size() >= maxNumberOfInteractions){
                if (exp.getPublication() == null){
                    experimentsToRemove.add(exp);
                }
                else {
                    experimentsToRemove.addAll(getExperimentWithSamePubIdFrom(experiments, exp.getPublication().getPublicationId()));
                }
            }
        }

        experiments.removeAll(experimentsToRemove);
    }*/

    /*private boolean hasExactDatasetAnnotation(Experiment e){
        if (e == null){
            return false;
        }
        Collection<Annotation> annotations = e.getAnnotations();

        for (Annotation a : annotations){
            CvTopic topic = a.getCvTopic();
            if (CvTopic.DATASET_MI_REF.equalsIgnoreCase(topic.getIdentifier())){
                if (a.getAnnotationText().equalsIgnoreCase(this.datasetValue)){
                    return true;
                }
            }
        }
        return false;
    }*/

    /**
     *
     * @return  the dataset value
     */
    public String getDatasetValueToAdd() {
        return this.datasetValue;
    }

    /**
     * Set the dataset value
     * @param dataset : dataset annotation
     */
    public void setDatasetValueToAdd(String dataset) {
        this.datasetValue = dataset;
    }

    /**
     *
     * @return the maximum number of interactions per experiment
     */
    public int getMaxNumberOfInteractionsPerExperiment() {
        return maxNumberOfInteractions;
    }

    /**
     *
     * @return  the list of publication ids we want to exclude
     */
    public Set<String> getPublicationsIdToExclude() {
        return this.listOfExcludedPublications;
    }

    /**
     * return the isFileWriterEnabled
     * @return
     */
    public boolean isFileWriterEnabled() {
        return isFileWriterEnabled;
    }

    /**
     * set the isFileWriterEnabled
     * @param fileWriterEnabled
     */
    public void setFileWriterEnabled(boolean fileWriterEnabled) {
        isFileWriterEnabled = fileWriterEnabled;
    }

    @Override
    public File getReport() {
        return this.report;
    }
}
