package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.hibernate.criterion.Order;
import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.events.UniprotProteinMapperEvent;
import uk.ac.ebi.intact.update.persistence.dao.protein.UniprotProteinMapperEventDao;

import javax.persistence.EntityManager;
import java.util.List;

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

    @Override
    public List<UniprotProteinMapperEvent> getSuccessfulUniprotMappingEvents(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("identificationResults", "i").
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNotNull("i.finalUniprotId"))
                .addOrder(Order.asc("eventDate")).list();
    }

    @Override
    public List<UniprotProteinMapperEvent> getUnSuccessfulUniprotMappingEvents(long processId) {
        return getSession().createCriteria(getEntityClass()).
                createAlias("identificationResults", "i").
                createAlias("updateProcess", "p").add(Restrictions.eq("p.id", processId)).
                add(Restrictions.isNull("i.finalUniprotId"))
                .addOrder(Order.asc("eventDate")).list();
    }
}
