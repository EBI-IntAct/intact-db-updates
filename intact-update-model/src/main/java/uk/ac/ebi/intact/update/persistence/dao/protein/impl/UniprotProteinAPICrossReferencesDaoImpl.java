package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentUniprotProteinAPICrossReferences;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinAPICrossReferencesDao;

import java.util.List;

/**
 * The basic implementation of UniprotProteinAPICrossReferencesDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UniprotProteinAPICrossReferencesDaoImpl extends UpdateBaseDaoImpl<PersistentUniprotProteinAPICrossReferences> implements UniprotProteinAPICrossReferencesDao {

/**
     * Create a new UniprotProteinAPICrossReferencesDAOImpl
     */
    public UniprotProteinAPICrossReferencesDaoImpl() {
        super(PersistentUniprotProteinAPICrossReferences.class);
    }

    /**
     *
     * @param databaseName
     * @return
     */
    public List<PersistentUniprotProteinAPICrossReferences> getAllCrossReferencesByDatabaseName(String databaseName) {
        return getSession().createCriteria(PersistentUniprotProteinAPICrossReferences.class).add(Restrictions.eq("database", databaseName)).list();
    }

    /**
     *
     * @param databaseName
     * @param actionId
     * @return
     */
    public List<PersistentUniprotProteinAPICrossReferences> getCrossReferencesByDatabaseNameAndActionId(String databaseName, long actionId) {
        return getSession().createCriteria(PersistentUniprotProteinAPICrossReferences.class).
                createAlias("uniprotProteinAPIReport", "p").add(Restrictions.eq("database", databaseName))
                .add(Restrictions.eq("p.id", actionId)).list();
    }

}
