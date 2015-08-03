package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.IntactTranscriptUpdateEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.IntactTranscriptUpdateEventDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default dao for intact transcript update events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class IntactTranscriptUpdateDaoImpl extends ProteinEventDaoImpl<IntactTranscriptUpdateEvent> implements IntactTranscriptUpdateEventDao {

    public IntactTranscriptUpdateDaoImpl(){
        super(IntactTranscriptUpdateEvent.class, null);
    }
    public IntactTranscriptUpdateDaoImpl(EntityManager entityManager) {
        super( IntactTranscriptUpdateEvent.class, entityManager);
    }

    @Override
    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithoutOldParent(long processId) {
        return getSession().createCriteria(IntactTranscriptUpdateEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNull("oldParentAc"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithOldParent(long processId, String oldParent) {
        return getSession().createCriteria(IntactTranscriptUpdateEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("oldParentAc", oldParent))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<IntactTranscriptUpdateEvent> getUpdatedTranscriptsWithNewParent(long processId, String newParent) {
        return getSession().createCriteria(IntactTranscriptUpdateEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("newParentAc", newParent))
                .addOrder(Order.asc("eventDate")).list();
    }
}
