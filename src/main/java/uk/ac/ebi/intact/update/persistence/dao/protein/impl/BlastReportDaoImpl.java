package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.persistence.dao.protein.BlastReportDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Basic implementation of BlastReportDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class BlastReportDaoImpl extends MappingReportDaoImpl<PersistentBlastReport> implements BlastReportDao {

    /**
     * create a new BlastReportDaoImpl
     */
    public BlastReportDaoImpl() {
        super(PersistentBlastReport.class, null);
    }

    /**
     * Create BlastReportDaoImpl with entity manager
     * @param entityManager
     */
    public BlastReportDaoImpl(EntityManager entityManager) {
        super(PersistentBlastReport.class, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public List<PersistentBlastReport> getByResultsId(long id) {
        return getSession().createCriteria(PersistentBlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id)).list();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<PersistentBlastReport> getReportsWithSwissprotRemappingResultsByResultsId(long id) {
        return getSession().createCriteria(PersistentBlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id))
                .add(Restrictions.eq("name", ActionName.BLAST_Swissprot_Remapping)).list();
    }
}
