package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.context.annotation.Scope;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.MappingReportDao;

import javax.persistence.EntityManager;
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
public class MappingReportDaoImpl<T extends PersistentMappingReport> extends UpdateBaseDaoImpl<T> implements MappingReportDao<T> {

    /**
     * Create an MappingReportDaoImpl
     */
    public MappingReportDaoImpl() {
        super((Class<T>) PersistentMappingReport.class);
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
     * @param name
     * @return
     */
    public List<T> getByActionName(ActionName name) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("name", name)).list();
    }

    /**
     *
     * @param status
     * @return
     */
    public List<T> getByReportStatus(StatusLabel status) {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.eq("statusLabel", status)).list();
    }

    /**
     *
     * @return
     */
    public List<T> getAllReportsWithWarnings() {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.isNotEmpty("warnings")).list();
    }

    /**
     *
     * @return
     */
    public List<T> getAllReportsWithSeveralPossibleUniprot() {
        return getSession().createCriteria(getEntityClass()).add(Restrictions.isNotEmpty("possibleAccessions")).list();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<T> getReportsWithWarningsByResultsId(long id) {
        return getSession().createCriteria(getEntityClass()).createAlias("updateResult", "u").add(Restrictions.eq("u.id", id))
                .add(Restrictions.isNotEmpty("warnings")).list();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<T> getAllReportsByResultsId(long id) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id)).list();
    }

    /**
     *
     * @param id
     * @return
     */
    public List<T> getReportsWithSeveralPossibleUniprotByResultId(long id) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", id))
                .add(Restrictions.isNotEmpty("possibleAccessions")).list();
    }

    /**
     *
     * @param name
     * @param resultId
     * @return
     */
    public List<T> getActionReportsByNameAndResultId(ActionName name, long resultId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", resultId))
                .add(Restrictions.eq("name", name)).list();
    }

    /**
     * 
     * @param label
     * @param resultId
     * @return
     */
    public List<T> getActionReportsByStatusAndResultId(StatusLabel label, long resultId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("updateResult", "u").add(Restrictions.eq("u.id", resultId))
                .add(Restrictions.eq("statusLabel", label)).list();
    }
}
