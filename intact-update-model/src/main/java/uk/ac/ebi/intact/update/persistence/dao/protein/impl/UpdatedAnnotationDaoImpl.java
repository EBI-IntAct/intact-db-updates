package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.UpdatedAnnotation;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.UpdatedAnnotationDao;

import javax.persistence.EntityManager;
import java.util.Collection;

/**
 * Dao implementation for UpdatedAnnotation
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Repository
@Transactional(readOnly = true)
public class UpdatedAnnotationDaoImpl<T extends UpdatedAnnotation> extends UpdateBaseDaoImpl<T> implements UpdatedAnnotationDao<T> {
    public UpdatedAnnotationDaoImpl() {
        super((Class<T>) UpdatedAnnotation.class);
    }

    public UpdatedAnnotationDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }

    @Override
    public Collection<T> getDeletedAnnotationFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.deleted))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getUpdatedAnnotationFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.updated))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getAddedAnnotationFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.added))
                .add(Restrictions.eq("p.id", eventId)).list();
    }
}
