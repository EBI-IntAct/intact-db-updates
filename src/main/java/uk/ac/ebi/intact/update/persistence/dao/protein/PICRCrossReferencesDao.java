package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentPICRCrossReferences;
import uk.ac.ebi.intact.update.persistence.dao.UpdateBaseDao;

import java.util.List;

/**
 * This interface contains some methods to query the database and get specific PersistentPICRCrossReferences
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface PICRCrossReferencesDao extends UpdateBaseDao<PersistentPICRCrossReferences> {

    /**
     *
     * @param databaseName
     * @return the list of PersistentPICRCrossReferences with a specific database name
     */
    public List<PersistentPICRCrossReferences> getAllCrossReferencesByDatabaseName(String databaseName);

    /**
     *
     * @param databaseName
     * @param actionId
     * @return the list of PersistentPICRCrossReferences with a specific database name and attached to a specific action
     */
    public List<PersistentPICRCrossReferences> getCrossReferencesByDatabaseNameAndActionId(String databaseName, long actionId);
}
