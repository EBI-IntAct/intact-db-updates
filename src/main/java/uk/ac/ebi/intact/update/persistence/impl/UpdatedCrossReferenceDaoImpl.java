package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.UpdatedCrossReference;
import uk.ac.ebi.intact.update.persistence.UpdatedCrossReferenceDao;

import javax.persistence.EntityManager;
import java.util.Collection;

/**
 * Dao implementation for UpdatedCrossReference
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Repository
@Transactional(readOnly = true)
public class UpdatedCrossReferenceDaoImpl<T extends UpdatedCrossReference> extends UpdateBaseDaoImpl<T> implements UpdatedCrossReferenceDao<T> {
    public UpdatedCrossReferenceDaoImpl() {
        super((Class<T>) UpdatedCrossReference.class);
    }

    public UpdatedCrossReferenceDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }

    @Override
    public Collection<T> getDeletedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.deleted))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getUpdatedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.updated))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getAddedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.added))
                .add(Restrictions.eq("p.id", eventId)).list();
    }
}
