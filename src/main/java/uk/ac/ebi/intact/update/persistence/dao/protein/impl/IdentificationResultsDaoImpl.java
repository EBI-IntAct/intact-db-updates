package uk.ac.ebi.intact.update.persistence.dao.protein.impl;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.persistence.dao.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.dao.protein.IdentificationResultsDao;

import javax.persistence.Query;
import java.util.List;

/**
 * The basic implementation of IdentificationResultsDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class IdentificationResultsDaoImpl extends UpdateBaseDaoImpl<PersistentIdentificationResults> implements IdentificationResultsDao {
    /**
     * Create a new IdentificationResultsDaoImpl
     */
    public IdentificationResultsDaoImpl() {
        super(PersistentIdentificationResults.class);
    }

    /**
     *
     * @param name
     * @return
     */
    public List<PersistentIdentificationResults> getResultsContainingAction(ActionName name) {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.name = :name" );
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param label
     * @return
     */
    public List<PersistentIdentificationResults> getResultsContainingActionWithLabel(StatusLabel label) {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.statusLabel = :status" );
        query.setParameter( "status", label);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getUpdateResultsWithSwissprotRemapping() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.name = :name" );
        query.setParameter( "name", ActionName.BLAST_Swissprot_Remapping);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getSuccessfulUpdateResults() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u where u.finalUniprotId <> null" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getUpdateResultsToBeReviewedByACurator() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.statusLabel = :status or" +
                " (a.statusLabel = :status2 and u.finalUniprotId = null and a.name <> :name)" );
        query.setParameter( "status", StatusLabel.TO_BE_REVIEWED);
        query.setParameter( "status2", StatusLabel.COMPLETED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getUnsuccessfulUpdateResults() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where u.finalUniprotId = null and " +
                "u not in ( select u2 from PersistentIdentificationResults as u2 join u2.listOfActions as a2 where a2.statusLabel <> :status and a2.name <> :name) " +
                "and a.name <> :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<PersistentIdentificationResults> getUpdateResultsWithConflictsBetweenActions() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.TO_BE_REVIEWED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     * 
     * @return
     */
    public List<PersistentIdentificationResults> getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges() {
        final Query query = getEntityManager().createQuery( "select u from PersistentIdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.feature_range_checking);

        return query.getResultList();
    }
}
