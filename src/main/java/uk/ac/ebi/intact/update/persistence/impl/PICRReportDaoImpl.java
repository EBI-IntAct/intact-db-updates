package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.persistence.PICRReportDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * The basic implementation of PICRReportDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class PICRReportDaoImpl extends MappingReportDaoImpl<PICRReport> implements PICRReportDao {

    /**
     * create a new PICRReportDaoImpl
     */
    public PICRReportDaoImpl() {
        super(PICRReport.class, null);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public PICRReportDaoImpl(EntityManager entityManager) {
        super(PICRReport.class, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public List<PICRReport> getPICRReportsByResultsId(long id) {
        return getSession().createCriteria(PICRReport.class).createAlias("updateResult", "u")
                .add(Restrictions.eq("u.id", id)).list();
    }

    /**
     * 
     * @param protAc
     * @return
     */
    public List<PICRReport> getActionReportsWithPICRCrossReferencesByProteinAc(String protAc) {
        return getSession().createCriteria(PICRReport.class).createAlias("updateResult", "u")
                .add(Restrictions.eq("u.intactAccession", protAc)).list();
    }
}
