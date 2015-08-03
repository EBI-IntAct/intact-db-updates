package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.SecondaryProteinEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.SecondaryProteinEventDao;

import javax.persistence.EntityManager;
import java.util.List;

/**
 * Default dao for secondary protein events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class SecondaryProteinEventDaoImpl extends ProteinEventDaoImpl<SecondaryProteinEvent> implements SecondaryProteinEventDao {

    public SecondaryProteinEventDaoImpl(){
        super(SecondaryProteinEvent.class, null);
    }
    public SecondaryProteinEventDaoImpl(EntityManager entityManager) {
        super( SecondaryProteinEvent.class, entityManager);
    }

    @Override
    public List<SecondaryProteinEvent> getSecondaryProteinEventsBySecondaryAc(long processId, String secondary) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.eq("secondaryUniprotAc", secondary))
                .addOrder(Order.asc("eventDate")).list();
    }
}
