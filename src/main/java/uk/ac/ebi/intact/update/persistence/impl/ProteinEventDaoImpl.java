package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEvent;
import uk.ac.ebi.intact.update.persistence.ProteinEventDao;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of ProteinEventDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class ProteinEventDaoImpl<T extends ProteinEvent> extends UpdateBaseDaoImpl<T> implements ProteinEventDao<T> {
    public ProteinEventDaoImpl() {
        super((Class<T>) ProteinEvent.class);
    }

    public ProteinEventDaoImpl(Class<ProteinEvent> entityClass, EntityManager entityManager) {
        super((Class<T>) entityClass, entityManager);
    }

    @Override
    public List<T> getAllProteinEventsByName(EventName name) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe where pe.name = :name" );
        query.setParameter( "name", name.toString());

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAc(String proteinAc) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe where pe.proteinAc = :ac" );
        query.setParameter( "ac", proteinAc);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProcessId(long processId) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.id = :id" );
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByDate(Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.date = :date" );
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByNameAndProteinAc(EventName name, String proteinAc) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and pe.name = :name" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "name", name.toString());

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByNameAndProcessId(EventName name, long processId) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.id = :id and pe.name = :name" );
        query.setParameter( "id", processId);
        query.setParameter( "name", name.toString());

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAcAndProcessId(String proteinAc, long processId) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and p.id = :id" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, long processId) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and pe.name = :name and p.id = :id" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "name", name.toString());
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByNameAndDate(EventName name, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.date = :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAcAndDate(String proteinAc, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and p.date = :date" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAc(EventName name, String proteinAc, Date date) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.proteinAc = :ac and p.date = :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", date);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByNameBeforeDate(EventName name, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.date <= :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAcBeforeDate(String proteinAc, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and p.date <= :date" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcBefore(EventName name, String proteinAc, Date date) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.proteinAc = :ac and p.date <= :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", date);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByNameAfterDate(EventName name, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.date >= :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getAllProteinEventsByProteinAcAfterDate(String proteinAc, Date updatedDate) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where p.proteinAc = :ac and p.date >= :date" );
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", updatedDate);

        return query.getResultList();
    }

    @Override
    public List<T> getProteinEventsByNameAndProteinAcAfter(EventName name, String proteinAc, Date date) {
        final Query query = getEntityManager().createQuery( "select pe from "+ this.getEntityClass().getSimpleName() +" pe join pe.parent as p where pe.name = :name and p.proteinAc = :ac and p.date >= :date" );
        query.setParameter( "name", name.toString());
        query.setParameter( "ac", proteinAc);
        query.setParameter( "date", date);

        return query.getResultList();
    }
}
