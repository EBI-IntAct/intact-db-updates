package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.ProteinUpdateErrorDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default implementation for protein update error dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class ProteinUpdateErrorDaoImpl<T extends DefaultPersistentUpdateError> extends UpdateEventDaoImpl<T> implements ProteinUpdateErrorDao<T> {
    public ProteinUpdateErrorDaoImpl() {
        super((Class<T>)DefaultPersistentUpdateError.class, null);
    }

    public ProteinUpdateErrorDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }

    @Override
    public List<T> getUpdateErrorByLabel(long processId, UpdateError label) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("errorLabel", label))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<T> getUpdateErrorByReason(long processId, String reason) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("errorMessage", reason))
                .addOrder(Order.asc("eventDate")).list();
    }
}
