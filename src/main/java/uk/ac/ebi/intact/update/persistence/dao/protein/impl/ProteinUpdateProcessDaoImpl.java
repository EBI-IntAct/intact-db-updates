package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateProcessDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.ProteinUpdateProcessDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of ProteinUpdateProcessDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class ProteinUpdateProcessDaoImpl extends UpdateProcessDaoImpl<ProteinUpdateProcess> implements ProteinUpdateProcessDao {
    public ProteinUpdateProcessDaoImpl() {
        super(ProteinUpdateProcess.class);
    }

    /**
     * Create BlastReportDaoImpl with entity manager
     * @param entityManager
     */
    public ProteinUpdateProcessDaoImpl(EntityManager entityManager) {
        super(ProteinUpdateProcess.class, entityManager);
    }

    @Override
    public List<ProteinUpdateProcess> getByDate(Date date) {
        return getSession().createCriteria(ProteinUpdateProcess.class)
                .add(Restrictions.lt("date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("date", DateUtils.addDays(date, -1))).list();
    }

    @Override
    public List<ProteinUpdateProcess> getBeforeDate(Date date) {
        return getSession().createCriteria(ProteinUpdateProcess.class)
                .add(Restrictions.le("date", date)).list();
    }

    @Override
    public List<ProteinUpdateProcess> getAfterDate(Date date) {
        return getSession().createCriteria(ProteinUpdateProcess.class)
                .add(Restrictions.ge("date", date)).list();
    }

    @Override
    public List<ProteinUpdateProcess> getUpdateProcessHavingErrors() {
        return getSession().createCriteria(getEntityClass()).
                add(Restrictions.isNotEmpty("updateErrors")).list();
    }

    @Override
    public List<ProteinUpdateProcess> getUpdateProcessWithoutErrors() {
        return getSession().createCriteria(getEntityClass()).
                add(Restrictions.isEmpty("updateErrors")).list();
    }
}
