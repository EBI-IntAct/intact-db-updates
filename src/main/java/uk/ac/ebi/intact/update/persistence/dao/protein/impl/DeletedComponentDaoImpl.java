package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.DeletedComponentEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.DeletedComponentEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for Deleted component events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DeletedComponentDaoImpl extends ProteinEventDaoImpl<DeletedComponentEvent> implements DeletedComponentEventDao {

    public DeletedComponentDaoImpl(){
        super(DeletedComponentEvent.class, null);
    }
    public DeletedComponentDaoImpl(EntityManager entityManager) {
        super( DeletedComponentEvent.class, entityManager);
    }
}
