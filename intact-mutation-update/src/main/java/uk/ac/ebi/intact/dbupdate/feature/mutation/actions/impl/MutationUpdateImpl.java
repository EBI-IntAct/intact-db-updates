package uk.ac.ebi.intact.dbupdate.feature.mutation.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.impl.DefaultAnnotation;
import uk.ac.ebi.intact.dbupdate.feature.mutation.MutationUpdateContext;
import uk.ac.ebi.intact.dbupdate.feature.mutation.actions.MutationUpdate;
import uk.ac.ebi.intact.dbupdate.feature.mutation.processor.MutationUpdateProcessorConfig;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;
import uk.ac.ebi.intact.jami.utils.IntactUtils;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.ModifiedMutationShortlabelEvent;

import java.util.Date;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

public class MutationUpdateImpl implements MutationUpdate {
    private static final Log log = LogFactory.getLog(MutationUpdateImpl.class);

    private IntactDao intactDao;

    public MutationUpdateImpl() {
        MutationUpdateProcessorConfig config = MutationUpdateContext.getInstance().getConfig();
        intactDao = config.getIntactDao();
    }

    @Override
    public void updateMutation(ModifiedMutationShortlabelEvent event) {
        try {
            doUpdate(addInternalRemark(event));
        } catch (PersisterException | FinderException | SynchronizerException e) {
            e.printStackTrace();
        }
    }

    private IntactFeatureEvidence addInternalRemark(ModifiedMutationShortlabelEvent event) {
        IntactFeatureEvidence intactFeatureEvidence = event.getFeatureEvidence();
        String originalShortlabel = event.getOriginalShortlabel();
        CvTerm cvTerm = getRemarkInternal();
        String annotationMessage = new Date() + " This feature has been corrected as a result of our quality control procedures. The original label was '" + originalShortlabel + "'";
        DefaultAnnotation defaultAnnotation = new DefaultAnnotation(cvTerm, annotationMessage);
        intactFeatureEvidence.getAnnotations().add(defaultAnnotation);
        if (intactFeatureEvidence.getShortName() != null) {
            if (intactFeatureEvidence.getShortName().isEmpty()) {
                log.error("Short label in feature evidence (ac = " + intactFeatureEvidence.getAc() + ")  is too short. Short label = " + intactFeatureEvidence.getShortName());
            } else if (intactFeatureEvidence.getShortName().length() > IntactUtils.MAX_SHORT_LABEL_LEN) {
                log.error("Short label in feature evidence (ac = " + intactFeatureEvidence.getAc() + ")  is too long. Short label = " + intactFeatureEvidence.getShortName());
            }
        }
        return intactFeatureEvidence;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    private void doUpdate(IntactFeatureEvidence intactFeatureEvidence) throws PersisterException, FinderException, SynchronizerException {
        intactDao.getEntityManager().clear();
        try {
            intactDao.getFeatureEvidenceDao().update(intactFeatureEvidence);
            log.info("Updated: " + intactFeatureEvidence.getAc());
        } catch (SynchronizerException | FinderException | PersisterException e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            log.error(intactFeatureEvidence.getAc());
            throw e;
        } catch (Throwable e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            throw new PersisterException(e.getMessage(), e);
        }
        intactDao.getSynchronizerContext().clearCache();
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    private CvTerm getRemarkInternal() {
        return intactDao.getCvTermDao().getByAc("EBI-20");
    }
}
