package uk.ac.ebi.intact.update.persistence.impl;

import org.apache.commons.lang.time.DateUtils;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;
import uk.ac.ebi.intact.update.persistence.UpdateProcessDao;

import javax.persistence.EntityManager;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of UpdateProcessDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UpdateProcessDaoImpl extends UpdateBaseDaoImpl<UpdateProcess> implements UpdateProcessDao {
    public UpdateProcessDaoImpl() {
        super(UpdateProcess.class);
    }

    public UpdateProcessDaoImpl(EntityManager entityManager) {
        super(UpdateProcess.class, entityManager);
    }

    @Override
    public List<UpdateProcess> getAllUpdateProcessesByDate(Date date) {
        return getSession().createCriteria(UpdateProcess.class)
                .add(Restrictions.lt("date", DateUtils.addDays(date, 1)))
                .add(Restrictions.gt("date", DateUtils.addDays(date, -1))).list();
    }

    @Override
    public List<UpdateProcess> getAllUpdateProcessesBeforeDate(Date date) {
        return getSession().createCriteria(UpdateProcess.class)
                .add(Restrictions.le("date", date)).list();
    }

    @Override
    public List<UpdateProcess> getAllUpdateProcessesAfterDate(Date date) {
        return getSession().createCriteria(UpdateProcess.class)
                .add(Restrictions.ge("date", date)).list();
    }
}
