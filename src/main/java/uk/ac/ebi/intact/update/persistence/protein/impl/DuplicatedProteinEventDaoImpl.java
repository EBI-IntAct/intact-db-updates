package uk.ac.ebi.intact.update.persistence.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.DuplicatedProteinEvent;
import uk.ac.ebi.intact.update.persistence.protein.DuplicatedProteinEventDao;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DuplicatedProteinEventDaoImpl extends ProteinEventDaoImpl<DuplicatedProteinEvent> implements DuplicatedProteinEventDao {
}
