package uk.ac.ebi.intact.update.persistence.impl;

import org.hibernate.criterion.Restrictions;
import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.IdentificationResults;
import uk.ac.ebi.intact.update.persistence.IdentificationResultsDao;

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
public class IdentificationResultsDaoImpl extends UpdateBaseDaoImpl<IdentificationResults> implements IdentificationResultsDao {
    /**
     * Create a new IdentificationResultsDaoImpl
     */
    public IdentificationResultsDaoImpl() {
        super(IdentificationResults.class);
    }

    /**
     *
     * @param name
     * @return
     */
    public List<IdentificationResults> getResultsContainingAction(ActionName name) {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.name = :name" );
        query.setParameter( "name", name);

        return query.getResultList();
    }

    /**
     *
     * @param label
     * @return
     */
    public List<IdentificationResults> getResultsContainingActionWithLabel(StatusLabel label) {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.statusLabel = :status" );
        query.setParameter( "status", label);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<IdentificationResults> getUpdateResultsWithSwissprotRemapping() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.name = :name" );
        query.setParameter( "name", ActionName.BLAST_Swissprot_Remapping);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<IdentificationResults> getSuccessfulUpdateResults() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u where u.finalUniprotId <> null" );

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<IdentificationResults> getUpdateResultsToBeReviewedByACurator() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.statusLabel = :status or" +
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
    public List<IdentificationResults> getProteinNotUpdatedBecauseNoSequenceAndNoIdentityXrefs() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<IdentificationResults> getUnsuccessfulUpdateResults() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where u.finalUniprotId = null and " +
                "u not in ( select u2 from IdentificationResults as u2 join u2.listOfActions as a2 where a2.statusLabel <> :status and a2.name <> :name) " +
                "and a.name <> :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<IdentificationResults> getUpdateResultsWithConflictsBetweenActions() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.TO_BE_REVIEWED);
        query.setParameter( "name", ActionName.update_checking);

        return query.getResultList();
    }

    /**
     * 
     * @return
     */
    public List<IdentificationResults> getUpdateResultsWithConflictBetweenSwissprotSequenceAndFeatureRanges() {
        final Query query = getEntityManager().createQuery( "select u from IdentificationResults as u join u.listOfActions as a where a.statusLabel = :status and a.name = :name" );
        query.setParameter( "status", StatusLabel.FAILED);
        query.setParameter( "name", ActionName.feature_range_checking);

        return query.getResultList();
    }
}
