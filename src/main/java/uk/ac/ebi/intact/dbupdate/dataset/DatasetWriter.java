package uk.ac.ebi.intact.dbupdate.dataset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.DatasetSelector;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.component.ComponentDatasetSelector;
import uk.ac.ebi.intact.dbupdate.dataset.selectors.protein.ProteinDatasetSelector;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Experiment;
import uk.ac.ebi.intact.model.Publication;

import javax.persistence.Query;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * A DatasetWriter will add a dataset annotation to all the experiments of a same publication with at least one experiment
 * containing interaction(s) involving a specific object (protein, component, publication). It needs a DatasetSelector to collect the specific IntAct accessions of the object selected by the selector (proteins, components, etc.).
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Jun-2010</pre>
 */
public class DatasetWriter {

    /**
     * the log of this class
     */
    protected static final Log log = LogFactory.getLog( DatasetWriter.class );

    /**
     * The DatasetSelector instance of this object
     */
    protected DatasetSelector selector;

    /**
     * Contains the list of publications updated
     */
    protected HashSet<String> listOfpublicationUpdated = new HashSet<String>();

    protected HashSet<String> processedPublications = new HashSet<String>();

    /**
     * To know if a file should be written with the results of the update
     */
    protected boolean isFileWriterEnabled = true;

    protected File report;

    private int NUMBER_ACCESSION = 100;

    public DatasetWriter(){
        report = new File("dataset_report_" + Calendar.getInstance().getTime().getTime()+".txt");
    }

    public DatasetWriter(String report){
        this.report = new File(report);
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

    /**
     *
     * @param intactAccession : the protein accession in IntAct
     * @return the list of experiments for what a dataset annotation can be added if at least one experiment of a same publication
     * has interaction(s) involving this protein and no experiment of a same publication has more than 'maximumNumberOfInteractions' interactions
     * @throws DatasetException
     */
    private List<Publication> getPublicationsContainingProtein(String intactAccession) throws DatasetException {

        return getPublicationsContainingProteins(Arrays.asList(intactAccession));
    }

    private List<Publication> getPublicationsContainingProteins(Collection<String> intactAccessions) throws DatasetException {

        // get the intact datacontext and daofactory
        final DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // This query is looking for all experiences with at least one interaction involving the protein of interest
        String componentQuery = "select inter.ac from Component c join c.interactor as i join c.interaction as inter " +
                "where i.ac in (:accessionList)";

        return getPublicationsWithSpecificSelection(componentQuery, intactAccessions, daoFactory);
    }

    /**
     *
     * @param componentQuery : the query selecting experiments directly linked to the dataset
     * @param accessions : the accessions of the objects linked to the dataset (a protein, component, publication)
     * @param daoFactory
     * @return the list of experiments we want to add a dataset annotation to
     * @throws DatasetException
     */
    private List<Publication> getPublicationsWithSpecificSelection(String componentQuery, Collection<String> accessions, DaoFactory daoFactory) throws DatasetException{
        // This query is looking for all the publications containing the experiments of the previous query : componentQuery
        String publicationsContainingSpecificProteinsQuery = "select pub from Experiment exp2 join exp2.publication as pub join exp2.interactions as i2 where i2 in " +
                "("+componentQuery+")";

        // Create the query
        final Query query = daoFactory.getEntityManager().createQuery(publicationsContainingSpecificProteinsQuery);

        // Set the parameters of the query
        query.setParameter("accessionList", accessions);

        // get the results
        final List<Publication> publications = query.getResultList();

        return publications;
    }
    
    private boolean isPublicationEligibleForDataset(Publication p){

        // the publication is not excluded by the selector
        if (this.selector.getPublicationsIdToExclude().contains(p.getShortLabel())){
            return false;
        }
        
        // all the experiments must contain less than 100 interactions
        for (Experiment exp : p.getExperiments()){
            if (this.selector.getMaxNumberOfInteractionsPerExperiment() <= exp.getInteractions().size()){
                return false;
            }
        }
        
        return true;
    }

    /**
     *
     * @param intactAccession : the component accession in IntAct
     * @return the list of experiments for what a dataset annotation can be added if at least one experiment of a same publication
     * has interaction(s) involving this component and no experiment of a same publication has more than 'maximumNumberOfInteractions' interactions
     * @throws DatasetException
     */
    private List<Publication> getPublicationsContainingComponent(String intactAccession) throws DatasetException {

        return getPublicationsContainingComponents(Arrays.asList(intactAccession));
    }

    private List<Publication> getPublicationsContainingComponents(Collection<String> intactAccession) throws DatasetException {

        // get the intact datacontext and daofactory
        final DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // This query is looking for all experiences with at least one interaction involving the component of interest
        String componentQuery = "select i.ac from InteractionImpl i join i.components as c " +
                "where c.ac in (:accession)";

        return getPublicationsWithSpecificSelection(componentQuery, intactAccession, daoFactory);
    }

    /**
     *
     * @param finalQuery : the final query for the experiments to add a dataset
     * @return a new query as a String adding the publication restrictions to the previous query
     */
    private String removeExcludedPublications(String finalQuery){
        StringBuffer query = new StringBuffer(1640);

        // We don't have any publication restrictions so we don't change the previous query
        if (this.selector.getPublicationsIdToExclude().isEmpty()){
            return finalQuery;
        }

        // We want all the experiments with a publication id different from the list of publication ids to exclude
        query.append("select e2 from Experiment e2 join e2.publication as pubId where (" );

        for (String t : this.selector.getPublicationsIdToExclude()){
            query.append(" pubId.shortLabel <> '"+t+"' and");
        }

        query.delete(query.lastIndexOf("and"), query.length());
        query.append(") and e2 in ("+finalQuery+")");

        return query.toString();
    }

    /**
     *
     * @return a new Dataset annotation with the dataset value contained in the DatasetSelector of this object
     */
    private Annotation createNewDataset() throws DatasetException {
        CvTopic dataset = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao( CvTopic.class ).getByPsiMiRef( CvTopic.DATASET_MI_REF );

        if (dataset == null){
            throw new DatasetException("The CVTopic " + CvTopic.DATASET_MI_REF + " : " + CvTopic.DATASET + "doesn't exist in the database.");
        }

        Annotation annotation = new Annotation(dataset, this.selector.getDatasetValueToAdd());
        return annotation;
    }

    /**
     * Add the dataset annotation for each experiment in the list
     * @param publications : the publications
     */
    private void addDatasetToExperimentsAndPublication(List<Publication> publications) throws IOException, DatasetException {
        for (Publication p : publications){
            if (!processedPublications.contains(p.getAc()) && isPublicationEligibleForDataset(p)){
                processedPublications.add(p.getAc());

                String pubId = p.getShortLabel();

                // if publication has not been processed, we add the dataset to the publication
                if (this.listOfpublicationUpdated.add(pubId)){
                    Annotation annotation = createNewDataset();
                    log.info("Add dataset to " + p.getAc() + ": " + p.getShortLabel());

                    p.addAnnotation(annotation);

                    for (Experiment exp : p.getExperiments()){
                        Annotation annotation2 = createNewDataset();
                        log.info("Add dataset to " + exp.getAc() + ": " + exp.getShortLabel());

                        exp.addAnnotation(annotation2);

                        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(exp);
                    }

                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(p);
                }
            }
        }
    }

    /**
     * Use the selector to select the list of protein of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of object criterias that the selector is using to select the object of interests can be stored in a file but it is not mandatory, the file can be null
     * @param file : the file containing the list of proteins. Can be null if it is not needed
     * @param selector : the DatasetSelector
     * @throws DatasetException
     */
    public void addDatasetAnnotationToExperimentsFor(String file, DatasetSelector selector) throws DatasetException {
        // set the DatasetSelector of this object
        setSelector(selector);

        addDatasetAnnotationToExperimentsAndPublicationsFor(file);
    }

    /**
     * Use the selector to select the list of object of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of object criterias that the selector is using to select the object of interests can be stored in a file but it is not mandatory, the file can be null
     * @param file : the file containing the list of proteins. Can be null if it is not needed
     * @throws DatasetException
     */
    public void addDatasetAnnotationToExperimentsAndPublicationsFor(String file) throws DatasetException {

        // if the file is null, we are supposing that the selector already contains the list of proteins and the dataset value, otherwise, we need to read the file and initialise the selector
        if (file != null){
            log.info("Load resource " + file);
            this.selector.readDatasetFromResources(file);
        }

        addDatasetAnnotationToExperimentsAndPublications();
    }

    /**
     * Use the selector to select the list of object of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of object criterias that the selector is using to select the object of interests can be stored in a file but it is not mandatory, the file can be null
     * @throws DatasetException
     */
    public void addDatasetAnnotationToExperimentsAndPublications() throws DatasetException {

        // The DatasetSelector must be not null
        if (this.selector == null){
            throw new DatasetException("The selector has not been initialised, we can't determine the list of proteins to look for.");
        }

        // The selector must have a dataset value
        if (this.selector.getDatasetValueToAdd() == null){
            throw new DatasetException("The dataset value to add for the selector has not been initialised, we can't determine the dataset value to add on each experiment containing the proteins of this dataset.");
        }
        // The selector must have a maximum number of interactions per experiment
        if (this.selector.getMaxNumberOfInteractionsPerExperiment() == 0){
            throw new DatasetException("The maximum number of interactions per experiment acceptable is 0. We will not be able to add the dataset annotation to any experiments.");
        }

        log.info("Start transaction...");
        try {

            processDatasetSelection();

        } catch (IOException e) {
            throw new DatasetException("We can't write the results of the dataset update.");
        }
    }

    /**
     * Write a report about the experiment dataset update
     * @param numberOfProteinSelected
     * @param totalNumberOfExperiments
     * @throws IOException
     */
    private void writeDatasetReport(int numberOfProteinSelected, int totalNumberOfExperiments) throws IOException {
        if (isFileWriterEnabled){
            // create the file where to write the report
            Writer writer = new FileWriter(report);

            writer.write(numberOfProteinSelected + " proteins have been selected for the dataset '" + this.selector.getDatasetValueToAdd() + "' \n \n");

            writer.write("\nThe dataset '" + this.selector.getDatasetValueToAdd() + "' has been added to a total of " + totalNumberOfExperiments + " experiments. \n \n");

            for (String p : this.listOfpublicationUpdated){
                writer.write(p + "\n \n");
            }

            writer.close();
        }
    }

    /**
     *
     * @return  the selector instance of this object
     */
    public DatasetSelector getSelector() {
        return selector;
    }

    /**
     * set the dataset selector
     * @param selector
     */
    public void setSelector(DatasetSelector selector) {
        this.selector = selector;
    }

    /**
     * Depending on the type of selector (ProteinDatasetSelector, ComponentDatasetSelector, etc..), we will process differently the query to get the experiments we want to add a dataset to.
     * @throws DatasetException
     * @throws IOException
     */
    private void processDatasetSelection() throws DatasetException, IOException {

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        // number of intcact object related to the dataset
        int numberOfElementSelected = 0;
        // total number of experiments updated
        int numberOfExperiments = 0;

        // we want to retrieve experiments containing interactions involving specific proteins
        if (this.selector instanceof ProteinDatasetSelector){
            ProteinDatasetSelector proteinSelector = (ProteinDatasetSelector) this.selector;

            Set<String> proteinSelected = proteinSelector.collectSelectionOfProteinAccessionsInIntact();

            numberOfElementSelected = proteinSelected.size();

            log.info(proteinSelected.size() + " proteins have been selected for the dataset '" + this.selector.getDatasetValueToAdd() + "' \n \n");

            Collection<String> proteinAcs = new ArrayList<String>();
            int totalSize = proteinSelected.size();
            int processedAcs = 0;

            // for each protein of interest
            for (String accession : proteinSelected){

                proteinAcs.add(accession);
                processedAcs ++;

                if (proteinAcs.size() >= NUMBER_ACCESSION || processedAcs == totalSize){
                    TransactionStatus transactionStatus = dataContext.beginTransaction();

                    log.info("Processed proteins : "+processedAcs+". Collect experiments...");

                    // add the dataset annotation
                    List<Publication> publicationsToAddDataset = getPublicationsContainingProteins(proteinAcs);

                    numberOfExperiments += publicationsToAddDataset.size();

                    log.info("Add dataset " + this.selector.getDatasetValueToAdd() + " to " +publicationsToAddDataset.size() + " experiments containing interaction(s) involving the protein " + accession + " and " + proteinAcs.size() + " other proteins \n");
                    addDatasetToExperimentsAndPublication(publicationsToAddDataset);

                    dataContext.commitTransaction(transactionStatus);

                    proteinAcs.clear();
                }
            }
        }
        // we want to retrieve experiments containing interactions involving specific components (participants)
        else if (this.selector instanceof ComponentDatasetSelector){
            ComponentDatasetSelector componentSelector = (ComponentDatasetSelector) this.selector;

            Set<String> componentSelected = componentSelector.getSelectionOfComponentAccessionsInIntact();

            numberOfElementSelected = componentSelected.size();

            log.info(componentSelected.size() + " components have been selected for the dataset '" + this.selector.getDatasetValueToAdd() + " and "+componentSelected.size()+" other components \n");

            // for each component of interest

            Collection<String> componentAcs = new ArrayList<String>();
            int totalSize = componentSelected.size();
            int processedAcs = 0;

            // for each protein of interest
            for (String accession : componentSelected){

                componentAcs.add(accession);
                processedAcs ++;

                if (componentAcs.size() >= NUMBER_ACCESSION || processedAcs == totalSize){
                    TransactionStatus transactionStatus = dataContext.beginTransaction();

                    log.info("Processed components : "+processedAcs+". Collect experiments...");

                    // add the dataset annotation
                    List<Publication> publicationToAddDataset = getPublicationsContainingComponents(componentAcs);

                    numberOfExperiments += publicationToAddDataset.size();

                    log.info("Add dataset " + this.selector.getDatasetValueToAdd() + " to " +publicationToAddDataset.size() + " experiments containing interaction(s) involving the component " + accession + " \n");
                    addDatasetToExperimentsAndPublication(publicationToAddDataset);

                    dataContext.commitTransaction(transactionStatus);

                    componentAcs.clear();
                }
            }
        }

        log.info("\n The dataset '" + this.selector.getDatasetValueToAdd() + "' has been added to a total of " + numberOfExperiments + " experiments. \n");

        writeDatasetReport(numberOfElementSelected, numberOfExperiments);
    }

    /**
     *
     * @return the list of pubmed Id and title of the publications updated
     */
    public HashSet<String> getListOfpublicationUpdated() {
        return listOfpublicationUpdated;
    }

    /**
     * Set the list of Publications updated
     * @param listOfpublicationUpdated
     */
    public void setListOfpublicationUpdated(HashSet<String> listOfpublicationUpdated) {
        this.listOfpublicationUpdated = listOfpublicationUpdated;
    }
}

