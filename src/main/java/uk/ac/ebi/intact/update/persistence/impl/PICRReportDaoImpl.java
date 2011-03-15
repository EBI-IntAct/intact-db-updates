package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.persistence.PICRReportDao;

import javax.persistence.EntityManager;
import javax.persistence.Query;
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
public class PICRReportDaoImpl extends ActionReportDaoImpl<PICRReport> implements PICRReportDao {

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
        final Query query = getEntityManager().createQuery( "select ar from PICRReport as ar join ar.updateResult as res where res.id = :id" );
        query.setParameter( "id", id);

        return query.getResultList();
    }

    /**
     * 
     * @param protAc
     * @return
     */
    public List<PICRReport> getActionReportsWithPICRCrossReferencesByProteinAc(String protAc) {
        final Query query = getEntityManager().createQuery( "select a from PICRReport as a join a.updateResult as u where u.intactAccession = :protAc" );
        query.setParameter( "protAc", protAc);

        return query.getResultList();
    }
}
