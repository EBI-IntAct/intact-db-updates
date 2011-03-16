package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;

import java.util.List;

/**
 * This interface contains methods to query the database and get specific UpdateMappingResults
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Mockable
public interface UpdateResultsDao extends UpdateBaseDao<UpdateMappingResults> {

    /**
     *
     * @param id
     * @return The updateResults with this unique identifier in the database
     */
    public UpdateMappingResults getUpdateResultsWithId(long id);

    /**
     *
     * @param proteinAc
     * @return The updateResults for a specific protein
     */
    public UpdateMappingResults getUpdateResultsForProteinAc(String proteinAc);

    /**
     *
     * @param name
     * @return The list of update results containing an action with a specific name
     */
    public List<UpdateMappingResults> getResultsContainingAction(ActionName name);

    /**
     *
     * @param label
     * @return The list of update results containing an action with a specific status
     */
    public List<UpdateMappingResults> getResultsContainingActionWithLabel(StatusLabel label);

    /**
     *
     * @return the list of UpdateMappingResults containing swissprot remapping reports
     */
    public List<UpdateMappingResults> getUpdateResultsWithSwissprotRemapping();

    /**
     *
     * @return the list of UpdateMappingResults containing a final uniprot accession not null
     */
    public List<UpdateMappingResults> getSuccessfulUpdateResults();

    /**
     *
     * @return the list of UpdateMappingResults to be reviewed by a curator
     */
    public List<UpdateMappingResults> getUpdateResultsToBeReviewedByACurator();

    /**
     *
     * @return the list of UpdateMappingResults which failed because the protein had no sequence and no identity XRefs
     */
    public List<UpdateMappingResults> getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

    /**
     *
     * @return  the list of UpdateMappingResults with a final uniprot id which is null and all the actions have a status FAILED
     */
    public List<UpdateMappingResults> getUnsuccessfulUpdateResults();

    /**
     *
     * @return the list of UpdateMappingResults with a conflict between the results of the strategy with sequence and those of the strategy with identifier
     */
    public List<UpdateMappingResults> getUpdateResultsWithConflictsBetweenActions();

    /**
     *
     * @return the list of UpdateMappingResults with a confluct between the new Swissprot sequence and several feature ranges
     */
    public List<UpdateMappingResults> getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();
}
