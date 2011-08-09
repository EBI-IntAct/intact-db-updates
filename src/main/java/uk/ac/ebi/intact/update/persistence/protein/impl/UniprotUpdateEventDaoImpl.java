package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.update.persistence.protein.UniprotUpdateEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for uniprot update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UniprotUpdateEventDaoImpl extends ProteinEventDaoImpl<UniprotUpdateEvent> implements UniprotUpdateEventDao {

    public UniprotUpdateEventDaoImpl(){
        super(UniprotUpdateEvent.class, null);
    }
    public UniprotUpdateEventDaoImpl(EntityManager entityManager) {
        super( UniprotUpdateEvent.class, entityManager);
    }
}
