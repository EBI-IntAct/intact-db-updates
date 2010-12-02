package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.persistence.ActionReportDao;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.List;

/**
 * The basic implementation of ActionReportDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Scope(org.springframework.beans.factory.config.BeanDefinition.SCOPE_PROTOTYPE)
@Transactional(readOnly = true)
public class ActionReportDaoImpl<T extends ActionReport> extends UpdateBaseDaoImpl<T> implements ActionReportDao<T> {

    /**
     * Create an ActionReportDaoImpl
     */
    public ActionReportDaoImpl() {
        super((Class<T>) ActionReport.class);
    }

    /**
     * Create an ActionReportDaoImpl with entityClass en entity manager
     * @param entityClass
     * @param entityManager
     */
    public ActionReportDaoImpl( Class<T> entityClass, EntityManager entityManager ) {
        super( entityClass, entityManager);
    }

    /**
     *
     * @param id
     * @return
     */
    public ActionReport getByReportId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar where ar.id = :id" );
        query.setParameter( "id", id);

        if (query.getResultList().isEmpty()){
            return null;
        }

        return (ActionReport) query.getResultList().iterator().next();
    }

    /**
     *
     * @param name
     * @return
     */
    public List<ActionReport> getByActionName(ActionName name) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar where ar.name = :name" );
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param status
     * @return
     */
    public List<ActionReport> getByReportStatus(StatusLabel status) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar where ar.statusLabel = :label" );
        query.setParameter( "label", status);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<ActionReport> getAllReportsWithWarnings() {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar join ar.warnings as warn" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<ActionReport> getAllReportsWithSeveralPossibleUniprot() {
         final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar where ar.listOfPossibleAccessions <> null" );

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
    public List<ActionReport> getReportsWithWarningsByResultsId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar join ar.updateResult as res join ar.warnings as warn where res.id = :id" );
        query.setParameter( "id", id);

        return query.getResultList();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<ActionReport> getAllReportsByResultsId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar join ar.updateResult as res where res.id = :id" );
        query.setParameter( "id", id);

        return query.getResultList();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<ActionReport> getReportsWithSeveralPossibleUniprotByResultId(long id) {
        final Query query = getEntityManager().createQuery( "select ar from ActionReport as ar join ar.updateResult as res where ar.listOfPossibleAccessions <> null and res.id = :id" );
        query.setParameter("id", id);

        return query.getResultList();
    }

    /**
     *
     * @param name
     * @param proteinAc
     * @return
     */
    public List<ActionReport> getActionReportsByNameAndProteinAc(ActionName name, String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from ActionReport as a join a.updateResult as u where u.intactAccession = :proteinAc and a.name = :name" );
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
    public List<ActionReport> getActionReportsByNameAndResultId(ActionName name, long resultId) {
        final Query query = getEntityManager().createQuery( "select a from ActionReport as a join a.updateResult as u where u.id = :id and a.name = :name" );
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
    public List<ActionReport> getActionReportsByStatusAndProteinAc(StatusLabel status, String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from ActionReport as a join a.updateResult as u where u.intactAccession = :proteinAc and a.statusLabel = :status" );
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
    public List<ActionReport> getActionReportsByStatusAndResultId(StatusLabel label, long resultId) {
        final Query query = getEntityManager().createQuery( "select a from ActionReport as a join a.updateResult as u where u.id = :id and a.statusLabel = :label" );
        query.setParameter( "id", resultId);
        query.setParameter( "label", label );

        return query.getResultList();
    }

    /**
     * 
     * @param proteinAc
     * @return
     */
    public List<ActionReport> getActionReportsWithWarningsByProteinAc(String proteinAc) {
        final Query query = getEntityManager().createQuery( "select a from ActionReport as a join a.updateResult as u join a.warnings as warn where u.intactAccession = :protac" );
        query.setParameter( "protac", proteinAc);

        return query.getResultList();
    }
}
