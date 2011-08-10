package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.DeletedProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.DeletedProteinEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for deleted protein events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DeletedProteinEventDaoImpl extends ProteinEventDaoImpl<DeletedProteinEvent> implements DeletedProteinEventDao {
    public DeletedProteinEventDaoImpl(){
        super(DeletedProteinEvent.class, null);
    }
    public DeletedProteinEventDaoImpl(EntityManager entityManager) {
        super( DeletedProteinEvent.class, entityManager);
    }
}
