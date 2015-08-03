package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.CreatedProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.CreatedProteinEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for created proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class CreatedProteinEventDaoImpl extends ProteinEventDaoImpl<CreatedProteinEvent> implements CreatedProteinEventDao {

    public CreatedProteinEventDaoImpl(){
        super(CreatedProteinEvent.class, null);
    }
    public CreatedProteinEventDaoImpl(EntityManager entityManager) {
        super( CreatedProteinEvent.class, entityManager);
    }
}
