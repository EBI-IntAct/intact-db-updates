package uk.ac.ebi.intact.update.persistence.impl.proteinmapping;

import org.springframework.context.annotation.Lazy;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.model.proteinmapping.results.BlastResults;
import uk.ac.ebi.intact.update.persistence.impl.UpdateBaseDaoImpl;
import uk.ac.ebi.intact.update.persistence.proteinmapping.BlastResultsDao;

import javax.persistence.Query;
import java.util.List;

/**
 * The basic implementation of BlastResultsDao
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Repository
@Transactional(readOnly = true)
@Lazy
public class BlastResultsDaoImpl extends UpdateBaseDaoImpl<BlastResults> implements BlastResultsDao {

    /**
     * Create a new BlastResultsDaoImpl
     */
    public BlastResultsDaoImpl() {
        super(BlastResults.class);
    }

    /**
     *
     * @param identity
     * @return
     */
    public List<BlastResults> getResultsByIdentitySuperior(float identity) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults as br where br.identity >= :identity" );
        query.setParameter( "identity", identity);

        return query.getResultList();
    }

    /**
     *
     * @param identity
     * @param actionId
     * @return
     */
    public List<BlastResults> getResultsByActionIdAndIdentitySuperior(float identity, long actionId) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br join br.blastReport as res where br.identity >= :identity and res.id = :id" );
        query.setParameter( "identity", identity);
        query.setParameter( "id", actionId);

        return query.getResultList();
    }

    /**
     *
     * @return
     */
    public List<BlastResults> getAllSwissprotRemappingResults() {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br where br.tremblAccession <> null" );

        return query.getResultList();
    }

    /**
     *
     * @param actionId
     * @return
     */
    public List<BlastResults> getAllSwissprotRemappingResultsFor(long actionId) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br join br.blastReport as res where br.tremblAccession <> null and res.id = :id" );
        query.setParameter( "id", actionId);

        return query.getResultList();
    }

    /**
     *
     * @param tremblAc
     * @return
     */
    public List<BlastResults> getSwissprotRemappingResultsByTremblAc(String tremblAc) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br where br.tremblAccession = :tremblAc" );
        query.setParameter( "tremblAc", tremblAc);

        return query.getResultList();
    }

    /**
     *
     * @param proteinAc
     * @return
     */
    public List<BlastResults> getBlastResultsByProteinAc(String proteinAc) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br join br.blastReport as blast join blast.updateResult as ur where ur.intactAccession = :ac" );
        query.setParameter( "ac", proteinAc);

        return query.getResultList();
    }

    /**
     * 
     * @param id
     * @return
     */
    public BlastResults getResultsById(long id) {
        final Query query = getEntityManager().createQuery( "select br from BlastResults br where br.id = :id" );
        query.setParameter( "id", id);

        if (query.getResultList().isEmpty()){
             return null;
        }

        return (BlastResults) query.getResultList().iterator().next();
    }
}
