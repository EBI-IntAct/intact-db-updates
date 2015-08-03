package uk.ac.ebi.intact.update.persistence.dao.impl;

import org.hibernate.Session;
import org.hibernate.criterion.Projections;
import org.hibernate.criterion.Restrictions;
import org.hibernate.ejb.HibernateEntityManager;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.persistence.dao.UpdateBaseDao;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.List;

/**
 * Basic implementation of UpdateBaseDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public abstract class UpdateBaseDaoImpl<T extends HibernateUpdatePersistentImpl> implements UpdateBaseDao<T> {

    /**
     * The entity maneger
     */
    @PersistenceContext( unitName = "intact-update" )
    private EntityManager entityManager;

    /**
     * The entity class
     */
    private Class<T> entityClass;

    /**
     * Create a new UpdateBaseDaoImpl
     * @param entityClass
     */
    public UpdateBaseDaoImpl( Class<T> entityClass ) {
        this.entityClass = entityClass;
    }

    /**
     * Create a new UpdateBaseDaoImpl with entity class and entity manager
     * @param entityClass
     * @param entityManager
     */
    public UpdateBaseDaoImpl( Class<T> entityClass, EntityManager entityManager ) {
        this.entityClass = entityClass;
        this.entityManager = entityManager;
    }

    /**
     *
     * @return
     */
    public int countAll() {
        final Long count = (Long) getSession().createCriteria(entityClass)
                .setProjection(Projections.rowCount())
                .uniqueResult();
        return count.intValue();
    }

    /**
     *
     * @return
     */
    public List<T> getAll() {
        return getSession().createCriteria(entityClass).list();
    }

    /**
     *
     * @return
     */
    public T getById(long id) {
        T res = (T) getSession().createCriteria(entityClass).add(Restrictions.eq("id", id)).uniqueResult();

        return res;
    }

    /**
     *
     * @param entity
     */
    public void persist( T entity ) {
        getEntityManager().persist( entity );
    }

    /**
     *
     * @param entity
     */
    public void delete( T entity ) {
        getEntityManager().remove( entity );
    }

    /**
     *
     * @param entity
     */
    public void update( T entity ) {
        getSession().update( entity );
    }

    /**
     *
     * @param entity
     */
    public void saveOrUpdate( T entity ) {
        getSession().saveOrUpdate( entity );
    }

    /**
     *
     */
    public void flush() {
        entityManager.flush();
    }

    /**
     *
     * @return
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     *
     * @return
     */
    public Session getSession() {
        return ( (HibernateEntityManager) entityManager ).getSession();
    }

    /**
     *
     * @param entityClass
     */
    public void setEntityClass(Class<T> entityClass) {
        this.entityClass = entityClass;
    }

    /**
     *
     * @return
     */
    public Class<T> getEntityClass() {
        return entityClass;
    }
}
