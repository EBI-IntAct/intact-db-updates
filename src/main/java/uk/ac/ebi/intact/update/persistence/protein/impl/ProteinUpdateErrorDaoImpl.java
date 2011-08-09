package uk.ac.ebi.intact.update.persistence.protein.impl;

import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.persistence.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.protein.ProteinUpdateErrorDao;

import javax.persistence.EntityManager;

/**
 * Default implementation for protein update error dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/08/11</pre>
 */

public class ProteinUpdateErrorDaoImpl<T extends DefaultPersistentUpdateError> extends UpdateBaseDaoImpl<T> implements ProteinUpdateErrorDao<T> {
    public ProteinUpdateErrorDaoImpl() {
        super((Class<T>)DefaultPersistentUpdateError.class);
    }

    public ProteinUpdateErrorDaoImpl(Class<T> entityClass, EntityManager entityManager) {
        super(entityClass, entityManager);
    }
}
