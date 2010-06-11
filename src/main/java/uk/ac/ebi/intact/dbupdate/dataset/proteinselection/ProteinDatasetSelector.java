package uk.ac.ebi.intact.dbupdate.dataset.proteinselection;

import uk.ac.ebi.intact.core.context.IntactContext;

import java.util.Set;

/**
 * The interface to implement for all the classes which are collecting intact proteins with specific conditions.
 * The classes should provide a set of protein accessions we want to retrieve in Intact to add a dataset
 * annotation for the experiments involving at least of of these proteins.
 * Each ProteinDatasetSelector implementation should have a dataset value for the proteins it returned.
 * It is simpler to have the different values (dataset value, gene names, protein identifiers, organisms) in a
 * configuration file but it is not mandatory.
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public interface ProteinDatasetSelector {

    /**
     *
     * @return The protein IntAct accessions which are respecting the conditions imposed by the ProteinDatasetSelector
     * @throws ProteinSelectorException : exception if the intact context is not set or if there is no protein to retrieve
     * or if the dataset value is not set.
     */
    public Set<String> getSelectionOfProteinAccessionsInIntact() throws ProteinSelectorException ;

    /**
     *
     * @return the dataset value associated with the proteins returned by the ProteinDatasetSelector
     */
    public String getDatasetValueToAdd();

    /**
     * Set the dataset value associated with the proteins returned by the ProteinDatasetSelector.
     * It can be used if the selector has no configuration file where to find the dataset value associated with the proteins it is looking for.
     * @param dataset : dataset annotation
     */
    public void setDatasetValueToAdd(String dataset);

    /**
     *
     * @return The maximum number of interactions in an experiment. If one experiment has more than this maximum number,
     * all the experiments attached to the same publication will be ignored and the dataset is not added to these experiments.
     * We can't add automatic dataset annotations for large proteomic experiments.
     */
    public int getMaxNumberOfInteractionsPerExperiment();

    /**
     * Set the intact context
     * @param context : the intact context
     */
    public void setIntactContext(IntactContext context);

    /**
     * Clear all possible variables that the Selector keep in memory. Usually, a list of names, organisms loaded from a file
     */
    public void clearDatasetContent();

    /**
     * Read the file containing the list of protein criterias (gene name, dataset value, organism, etc...) in the resources
     * @param datasetFile : name of the dataset file
     * @throws ProteinSelectorException : exceptiom thrown if the file can't be read, found or is malformed
     */
    public void readDatasetFromResources(String datasetFile) throws ProteinSelectorException;

    /**
     * Read the file containing the list of protein criterias (gene name, dataset value, organism, etc...)
     * @param datasetFile : name of the dataset file
     * @throws ProteinSelectorException : exceptiom thrown if the file can't be read, found or is malformed
     */
    public void readDataset(String datasetFile) throws ProteinSelectorException;

    /**
     *
     * @return the set of publication Ids we want to remove from the list of results
     */
    public Set<String> getPublicationsIdToExclude();
}

