package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentBlastResults;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.BlastResultsDao;

import java.util.List;

/**
 * The basic implementation of BlastResultsDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class BlastResultsDaoImpl extends UpdateBaseDaoImpl<PersistentBlastResults> implements BlastResultsDao {

    /**
     * Create a new BlastResultsDaoImpl
     */
    public BlastResultsDaoImpl() {
        super(PersistentBlastResults.class);
    }

    /**
     *
     * @param identity
     * @return
     */
    public List<PersistentBlastResults> getByIdentitySuperiorTo(float identity) {
        return getSession().createCriteria(PersistentBlastResults.class).add(Restrictions.ge("identity", identity)).list();
    }

    /**
     *
     * @param identity
     * @param actionId
     * @return
     */
    public List<PersistentBlastResults> getByActionIdAndIdentitySuperiorTo(float identity, long actionId) {
        return getSession().createCriteria(PersistentBlastResults.class).createAlias("blastReport", "b")
                .add(Restrictions.ge("identity", identity)).add(Restrictions.eq("b.id", actionId)).list();

    }

    /**
     *
     * @return
     */
    public List<PersistentBlastResults> getAllSwissprotRemappingResults() {
        return getSession().createCriteria(PersistentBlastResults.class).add(Restrictions.isNotNull("tremblAccession")).list();
    }

    /**
     *
     * @param actionId
     * @return
     */
    public List<PersistentBlastResults> getAllSwissprotRemappingResultsFor(long actionId) {
        return getSession().createCriteria(PersistentBlastResults.class).createAlias("blastReport", "b")
                .add(Restrictions.isNotNull("tremblAccession")).add(Restrictions.eq("b.id", actionId)).list();
    }

    /**
     *
     * @param tremblAc
     * @return
     */
    public List<PersistentBlastResults> getSwissprotRemappingResultsByTremblAc(String tremblAc) {
        return getSession().createCriteria(PersistentBlastResults.class).add(Restrictions.eq("tremblAccession", tremblAc)).list();
    }
}
