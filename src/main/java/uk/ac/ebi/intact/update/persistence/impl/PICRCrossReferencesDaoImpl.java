package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.persistence.PICRCrossReferencesDao;

import java.util.List;

/**
 * The basic implementation of PICRCrossReferencesDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class PICRCrossReferencesDaoImpl extends UpdateBaseDaoImpl<PICRCrossReferences> implements PICRCrossReferencesDao {

/**
     * Create a new PICRCrossReferencesDAOImpl
     */
    public PICRCrossReferencesDaoImpl() {
        super(PICRCrossReferences.class);
    }

    /**
     *
     * @param databaseName
     * @return
     */
    public List<PICRCrossReferences> getAllCrossReferencesByDatabaseName(String databaseName) {
        return getSession().createCriteria(PICRCrossReferences.class).add(Restrictions.eq("database", databaseName)).list();
    }

    /**
     *
     * @param databaseName
     * @param actionId
     * @return
     */
    public List<PICRCrossReferences> getCrossReferencesByDatabaseNameAndActionId(String databaseName, long actionId) {
        return getSession().createCriteria(PICRCrossReferences.class).
                createAlias("picrReport", "p").add(Restrictions.eq("database", databaseName))
                .add(Restrictions.eq("p.id", actionId)).list();
    }

}
