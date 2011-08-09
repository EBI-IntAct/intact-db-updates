package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.hibernate.criterion.Restrictions;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.persistence.impl.UpdateEventDaoImpl;
import uk.ac.ebi.intact.update.persistence.protein.ProteinUpdateErrorDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default implementation for protein update error dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/08/11</pre>
 */

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
                createAlias("parent", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("errorLabel", label)).list();
    }

    @Override
    public List<T> getUpdateErrorByReason(long processId, String reason) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("parent", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("errorMessage", reason)).list();
    }
}
