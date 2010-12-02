package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.proteinmapping.results.UpdateResults;

import java.util.List;

/**
 * This interface contains methods to query the database and get specific UpdateResults
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Mockable
public interface UpdateResultsDao extends UpdateBaseDao<UpdateResults> {

    /**
     *
     * @param id
     * @return The updateResults with this unique identifier in the database
     */
    public UpdateResults getUpdateResultsWithId(long id);

    /**
     *
     * @param proteinAc
     * @return The updateResults for a specific protein
     */
    public UpdateResults getUpdateResultsForProteinAc(String proteinAc);

    /**
     *
     * @param name
     * @return The list of update results containing an action with a specific name
     */
    public List<UpdateResults> getResultsContainingAction(ActionName name);

    /**
     *
     * @param label
     * @return The list of update results containing an action with a specific status
     */
    public List<UpdateResults> getResultsContainingActionWithLabel(StatusLabel label);

    /**
     *
     * @return the list of UpdateResults containing swissprot remapping reports
     */
    public List<UpdateResults> getUpdateResultsWithSwissprotRemapping();

    /**
     *
     * @return the list of UpdateResults containing a final uniprot accession not null
     */
    public List<UpdateResults> getSuccessfulUpdateResults();

    /**
     *
     * @return the list of UpdateResults to be reviewed by a curator
     */
    public List<UpdateResults> getUpdateResultsToBeReviewedByACurator();

    /**
     *
     * @return the list of UpdateResults which failed because the protein had no sequence and no identity XRefs
     */
    public List<UpdateResults> getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs();

    /**
     *
     * @return  the list of UpdateResults with a final uniprot id which is null and all the actions have a status FAILED
     */
    public List<UpdateResults> getUnsuccessfulUpdateResults();

    /**
     *
     * @return the list of UpdateResults with a conflict between the results of the strategy with sequence and those of the strategy with identifier
     */
    public List<UpdateResults> getUpdateResultsWithConflictsBetweenActions();

    /**
     *
     * @return the list of UpdateResults with a confluct between the new Swissprot sequence and several feature ranges
     */
    public List<UpdateResults> getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges();
}
