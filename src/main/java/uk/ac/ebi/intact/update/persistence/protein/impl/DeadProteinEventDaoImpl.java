package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.DeadProteinEvent;
import uk.ac.ebi.intact.update.persistence.protein.DeadProteinEventDao;

import javax.persistence.EntityManager;

/**
 * Default implementation of Dead protein event dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DeadProteinEventDaoImpl extends ProteinEventDaoImpl<DeadProteinEvent> implements DeadProteinEventDao {

    public DeadProteinEventDaoImpl(){
        super(DeadProteinEvent.class, null);
    }
    public DeadProteinEventDaoImpl(EntityManager entityManager) {
        super( DeadProteinEvent.class, entityManager);
    }
}
