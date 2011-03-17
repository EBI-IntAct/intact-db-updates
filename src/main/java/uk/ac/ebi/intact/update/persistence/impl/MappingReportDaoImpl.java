package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.persistence.MappingReportDao;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * The basic implementation of MappingReportDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Scope(org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE)
@Transactional(readOnly = true)
@Lazy
public class MappingReportDaoImpl<T extends MappingReport> extends UpdateBaseDaoImpl<T> implements MappingReportDao<T> {

    /**
     * Create an MappingReportDaoImpl
     */
    public MappingReportDaoImpl() {
        super((Class<T>) MappingReport.class);
    }

    /**
     * Create an MappingReportDaoImpl with entityClass en entity manager
     * @param entityClass
     * @param entityManager
     */
    public MappingReportDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super( entityClass, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public MappingReport getByReportId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar where ar.id = :id" );
        query.setParameter( "id", id);

        if (query.getResultList().isEmpty()){
            return null;
        }

        return (MappingReport) query.getResultList().iterator().next();
    }

    /**
     *
     * @param name
     * @return
     */
    public List<MappingReport> getByActionName(ActionName name) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar where ar.name = :name" );
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param status
     * @return
     */
    public List<MappingReport> getByReportStatus(StatusLabel status) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar where ar.statusLabel = :label" );
        query.setParameter( "label", status);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<MappingReport> getAllReportsWithWarnings() {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar join ar.warnings as warn" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<MappingReport> getAllReportsWithSeveralPossibleUniprot() {
         final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar where ar.listOfPossibleAccessions <> null" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PICRReport> getAllPICRReports() {
        final Query query = getEntityManager().createQuery( "select ar from PICRReport as ar" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<BlastReport> getAllBlastReports() {
        final Query query = getEntityManager().createQuery( "select ar from BlastReport as ar" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<BlastReport> getAllSwissprotRemappingReports() {
        final Query query = getEntityManager().createQuery( "select ar from BlastReport as ar where ar.name = :name" );
        query.setParameter("name", ActionName.BLAST_Swissprot_Remapping);

        return query.getResultList();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<MappingReport> getReportsWithWarningsByResultsId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar join ar.updateResult as res join ar.warnings as warn where res.id = :id" );
        query.setParameter( "id", id);

        return query.getResultList();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<MappingReport> getAllReportsByResultsId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar join ar.updateResult as res where res.id = :id" );
        query.setParameter( "id", id);

        return query.getResultList();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<MappingReport> getReportsWithSeveralPossibleUniprotByResultId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from MappingReport as ar join ar.updateResult as res where ar.listOfPossibleAccessions <> null and res.id = :id" );
        query.setParameter("id", id);

        return query.getResultList();
    }

    /**
     *
     * @param name
     * @param proteinAc
     * @return
     */
    public List<MappingReport> getActionReportsByNameAndProteinAc(ActionName name, String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from MappingReport as a join a.updateResult as u where u.intactAccession = :proteinAc and a.name = :name" );
        query.setParameter( "proteinAc", proteinAc);
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param name
     * @param resultId
     * @return
     */
    public List<MappingReport> getActionReportsByNameAndResultId(ActionName name, long resultId) {
        final Query query = getEntityManager().createQuery( "select a from MappingReport as a join a.updateResult as u where u.id = :id and a.name = :name" );
        query.setParameter( "id", resultId);
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param status
     * @param proteinAc
     * @return
     */
    public List<MappingReport> getActionReportsByStatusAndProteinAc(StatusLabel status, String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from MappingReport as a join a.updateResult as u where u.intactAccession = :proteinAc and a.statusLabel = :status" );
        query.setParameter( "proteinAc", proteinAc);
        query.setParameter( "status", status);

        return query.getResultList();
    }

    /**
     * 
     * @param label
     * @param resultId
     * @return
     */
    public List<MappingReport> getActionReportsByStatusAndResultId(StatusLabel label, long resultId) {
        final Query query = getEntityManager().createQuery( "select a from MappingReport as a join a.updateResult as u where u.id = :id and a.statusLabel = :label" );
        query.setParameter( "id", resultId);
        query.setParameter( "label", label );

        return query.getResultList();
    }

    /**
     * 
     * @param proteinAc
     * @return
     */
    public List<MappingReport> getActionReportsWithWarningsByProteinAc(String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from MappingReport as a join a.updateResult as u join a.warnings as warn where u.intactAccession = :protac" );
        query.setParameter( "protac", proteinAc);

        return query.getResultList();
    }
}
