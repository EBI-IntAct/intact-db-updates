package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.range.UpdatedRange;
import uk.ac.ebi.intact.update.persistence.UpdatedRangeDao;

import javax.persistence.EntityManager;
import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of UpdatedRangeDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class UpdatedRangeDaoImpl<T extends UpdatedRange> extends UpdateBaseDaoImpl<T> implements UpdatedRangeDao<T> {

    public UpdatedRangeDaoImpl() {
        super((Class<T>) UpdatedRange.class, null);
    }

    /**
     * create a new PICRReportDaoImpl with entity manager
     * @param entityManager
     */
    public UpdatedRangeDaoImpl(EntityManager entityManager) {
        super((Class<T>) UpdatedRange.class, entityManager);
    }

    @Override
    public List<T> getUpdatedRangesByRangeAc(String rangeAc) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur where ur.rangeAc = :ac" );
        query.setParameter( "ac", rangeAc);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAc(String componentAc) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur where ur.componentAc = :ac" );
        query.setParameter( "ac", componentAc);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByUpdateProcessId(long processId) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.id = :id" );
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByUpdateDate(Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.date = :date" );
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations() {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.featureAnnotations as fa" );

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations(long processId) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.featureAnnotations as fa join ur.parent as p where p.id = :id" );
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotations(Date date) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.featureAnnotations as fa join ur.parent as p where p.date = :date" );
        query.setParameter( "date", date);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndDate(String rangeAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.rangeAc = :ac and p.date = :date" );
        query.setParameter( "ac", rangeAc);
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndDate(String componentAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.componentAc = :ac and p.date = :date" );
        query.setParameter( "ac", componentAc);
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesBeforeUpdateDate(Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.date <= :date" );
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotationsBefore(Date date) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.featureAnnotations as fa join ur.parent as p where p.date <= :date" );
        query.setParameter( "date", date);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndBeforeDate(String rangeAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.date <= :date" );
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndBeforeDate(String componentAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.componentAc = :ac and p.date <= :date" );
        query.setParameter( "ac", componentAc);
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesAfterUpdateDate(Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.date >= :date" );
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesHavingUpdatedFeatureAnnotationsAfter(Date date) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.featureAnnotations as fa join ur.parent as p where p.date >= :date" );
        query.setParameter( "date", date);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndAfterDate(String rangeAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where p.date >= :date" );
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndAfterDate(String componentAc, Date updateDate) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.componentAc = :ac and p.date >= :date" );
        query.setParameter( "ac", componentAc);
        query.setParameter( "date", updateDate);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByRangeAcAndProcessId(String rangeAc, long processId) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.rangeAc = :ac and p.id = :id" );
        query.setParameter( "ac", rangeAc);
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<T> getUpdatedRangesByComponentAcAndProcessId(String componentAc, long processId) {
        final Query query = getEntityManager().createQuery( "select ur from "+getEntityClass().getSimpleName()+" ur join ur.parent as p where ur.componentAc = :ac and p.id = :id" );
        query.setParameter( "ac", componentAc);
        query.setParameter( "id", processId);

        return query.getResultList();
    }
}
