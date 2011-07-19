package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.UpdatedAlias;
import uk.ac.ebi.intact.update.persistence.UpdatedAliasDao;

import javax.persistence.EntityManager;
import java.util.Collection;

/**
 * Dao implementation for UpdatedAlias
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Repository
@Transactional(readOnly = true)
public class UpdatedAliasDaoImpl<T extends UpdatedAlias> extends UpdateBaseDaoImpl<T> implements UpdatedAliasDao<T> {
    public UpdatedAliasDaoImpl() {
        super((Class<T>) UpdatedAlias.class);
    }

    public UpdatedAliasDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }

    @Override
    public Collection<T> getDeletedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.deleted))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getUpdatedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.updated))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<T> getAddedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.added))
                .add(Restrictions.eq("p.id", eventId)).list();
    }
}
