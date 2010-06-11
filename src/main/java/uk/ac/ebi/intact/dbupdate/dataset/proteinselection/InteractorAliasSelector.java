package uk.ac.ebi.intact.dbupdate.dataset.proteinselection;

import org.apache.commons.lang.StringUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.CvAliasType;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.ProteinImpl;

import javax.persistence.Query;
import java.io.*;
import java.util.*;

/**
 * An InteractorAliasSelector is a ProteinDatasetSelector which is collecting proteins in IntAct with a specific
 * alias (gene name, orf,...) and organism. Usually the list of aliases as well as the organisms and the dataset value are in a configuration file
 * but we can use the methods setListOfProteins, setDatasetValue and setPossibleTaxId if the data are not taken from a file.
 * The configuration file of this selector must have 3 columns : one for the name of the parameter (ex : dataset), one for the mi number of this parameter (ex : MI:0875)
 * and one for the value of the parameter (ex : Interactions of proteins with an established role in the presynapse.).
 * We must have a line containing the dataset value, another containing the list of organism taxIds (separated by semi-colon) which can
 * be empty and the other lines contain gene names, orfs and/or other interactor aliases (Ex of line : gene name    MI:0301 AMPH).
 * All the columns are tab separated.
 * You can add an extra line containing publication to exclude (column name = publication and the pubmed ids are seperated by semi-colon)).
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public class InteractorAliasSelector implements ProteinDatasetSelector {

    /**
     * The log for this class
     */
    private static final Log log = LogFactory.getLog( InteractorAliasSelector.class );

    /**
     * The column separator in the parameter file
     */
    private static final String separator = "\t";

    /**
     * The name of the column for the organisms taxIds
     */
    private static final String organism = "organism";

    /**
     * The name of the column for the publication excluded
     */
    private static final String publication = "publication";

    /**
     * the value of the dataset the list of proteins are involved in
     */
    private String datasetValue;

    /**
     * The list of possible taxIds
     */
    private Set<String> listOfPossibleTaxId = new HashSet<String>();

    /**
     * The list of publications we want to remove
     */
    private Set<String> listOfExcludedPublications = new HashSet<String>();

    /**
     * The map containing the type of the alias (ex : gene name) associated with a set of names (ex : AMPH, APBA1, APBA2, ...)
     */
    private Map<CvAliasType, Set<String>> listOfProteins = new HashMap<CvAliasType, Set<String>>();

    /**
     * the maximum number of interactions per experiment which is accepted
     */
    private static final int maxNumberOfInteractions = 100;

    /**
     * the intact context
     */
    private IntactContext context;

    /**
     * To know if a file should be written with the results of the selection
     */
    private boolean isFileWriterEnabled = true;

    /**
     * Create a new InteractorAliasSelector with no dataset value and no intact context. These two variables must be initialised using the set methods
     */
    public InteractorAliasSelector(){
        this.datasetValue = null;

        this.context = null;
    }

    /**
     * Create a new InteractorAliasSelector with no dataset value and an intact context. The dataset must be initialised using the set method
     * @param context
     */
    public InteractorAliasSelector(IntactContext context){
        this.datasetValue = null;

        this.context = context;
    }

    /**
     *
     * @return the intact context
     */
    public IntactContext getIntactContext() {
        return context;
    }

    /**
     * set the intact context
     * @param context : the intact context
     */
    public void setIntactContext(IntactContext context) {
        this.context = context;
    }

    /**
     * Clear the list of proteins, the list of possible taxIds, the list of publication excluded and the dataset value of this object
     */
    public void clearDatasetContent(){
        this.listOfProteins.clear();
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
     *
     * @return the map containing the list of aliases
     */
    public Map<CvAliasType, Set<String>> getListOfProteins() {
        return listOfProteins;
    }

    /**
     * set the map containing the list of aliases
     * @param listOfProteins
     */
    public void setListOfProteins(Map<CvAliasType, Set<String>> listOfProteins) {
        this.listOfProteins = listOfProteins;
    }

    /**
     * Extract the list of idsToExtract from a String. Each Id must be separated by semi-colon
     * @param idsToExtract
     */
    private void extractListOfIdsFrom(String idsToExtract, Set<String> list){

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
    private boolean isOrganismLine(String[] columns){
        String o = columns[0];

        return organism.equalsIgnoreCase(o);
    }

    /**
     *
     * @param columns : the columns of a line in the file
     * @return true if the first column is 'publication'
     */
    private boolean isPublicationLine(String[] columns){
        String o = columns[0];

        return publication.equalsIgnoreCase(o);
    }

    /**
     *
     * @param columns : the columns of a line in the file
     * @return true if the line contains the dataset information
     */
    private boolean isDatasetLine(String[] columns){
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
     * This method read the columns of a line in the file and initialises either the listOfPossibleTaxIds, listOfProteins or dataset value.
     * It reads the content of the file and load the list of proteins
     * @param columns : the columns of a line in a file
     * @throws ProteinSelectorException : if the intact context is not set
     */
    private void addNewProtein(String [] columns) throws ProteinSelectorException {
        // the mi identifier of the alias type is the second column
        String mi = columns[1];
        // the name of the alias type is the first column
        String name = columns[0];

        // The alias type
        CvAliasType aliasType = null;

        // we need an Intact context
        if (this.context == null){
            throw new ProteinSelectorException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        if (columns [2].length() > 0){
            // get the aliastype instance
            if (mi != null && mi.length() > 0){
                aliasType = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvAliasType.class ).getByPsiMiRef( mi );
            }
            else {
                if (name.length() > 0){
                    aliasType = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvAliasType.class ).getByShortLabel( name );
                }
            }

            if (aliasType == null){
                throw new ProteinSelectorException("The alias type " + mi + ": "+name+" is not known and it is a mandatory field.");
            }

            // we have not already loaded this alias type, we add the new alias with its alias type
            if (!this.listOfProteins.containsKey(aliasType)){
                Set<String> values = new HashSet<String>();
                values.add(columns[2]);
                this.listOfProteins.put(aliasType, values);
            }
            // the alias type is already loaded, we add the new alias only
            else {
                Set<String> values = this.listOfProteins.get(aliasType);

                values.add(columns[2]);
            }
        }
    }

    /**
     * Remove the quote at the beginning and/or the end of the value if there are any quotes.
     * @param value : the String to check
     */
    private String removeQuotes(String value){
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
     * Load the list of proteins aliases contained in the file
     * @param file : the file containing the protein aliases
     * @throws ProteinSelectorException : if the file is null
     */
    private void extractDatasetFromFile(File file) throws ProteinSelectorException {

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
                        // we don't have neither dataset information, nor organism requirements, it is a protein alias to load
                        else {
                            addNewProtein(columns);
                        }
                    }
                }
                else {
                    throw new ProteinSelectorException("The file containing the dataset is malformed. We need a column for the type of data (dataset, organism (can be empty if no organism limitations), gene name, protein name), a column containing the MI " +
                            "identifier of this type of data (for instance dataset has a MI number MI:0875, gene name has a MI number MI:0301)");
                }
            }
            in.close();
        } catch (FileNotFoundException e) {
            throw new ProteinSelectorException("The file " + file.getAbsolutePath() + " has not been found.", e);
        } catch (IOException e) {
            throw new ProteinSelectorException("The file " + file.getAbsolutePath() + " couldn't be read.", e);
        }
    }

    /**
     * Read the file in the resources containing the list of aliases
     * @param datasetFile : name of the dataset file
     * @throws ProteinSelectorException
     */
    public void readDatasetFromResources(String datasetFile) throws ProteinSelectorException {
        // always clear previous content
        clearDatasetContent();

        // The file should not be null
        if (datasetFile == null){
            throw new IllegalArgumentException("The name of the file containing the dataset is null and we can't read the dataset.");
        }

        File file = new File(InteractorAliasSelector.class.getResource( datasetFile ).getFile());

        extractDatasetFromFile(file);
    }

    /**
     * Read the file containing the list of aliases
     * @param datasetFile : name of the dataset file
     * @throws ProteinSelectorException
     */
    public void readDataset(String datasetFile) throws ProteinSelectorException {
        // always clear previous content
        clearDatasetContent();

        // The file should not be null
        if (datasetFile == null){
            throw new IllegalArgumentException("The name of the file containing the dataset is null and we can't read the dataset.");
        }

        File file = new File (datasetFile );

        extractDatasetFromFile(file);
    }

    /**
     *
     * @param interactorQuery : the query to select interactors with specific aliases
     * @return a new query as a String adding the organism restrictions to the previous query
     */
    private String addOrganismSelection(String interactorQuery){
        StringBuffer query = new StringBuffer(1640);

        // We don't have any organism restrictions so we don't change the previous query
        if (this.listOfPossibleTaxId.isEmpty()){
            return interactorQuery;
        }

        // We want all the interactors with an organism which is contained in the list of organism restrictions and with a specific alias
        query.append("select i.ac from InteractorImpl i join i.bioSource as b where (" );

        for (String t : listOfPossibleTaxId){
            query.append(" b.taxId = '"+t+"' or");
        }

        query.delete(query.lastIndexOf("or"), query.length());
        query.append(") and i.ac in ("+interactorQuery+")");

        return query.toString();
    }

    /**
     *
     * @param type : alias type
     * @param name : alias name
     * @return the list of intact proteins matching the alias name for this alias type and with an organism respecting the restrictions
     */
    private List<String> getProteinAccessionsContainingAlias (CvAliasType type, String name){

        // get the intact datacontext and factory
        final DataContext dataContext = this.context.getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // we want all the interactor associated with this alias name and this alias type
        String interactorGeneQuery = "select prot.ac from InteractorAlias ia join ia.parent as prot join ia.cvAliasType as alias " +
                "where upper(ia.name) = upper(:name) and alias = :alias and prot.objClass = :objclass";

        // we add the organism restrictions
        String finalQuery = addOrganismSelection(interactorGeneQuery);

        // create the query
        final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

        // set the query parameters
        query.setParameter("name", name);
        query.setParameter("alias", type);
        query.setParameter("objclass", ProteinImpl.class.getName());

        // the list of results
        final List<String> interactorAcs = query.getResultList();

        return interactorAcs;
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
     * @return the Set of protein accessions matching ont of the aliases in the list and respecting the organism restrictions.
     * @throws ProteinSelectorException
     */
    public Set<String> getSelectionOfProteinAccessionsInIntact() throws ProteinSelectorException {
        // we need an Intact context to query Intact
        if (this.context == null){
            throw new ProteinSelectorException("The intact context must be not null, otherwise the protein selector can't query the Intact database.");
        }

        log.info("Collect proteins in Intact...");

        // The map containing the protein aliases should be initialised before
        if (this.listOfProteins.isEmpty()){
            throw new IllegalArgumentException("The list of protein aliases has not been initialised.");
        }
        // The dataset value associated with the list of protein aliases should be initialised before
        if (this.datasetValue == null){
            throw new IllegalArgumentException("The dataset value has not been initialised.");
        }

        // create the file where to write the report
        File file = new File("proteins_selected_for_dataset_" + Calendar.getInstance().getTime().getTime()+".txt");
        Set<String> proteinAccessions = new HashSet<String>();

        try {
            Writer writer = new FileWriter(file);

            // For each alias type in the file
            for (Map.Entry<CvAliasType, Set<String>> entry : this.listOfProteins.entrySet()){
                // For each alias name associated with this alias type
                for (String name : entry.getValue()){
                    // get the intact proteins matching the alias name and alias type
                    List<String> proteinAccessionsForName = getProteinAccessionsContainingAlias(entry.getKey(), name);
                    // we add the proteins to the list
                    proteinAccessions.addAll(proteinAccessionsForName);
                    writer.write("Collect "+proteinAccessionsForName.size()+" proteins ("+proteinAccessionsForName+") associated with the name " + name + " \n");
                    writer.flush();
                }
            }

            writer.close();

            if (!isFileWriterEnabled()){
                file.delete();
            }

        } catch (IOException e) {
            throw new ProteinSelectorException("We can't write the results of the protein selection.", e);
        }

        return proteinAccessions;

    }

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
     * Set the list of publications to exclude
     * @param listOfExcludedPublications
     */
    public void setListOfExcludedPublications(Set<String> listOfExcludedPublications) {
        this.listOfExcludedPublications = listOfExcludedPublications;
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
}
