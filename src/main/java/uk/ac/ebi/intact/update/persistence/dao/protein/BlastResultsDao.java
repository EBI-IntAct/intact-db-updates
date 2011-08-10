package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentBlastResults;
import uk.ac.ebi.intact.update.persistence.dao.UpdateBaseDao;

import java.util.List;

/**
 * This interface contains some methods to query the database and get specific PersistentBlastResults
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Mockable
public interface BlastResultsDao extends UpdateBaseDao<PersistentBlastResults> {

    /**
     *
     * @param identity
     * @return  The list of PersistentBlastResults with an identity percent superior or equal to 'identity'
     */
    public List<PersistentBlastResults> getByIdentitySuperiorTo(float identity);

    /**
     *
     * @param identity
     * @param actionId
     * @return  The list of PersistentBlastResults attached to a specific action and with an identity superior or equal to 'identity'
     */
    public List<PersistentBlastResults> getByActionIdAndIdentitySuperiorTo(float identity, long actionId);

    /**
     * 
     * @return The list of PersistentBlastResults obtained from a Swissprot remapping process
     */
    public List<PersistentBlastResults> getAllSwissprotRemappingResults();

    /**
     *
     * @param actionId
     * @return The list of PersistentBlastResults obtained from a Swissprot remapping process for a specific result
     */
    public List<PersistentBlastResults> getAllSwissprotRemappingResultsFor(long actionId);

    /**
     *
     * @param tremblAc
     * @return The list of PersistentBlastResults obtained from a Swissprot remapping process and with a specific trembl Ac
     */
    public List<PersistentBlastResults> getSwissprotRemappingResultsByTremblAc(String tremblAc);
}
