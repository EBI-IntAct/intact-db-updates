package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.UpdatedAlias;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.UpdatedAliasDao;

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
public class UpdatedAliasDaoImpl extends UpdateBaseDaoImpl<UpdatedAlias> implements UpdatedAliasDao {
    public UpdatedAliasDaoImpl() {
        super(UpdatedAlias.class);
    }

    public UpdatedAliasDaoImpl(EntityManager entityManager) {
        super(UpdatedAlias.class, entityManager);
    }

    @Override
    public Collection<UpdatedAlias> getDeletedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.deleted))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<UpdatedAlias> getUpdatedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.updated))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<UpdatedAlias> getAddedAliasesFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.added))
                .add(Restrictions.eq("p.id", eventId)).list();
    }
}
