package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.UpdatedCrossReferenceDao;

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
public class UpdatedCrossReferenceDaoImpl extends UpdateBaseDaoImpl<UpdatedCrossReference> implements UpdatedCrossReferenceDao {
    public UpdatedCrossReferenceDaoImpl() {
        super(UpdatedCrossReference.class);
    }

    public UpdatedCrossReferenceDaoImpl(EntityManager entityManager) {
        super(UpdatedCrossReference.class, entityManager);
    }

    @Override
    public Collection<UpdatedCrossReference> getDeletedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.deleted))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<UpdatedCrossReference> getUpdatedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.updated))
                .add(Restrictions.eq("p.id", eventId)).list();
    }

    @Override
    public Collection<UpdatedCrossReference> getAddedXrefsFor(Long eventId) {
        return getSession().createCriteria(getEntityClass())
                .createAlias("parent", "p")
                .add(Restrictions.eq("status", UpdateStatus.added))
                .add(Restrictions.eq("p.id", eventId)).list();
    }
}
