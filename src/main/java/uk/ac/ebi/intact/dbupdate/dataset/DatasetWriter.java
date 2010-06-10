package uk.ac.ebi.intact.dbupdate.dataset;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Experiment;

import javax.persistence.Query;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.io.Writer;
import java.util.*;

/**
 * A DatasetWriter will add a dataset annotation to all the experiments of a same publication with at least one experiment
 * containing interaction(s) involving a specific protein. It needs a ProteinDatasetSelector to collect the specific protein accessions in Intact.
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Jun-2010</pre>
 */
public class DatasetWriter {

    /**
     * the log of this class
     */
    private static final Log log = LogFactory.getLog( DatasetWriter.class );

    /**
     * The ProteinDatasetSelector instance of this object
     */
    private ProteinDatasetSelector proteinSelector;

    /**
     * The intact context
     */
    private IntactContext context;

    /**
     * Contains the list of publications updated
     */
    private HashSet<String> listOfpublicationUpdated = new HashSet<String>();

    /**
     * To know if a file should be written with the results of the update
     */
    private boolean isFileWriterEnabled = true;

    /**
     * Create a new DatasetWriter with a specific intact context for this database
     * @param context : the intact context
     */
    public DatasetWriter(IntactContext context){
        this.proteinSelector = null;
        this.context = context;
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
     * @throws ProteinSelectorException
     */
    private List<Experiment> getExperimentsContainingProtein (String intactAccession) throws ProteinSelectorException {

        // get the intact datacontext and daofactory
        final DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // This query is looking for all experiences with at least one interaction involving the protein of interest
        String componentQuery = "select exp.ac from Component c join c.interactor as i join c.interaction as inter join inter.experiments " +
                "as exp where i.ac = :accession";

        // This query is looking for all the publications containing the experiments of the previous query : componentQuery
        String publicationsContainingSpecificProteinsQuery = "select pub.ac from Experiment exp2 join exp2.publication as pub join exp2.interactions as i2 where exp2 in " +
                "("+componentQuery+")";

        // This query is looking for all the experiments which have a publication accession equal to one of the publications accessions retrieved previously with the query : publicationsContainingSpecificProteinsQuery
        // and wich have a number of interactions which is superior or equal to the maximumNumberOfInteractions.
        String experimentWithTooManyInteractionsQuery = "select exp3.ac from Experiment exp3 join exp3.publication as pub2 join exp3.interactions as i3 where pub2.ac in ("+publicationsContainingSpecificProteinsQuery+") group by exp3.ac having count(i3.ac) >= :max";

        // This query is looking for all the publications containing one of the experiments with too many interactions (previous result of experimentWithTooManyInteractionsQuery)
        String publicationWithTooManyInteractionsQuery = "select pub3.ac from Experiment exp4 join exp4.publication as pub3 where exp4.ac in ("+experimentWithTooManyInteractionsQuery+")";

        // This query is looking for all the experiments of a same publication involving the protein of interest, but no one has more than the maximum Number Of Interactions
        String experimentsWithCorrectInteractionNumberQuery = "select distinct exp5.ac from Experiment exp5 join exp5.publication as pub4 where pub4.ac in ("+publicationsContainingSpecificProteinsQuery+") and pub4.ac not in ("+publicationWithTooManyInteractionsQuery+")";

        // This query is looking for all the experiments resulting of the previous query (experimentsWithCorrectInteractionNumberQuery) with the dataset annotation already added
        String experimentsWithDatasetAlreadyPresent = "select exp6.ac from Experiment exp6 join exp6.annotations as a join a.cvTopic as cv where exp6.ac in ("+experimentsWithCorrectInteractionNumberQuery+") and cv.identifier = :dataset and a.annotationText = :text";

        // This query is looking for all the experiments resulting of the query (experimentsWithCorrectInteractionNumberQuery) with the dataset annotation which is not added yet
        String finalQuery = "select exp7 from Experiment exp7 where exp7.ac in ("+experimentsWithCorrectInteractionNumberQuery+") and exp7.ac not in ("+experimentsWithDatasetAlreadyPresent+")";

        // If some publications should be excluded
        finalQuery = removeExcludedPublications(finalQuery);

        // Create the query
        final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

        // Set the parameters of the query
        query.setParameter("accession", intactAccession);
        query.setParameter("max", (long) this.proteinSelector.getMaxNumberOfInteractionsPerExperiment());
        query.setParameter("dataset", CvTopic.DATASET_MI_REF);
        query.setParameter("text", this.proteinSelector.getDatasetValueToAdd());

        // get the results
        final List<Experiment> experiments = query.getResultList();

        return experiments;
    }

    /**
     *
     * @param finalQuery : the final query for the experiments to add a dataset
     * @return a new query as a String adding the publication restrictions to the previous query
     */
    private String removeExcludedPublications(String finalQuery){
        StringBuffer query = new StringBuffer(1640);

        // We don't have any publication restrictions so we don't change the previous query
        if (this.proteinSelector.getPublicationsIdToExclude().isEmpty()){
            return finalQuery;
        }

        // We want all the experiments with a publication id different from the list of publication ids to exclude
        query.append("select e2 from Experiment e2 join e2.publication as pubId where (" );

        for (String t : this.proteinSelector.getPublicationsIdToExclude()){
            query.append(" pubId.shortLabel <> '"+t+"' and");
        }

        query.delete(query.lastIndexOf("and"), query.length());
        query.append(") and e2 in ("+finalQuery+")");

        return query.toString();
    }

    /**
     * Get a list of experiments involving this protein to remove the dataset annotation if it exists
     * @param intactAccession
     * @return
     * @throws ProteinSelectorException
     */
    private List<Experiment> getExperimentsContainingDatasetToRemoveFor (String intactAccession) throws ProteinSelectorException {

        // get the intact datacontext and daofactory
        final DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final DaoFactory daoFactory = dataContext.getDaoFactory();

        // This query is looking for all experiences with at least one interaction involving the protein of interest
        String componentQuery = "select exp.ac from Component c join c.interactor as i join c.interaction as inter join inter.experiments " +
                "as exp where i.ac = :accession";

        // This query is looking for all the publications containing the experiments of the previous query : componentQuery
        String publicationsContainingSpecificProteinsQuery = "select pub.ac from Experiment exp2 join exp2.publication as pub join exp2.interactions as i2 where exp2 in " +
                "("+componentQuery+")";

        // This query is looking for all the experiments which have a publication accession equal to one of the publications accessions retrieved previously with the query : publicationsContainingSpecificProteinsQuery
        // and wich have a number of interactions which is superior or equal to the maximumNumberOfInteractions.
        String experimentWithTooManyInteractionsQuery = "select exp3.ac from Experiment exp3 join exp3.publication as pub2 join exp3.interactions as i3 where pub2.ac in ("+publicationsContainingSpecificProteinsQuery+") group by exp3.ac having count(i3.ac) >= :max";

        // This query is looking for all the publications containing one of the experiments with too many interactions (previous result of experimentWithTooManyInteractionsQuery)
        String publicationWithTooManyInteractionsQuery = "select pub3.ac from Experiment exp4 join exp4.publication as pub3 where exp4.ac in ("+experimentWithTooManyInteractionsQuery+")";

        // This query is looking for all the experiments of a same publication involving the protein of interest, but no one has more than the maximum Number Of Interactions
        String experimentsWithCorrectInteractionNumberQuery = "select distinct exp5.ac from Experiment exp5 join exp5.publication as pub4 where pub4.ac in ("+publicationsContainingSpecificProteinsQuery+") and pub4.ac not in ("+publicationWithTooManyInteractionsQuery+")";

        // This query is looking for all the experiments resulting of the query (experimentsWithCorrectInteractionNumberQuery)
        String finalQuery = "select exp7 from Experiment exp7 where exp7.ac in ("+experimentsWithCorrectInteractionNumberQuery+")";

        // If some publications should be excluded
        finalQuery = removeExcludedPublications(finalQuery);

        // Create the query
        final Query query = daoFactory.getEntityManager().createQuery(finalQuery);

        // Set the parameters of the query
        query.setParameter("accession", intactAccession);
        query.setParameter("max", (long) this.proteinSelector.getMaxNumberOfInteractionsPerExperiment());

        // get the results
        final List<Experiment> experiments = query.getResultList();

        return experiments;
    }

    /**
     *
     * @return a new Dataset annotation with the dataset value contained in the ProteinDatasetSelector of this object
     */
    private Annotation createNewDataset() throws ProteinSelectorException {
        CvTopic dataset = this.context.getDataContext().getDaoFactory().getCvObjectDao( CvTopic.class ).getByPsiMiRef( CvTopic.DATASET_MI_REF );

        if (dataset == null){
            throw new ProteinSelectorException("The CVTopic " + CvTopic.DATASET_MI_REF + " : " + CvTopic.DATASET + "doesn't exist in the database.");
        }

        Annotation annotation = new Annotation(dataset, this.proteinSelector.getDatasetValueToAdd());
        return annotation;
    }

    /**
     * Add the dataset annotation for each experiment in the list
     * @param experiments : the experiments
     */
    private void addDatasetToExperiments(List<Experiment> experiments) throws IOException, ProteinSelectorException {
        for (Experiment e : experiments){
            String pubId = e.getPublication() != null ? e.getPublication().getPublicationId() : "No publication object";
            this.listOfpublicationUpdated.add(pubId + " \t" + e.getFullName());
            Annotation annotation = createNewDataset();
            e.addAnnotation(annotation);
            this.context.getCorePersister().saveOrUpdate(e);
        }
    }

    /**
     * Remove the dataset annotation from the experiments in the list
     * @param experiments
     */
    private void removeDatasetToExperiments(List<Experiment> experiments){

        for (Experiment e : experiments){
            log.info("Experiment " + e.getAc() + " "+e.getShortLabel());
            Annotation annotation = null;
            Collection<Annotation> annotations = e.getAnnotations();

            for (Annotation a : annotations){
                CvTopic topic = a.getCvTopic();
                if (CvTopic.DATASET_MI_REF.equalsIgnoreCase(topic.getIdentifier())){
                    if (a.getAnnotationText().equalsIgnoreCase(this.proteinSelector.getDatasetValueToAdd())){
                        annotation = a;
                        break;
                    }
                }
            }

            if (annotation != null){
                e.removeAnnotation(annotation);
                this.context.getCorePersister().saveOrUpdate(e);
                log.info("Dataset removed");
            }
        }
    }

    /**
     * Use the selector to select the list of protein of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of protein criterias that the selector is using to select the protein of interests can be stored in a file but it is not mandatory, the file can be null
     * @param file : the file containing the list of proteins. Can be null if it is not needed
     * @param selector : the ProteinDatasetSelector
     * @throws ProteinSelectorException
     */
    public void addDatasetAnnotationToExperimentsFor(String file, ProteinDatasetSelector selector) throws ProteinSelectorException {
        // set the ProteinDatasetSelector of this object
        setProteinSelector(selector);

        addDatasetAnnotationToExperimentsFor(file);
    }

    /**
     * Use the selector to select the list of protein of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of protein criterias that the selector is using to select the protein of interests can be stored in a file but it is not mandatory, the file can be null
     * @param file : the file containing the list of proteins. Can be null if it is not needed
     * @throws ProteinSelectorException
     */
    public void addDatasetAnnotationToExperimentsFor(String file) throws ProteinSelectorException {

        // if the file is null, we are supposing that the selector already contains the list of proteins and the dataset value, otherwise, we need to read the file and initialise the selector
        if (file != null){
            log.info("Load resource " + file);
            this.proteinSelector.readDatasetFromResources(file);
        }

        addDatasetAnnotationToExperiments();
    }

    /**
     * Use the selector to select the list of protein of interest in Intact and add the dataset annotation for all the publications involving one of these proteins.
     * The list of protein criterias that the selector is using to select the protein of interests can be stored in a file but it is not mandatory, the file can be null
     * @throws ProteinSelectorException
     */
    public void addDatasetAnnotationToExperiments() throws ProteinSelectorException {

        // The ProteinDatasetSelector must be not null
        if (this.proteinSelector == null){
            throw new ProteinSelectorException("The proteinSelector has not been initialised, we can't determine the list of proteins to look for.");
        }

        // The protein selector must have a dataset value
        if (this.proteinSelector.getDatasetValueToAdd() == null){
            throw new ProteinSelectorException("The dataset value to add for the proteinSelector has not been initialised, we can't determine the dataset value to add on each experiment containing the proteins of this dataset.");
        }
        // The protein selector must have a maximum number of interactions per experiment
        if (this.proteinSelector.getMaxNumberOfInteractionsPerExperiment() == 0){
            throw new ProteinSelectorException("The maximum number of interactions per experiment acceptable is 0. We will not be able to add the dataset annotation to any experiments.");
        }

        log.info("Start transaction...");
        try {

            Set<String> proteinSelected = this.proteinSelector.getSelectionOfProteinAccessionsInIntact();

            log.info(proteinSelected.size() + " proteins have been selected for the dataset '" + this.proteinSelector.getDatasetValueToAdd() + "' \n \n");

            int totalNumberOfExperiments = 0;
            // for each protein of interest
            for (String accession : proteinSelected){
                //TransactionStatus transactionStatus = this.context.getDataContext().beginTransaction();

                // add the dataset annotation
                List<Experiment> experimentToAddDataset = getExperimentsContainingProtein(accession);

                totalNumberOfExperiments += experimentToAddDataset.size();

                log.info("Add dataset " + this.proteinSelector.getDatasetValueToAdd() + " to "+experimentToAddDataset.size()+" experiments containing interaction(s) involving the protein " + accession  + " \n");
                addDatasetToExperiments(experimentToAddDataset);

                //this.context.getDataContext().commitTransaction(transactionStatus);
            }
            log.info("\n The dataset '" + this.proteinSelector.getDatasetValueToAdd() + "' has been added to a total of " + totalNumberOfExperiments + " experiments. \n");

            writeDatasetReport(proteinSelected.size(), totalNumberOfExperiments);
        } catch (IOException e) {
            throw new ProteinSelectorException("We can't write the results of the dataset update.");
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
            File file = new File("dataset_report_" + Calendar.getInstance().getTime().getTime()+".txt");
            Writer writer = new FileWriter(file);

            writer.write(numberOfProteinSelected + " proteins have been selected for the dataset '" + this.proteinSelector.getDatasetValueToAdd() + "' \n \n");

            writer.write("\nThe dataset '" + this.proteinSelector.getDatasetValueToAdd() + "' has been added to a total of " + totalNumberOfExperiments + " experiments. \n \n");

            for (String p : this.listOfpublicationUpdated){
                writer.write(p + "\n \n");
            }

            writer.close();
        }
    }

    /**
     *
     * @return  the proteinSelector instance of this object
     */
    public ProteinDatasetSelector getProteinSelector() {
        return proteinSelector;
    }

    /**
     * set the protein selector
     * @param proteinSelector
     */
    public void setProteinSelector(ProteinDatasetSelector proteinSelector) {
        this.proteinSelector = proteinSelector;

        if (this.proteinSelector != null){
            this.proteinSelector.setIntactContext(this.context);
        }
    }

    /**
     *
     * @return  the intact context
     */
    public IntactContext getContext() {
        return context;
    }

    /**
     * Set the intact context
     * @param context
     */
    public void setContext(IntactContext context) {
        this.context = context;

        if (this.proteinSelector != null){
            this.proteinSelector.setIntactContext(this.context);
        }
    }

    /**
     * Revert the dataset annotation
     * @throws ProteinSelectorException
     */
    public void revertDatasetAnnotations() throws ProteinSelectorException {
        // The ProteinDatasetSelector must be not null
        if (this.proteinSelector == null){
            throw new ProteinSelectorException("The proteinSelector has not been initialised, we can't determine the list of proteins to look for.");
        }

        // The protein selector must have a dataset value
        if (this.proteinSelector.getDatasetValueToAdd() == null){
            throw new ProteinSelectorException("The dataset value to add for the proteinSelector has not been initialised, we can't determine the dataset value to add on each experiment containing the proteins of this dataset.");
        }
        // The protein selector must have a maximum number of interactions per experiment
        if (this.proteinSelector.getMaxNumberOfInteractionsPerExperiment() == 0){
            throw new ProteinSelectorException("The maximum number of interactions per experiment acceptable is 0. We will not be able to add the dataset annotation to any experiments.");
        }

        log.info("Start transaction...");

        Set<String> proteinSelected = this.proteinSelector.getSelectionOfProteinAccessionsInIntact();
        // for each protein of interest
        for (String accession : proteinSelected){
            //TransactionStatus transactionStatus = this.context.getDataContext().beginTransaction();

            // remove the dataset annotation
            List<Experiment> experimentToAddDataset = getExperimentsContainingDatasetToRemoveFor(accession);
            log.info("remove dataset " + this.proteinSelector.getDatasetValueToAdd() + " for "+experimentToAddDataset.size()+" experiments containing interaction involving the protein " + accession);
            removeDatasetToExperiments(experimentToAddDataset);

            //this.context.getDataContext().commitTransaction(transactionStatus);
        }
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

