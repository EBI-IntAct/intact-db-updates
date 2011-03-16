package uk.ac.ebi.intact.update.persistence.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.update.events.range.InvalidRange;
import uk.ac.ebi.intact.update.persistence.InvalidRangeDao;

import javax.persistence.Query;
import java.util.Date;
import java.util.List;

/**
 * Default implementation of InvalidRangeDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/03/11</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class InvalidRangeDaoImpl extends UpdatedRangeDaoImpl<InvalidRange> implements InvalidRangeDao {

    private final static String invalidRangeAc = "EBI-2907496";
    private final static String outOfDateRangeAc = "EBI-3058809";

    @Override
    public List<InvalidRange> getAllInvalidRanges() {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa where fa.topic =:topic" );
        query.setParameter( "topic", invalidRangeAc);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRanges() {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa where fa.topic =:topic" );
        query.setParameter( "topic", outOfDateRangeAc);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRangesWithoutSequenceVersion() {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa where fa.topic =:topic and ir.sequenceVersion = :version" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getAllOutOfDateRangesWithSequenceVersion() {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa where fa.topic =:topic and ir.sequenceVersion <> :version" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getInvalidRanges(long processId) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and p.id = :id" );
        query.setParameter( "topic", invalidRangeAc);
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRanges(long processId) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and p.id = :id" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "id", processId);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(long processId) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion = :version and p.id = :id" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "id", processId);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(long processId) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion <> :version and p.id = :id" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "id", processId);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getInvalidRanges(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) = trunc(:date)" );
        query.setParameter( "topic", invalidRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRanges(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) = trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersion(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion = :version and trunc(p.date) = trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersion(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion <> :version and trunc(p.date) = trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getInvalidRangesBefore(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) <= trunc(:date)" );
        query.setParameter( "topic", invalidRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesBefore(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) <= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionBefore(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion = :version and trunc(p.date) <= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionBefore(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion <> :version and trunc(p.date) <= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getInvalidRangesAfter(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) >= trunc(:date)" );
        query.setParameter( "topic", invalidRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateAfter(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and trunc(p.date) >= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "date", updateddate);

        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithoutSequenceVersionAfter(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion = :version and trunc(p.date) >= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }

    @Override
    public List<InvalidRange> getOutOfDateRangesWithSequenceVersionAfter(Date updateddate) {
        final Query query = getEntityManager().createQuery( "select ir from InvalidRange ir join ir.featureAnnotations as fa join ir.parent as p where fa.topic =:topic and ir.sequenceVersion <> :version and trunc(p.date) >= trunc(:date)" );
        query.setParameter( "topic", outOfDateRangeAc);
        query.setParameter( "version", -1);
        query.setParameter( "date", updateddate);


        return query.getResultList();
    }
}
