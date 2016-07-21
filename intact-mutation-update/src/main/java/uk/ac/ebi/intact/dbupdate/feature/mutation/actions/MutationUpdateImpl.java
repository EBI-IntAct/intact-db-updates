package uk.ac.ebi.intact.dbupdate.feature.mutation.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.CvTerm;
import psidev.psi.mi.jami.model.impl.DefaultAnnotation;
import uk.ac.ebi.intact.dbupdate.feature.mutation.MutationUpdateContext;
import uk.ac.ebi.intact.dbupdate.feature.mutation.MutationUpdateConfig;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.ModifiedMutationShortlabelEvent;

import java.util.Date;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */

public class MutationUpdateImpl implements MutationUpdate {
    private static final Log log = LogFactory.getLog(MutationUpdateImpl.class);

    private MutationUpdateConfig config = MutationUpdateContext.getInstance().getConfig();

    @Override
    public void updateMutation(ModifiedMutationShortlabelEvent event) {
        config.getMutationUpdateDao().doUpdate(addInternalRemark(event));
    }

    private IntactFeatureEvidence addInternalRemark(ModifiedMutationShortlabelEvent event) {
        IntactFeatureEvidence intactFeatureEvidence = event.getFeatureEvidence();
        String originalShortlabel = event.getOriginalShortlabel();
        CvTerm cvTerm = getRemarkInternal();
        String annotationMessage = new Date() + " This feature has been corrected as a result of our quality control procedures. The original label was '" + originalShortlabel + "'";
        DefaultAnnotation defaultAnnotation = new DefaultAnnotation(cvTerm, annotationMessage);
        intactFeatureEvidence.getAnnotations().add(defaultAnnotation);
        return intactFeatureEvidence;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    private CvTerm getRemarkInternal() {
        return config.getMutationUpdateDao().getCVTermByAc("EBI-20");
    }
}
