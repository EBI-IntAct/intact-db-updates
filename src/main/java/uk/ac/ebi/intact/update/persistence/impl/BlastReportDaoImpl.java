package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.persistence.BlastReportDao;

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
public class BlastReportDaoImpl extends MappingReportDaoImpl<BlastReport> implements BlastReportDao {

    /**
     * create a new BlastReportDaoImpl
     */
    public BlastReportDaoImpl() {
        super(BlastReport.class, null);
    }

    /**
     * Create BlastReportDaoImpl with entity manager
     * @param entityManager
     */
    public BlastReportDaoImpl(EntityManager entityManager) {
        super(BlastReport.class, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public List<BlastReport> getBlastReportsByResultsId(long id) {
        return getSession().createCriteria(BlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id)).list();
    }

    /**
     *
     * @param protAc
     * @return
     */
    public List<BlastReport> getActionReportsWithBlastResultsByProteinAc(String protAc) {
        return getSession().createCriteria(BlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.intactAccession", protAc)).list();
    }

    /**
     *
     * @param protAc
     * @return
     */
    public List<BlastReport> getActionReportsWithSwissprotRemappingResultsByProteinAc(String protAc) {
        return getSession().createCriteria(BlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.intactAccession", protAc))
                .add(Restrictions.eq("name", ActionName.BLAST_Swissprot_Remapping)).list();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<BlastReport> getActionReportsWithSwissprotRemappingResultsByResultsId(long id) {
        return getSession().createCriteria(BlastReport.class)
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id))
                .add(Restrictions.eq("name", ActionName.BLAST_Swissprot_Remapping)).list();
    }
}
