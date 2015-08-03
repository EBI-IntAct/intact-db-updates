package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotUpdateEventDao;

import javax.persistence.EntityManager;
import java.util.List;

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

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedShortLabel(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotNull("updatedShortLabel"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedFullName(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotNull("updatedFullName"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUniprotQuery(long processId, String query) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("uniprotQuery", query))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedXrefs(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotEmpty("updatedXrefs"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedAnnotations(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotEmpty("updatedAnnotations"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotUpdateEvent> getUniprotUpdateEventWithUpdatedAliases(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotEmpty("updatedAliases")).list();
    }
}
