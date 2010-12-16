package uk.ac.ebi.intact.update.persistence.proteinmapping;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.proteinmapping.results.BlastResults;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.util.List;

/**
 * This interface contains some methods to query the database and get specific BlastResults
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Mockable
public interface BlastResultsDao extends UpdateBaseDao<BlastResults> {

    /**
     *
     * @param identity
     * @return  The list of BlastResults with an identity percent superior or equal to 'identity'
     */
    public List<BlastResults> getResultsByIdentitySuperior(float identity);

    /**
     *
     * @param identity
     * @param actionId
     * @return  The list of BlastResults attached to a specific action and with an identity superior or equal to 'identity'
     */
    public List<BlastResults> getResultsByActionIdAndIdentitySuperior(float identity, long actionId);

    /**
     * 
     * @return The list of BlastResults obtained from a Swissprot remapping process
     */
    public List<BlastResults> getAllSwissprotRemappingResults();

    /**
     *
     * @param actionId
     * @return The list of BlastResults obtained from a Swissprot remapping process for a specific result
     */
    public List<BlastResults> getAllSwissprotRemappingResultsFor(long actionId);

    /**
     *
     * @param tremblAc
     * @return The list of BlastResults obtained from a Swissprot remapping process and with a specific trembl Ac
     */
    public List<BlastResults> getSwissprotRemappingResultsByTremblAc(String tremblAc);

    /**
     *
     * @param proteinAc
     * @return The list of BlastResults for a specific protein
     */
    public List<BlastResults> getBlastResultsByProteinAc(String proteinAc);

    /**
     *
     * @param id
     * @return the blast results with this unique identifier in the database
     */
    public BlastResults getResultsById(long id);
}
