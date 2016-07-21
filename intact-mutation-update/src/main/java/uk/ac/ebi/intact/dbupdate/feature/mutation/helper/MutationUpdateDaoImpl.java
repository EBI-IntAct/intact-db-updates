package uk.ac.ebi.intact.dbupdate.feature.mutation.helper;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.model.CvTerm;
import uk.ac.ebi.intact.dbupdate.feature.mutation.MutationUpdateContext;
import uk.ac.ebi.intact.dbupdate.feature.mutation.processor.MutationUpdateConfig;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.Collection;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateDaoImpl implements MutationUpdateDao {
    private static final Log log = LogFactory.getLog(MutationUpdateDaoImpl.class);

    private MutationUpdateConfig config = MutationUpdateContext.getInstance().getConfig();

    @Override
    public Collection<IntactFeatureEvidence> getFeatureEvidenceByType(String term) {
        return config.getIntactDao().getFeatureEvidenceDao().getByFeatureType(null, term);
    }

    @Override
    public void doUpdate(IntactFeatureEvidence intactFeatureEvidence) {
        config.getIntactDao().getEntityManager().clear();
        try {
            config.getIntactDao().getFeatureEvidenceDao().update(intactFeatureEvidence);
            log.info("Updated: " + intactFeatureEvidence.getAc());
        } catch (SynchronizerException | FinderException | PersisterException e) {
            config.getIntactDao().getSynchronizerContext().clearCache();
            config.getIntactDao().getEntityManager().clear();
            log.error(intactFeatureEvidence.getAc());
        } catch (Throwable e) {
            config.getIntactDao().getSynchronizerContext().clearCache();
            config.getIntactDao().getEntityManager().clear();
        }
        config.getIntactDao().getSynchronizerContext().clearCache();
    }

    @Override
    public CvTerm getCVTermByAc(String ac) {
        return null;
    }
}
