package uk.ac.ebi.intact.update.persistence.proteinmapping;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.proteinmapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.util.List;

/**
 * This interface contains some methods to query the database and get specific PICRCrossReferences
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface PICRCrossReferencesDao extends UpdateBaseDao<PICRCrossReferences> {

    /**
     *
     * @param databaseName
     * @return the list of PICRCrossReferences with a specific database name
     */
    public List<PICRCrossReferences> getCrossReferencesByDatabaseName(String databaseName);

    /**
     *
     * @param databaseName
     * @param actionId
     * @return the list of PICRCrossReferences with a specific database name and attached to a specific action
     */
    public List<PICRCrossReferences> getCrossReferencesByDatabaseNameAndActionId(String databaseName, long actionId);

    /**
     *
     * @param protAc
     * @return the list of PICRCrossReferences for a protein
     */
    public List<PICRCrossReferences> getCrossReferencesByProteinAc(String protAc);

    /**
     *
     * @param id
     * @return the PICRCrossReferences with this unique identifier in the database
     */
    public PICRCrossReferences getCrossReferenceWithId(long id);
}
