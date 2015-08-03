package uk.ac.ebi.intact.dbupdate.dataset.selectors;

import uk.ac.ebi.intact.dbupdate.dataset.DatasetException;

import java.io.File;
import java.util.Set;

/**
 * The interface to implement for all the classes which are collecting intact object (proteins, components, publications, etc..) with specific conditions.
 * The classes should provide a set of intact accessions we want to retrieve in Intact to add a dataset
 * annotation for the experiments involving at least one of these objects.
 * Each DatasetSelector implementation should have a dataset value for the intact accessions it returned.
 * It is simpler to have the different values (dataset value, gene names, protein identifiers, organisms) in a
 * configuration file but it is not mandatory.
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public interface DatasetSelector {

    /**
     *
     * @return the dataset value associated with the intact accessions returned by the DatasetSelector
     */
    public String getDatasetValueToAdd();

    /**
     *
     * @return The maximum number of interactions in an experiment. If one experiment has more than this maximum number,
     * all the experiments attached to the same publication will be ignored and the dataset is not added to these experiments.
     * We can't add automatic dataset annotations for large proteomic experiments.
     */
    public int getMaxNumberOfInteractionsPerExperiment();


    /**
     * Clear all possible variables that the Selector keep in memory. Usually, a list of names, organisms loaded from a file
     */
    public void clearDatasetContent();

    /**
     * Read the file containing the list of object criterias (gene name, dataset value, organism, feature type, etc...) in the resources
     * @param datasetFile : name of the dataset file
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : exceptiom thrown if the file can't be read, found or is malformed
     */
    public void readDatasetFromResources(String datasetFile) throws DatasetException;

    /**
     * Read the file containing the list of object criterias (gene name, dataset value, organism, etc...)
     * @param datasetFile : name of the dataset file
     * @throws uk.ac.ebi.intact.dbupdate.dataset.DatasetException : exceptiom thrown if the file can't be read, found or is malformed
     */
    public void readDataset(String datasetFile) throws DatasetException;

    /**
     *
     * @return the set of publication Ids we want to remove from the list of results
     */
    public Set<String> getPublicationsIdToExclude();

    public File getReport();
}

