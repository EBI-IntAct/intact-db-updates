package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.UniprotProteinMapperEvent;
import uk.ac.ebi.intact.update.persistence.protein.UniprotProteinMapperEventDao;

import javax.persistence.EntityManager;

/**
 * Default dao for uniprot mapping events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UniprotProteinMapperEventDaoImpl extends ProteinEventDaoImpl<UniprotProteinMapperEvent> implements UniprotProteinMapperEventDao {

    public UniprotProteinMapperEventDaoImpl(){
        super(UniprotProteinMapperEvent.class, null);
    }
    public UniprotProteinMapperEventDaoImpl(EntityManager entityManager) {
        super( UniprotProteinMapperEvent.class, entityManager);
    }
}
