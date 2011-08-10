package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;
import uk.ac.ebi.intact.update.persistence.dao.protein.PICRReportDao;

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
public class PICRReportDaoImpl extends MappingReportDaoImpl<PersistentPICRReport> implements PICRReportDao {

    /**
     * create a new PICRReportDaoImpl
     */
    public PICRReportDaoImpl() {
        super(PersistentPICRReport.class, null);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public PICRReportDaoImpl(EntityManager entityManager) {
        super(PersistentPICRReport.class, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public List<PersistentPICRReport> getPICRReportsByResultsId(long id) {
        return getSession().createCriteria(PersistentPICRReport.class).createAlias("updateResult", "u")
                .add(Restrictions.eq("u.id", id)).list();
    }
}
