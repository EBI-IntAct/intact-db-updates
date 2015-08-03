package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.DuplicatedProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.DuplicatedProteinEventDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default implementation for duplicated protein events dao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class DuplicatedProteinEventDaoImpl extends ProteinEventDaoImpl<DuplicatedProteinEvent> implements DuplicatedProteinEventDao {

    public DuplicatedProteinEventDaoImpl(){
        super(DuplicatedProteinEvent.class, null);
    }
    public DuplicatedProteinEventDaoImpl(EntityManager entityManager) {
        super( DuplicatedProteinEvent.class, entityManager);
    }

    @Override
    public List<DuplicatedProteinEvent> getDuplicatedEventByOriginalProtein(long processId, String originalProtein) {
        return getSession().createCriteria(DuplicatedProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("originalProtein", originalProtein))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<DuplicatedProteinEvent> getDuplicatedEventWithMergeSuccessful(long processId) {
        return getSession().createCriteria(DuplicatedProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("mergeSuccessful", true))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<DuplicatedProteinEvent> getDuplicatedEventWithMergeNotSuccessful(long processId) {
        return getSession().createCriteria(DuplicatedProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("mergeSuccessful", false))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<DuplicatedProteinEvent> getDuplicatedEventWithSequenceUpdate(long processId) {
        return getSession().createCriteria(DuplicatedProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("sequenceUpdate", true))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<DuplicatedProteinEvent> getDuplicatedEventWithoutSequenceUpdate(long processId) {
        return getSession().createCriteria(DuplicatedProteinEvent.class).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("sequenceUpdate", false))
                .addOrder(Order.asc("eventDate")).list();
    }
}
