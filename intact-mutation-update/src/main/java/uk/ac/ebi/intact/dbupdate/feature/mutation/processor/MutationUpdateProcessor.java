package uk.ac.ebi.intact.dbupdate.feature.mutation.processor;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.feature.mutation.MutationUpdateContext;
import uk.ac.ebi.intact.dbupdate.feature.mutation.listener.LoggingListener;
import uk.ac.ebi.intact.dbupdate.feature.mutation.listener.ReportWriterListener;
import uk.ac.ebi.intact.dbupdate.feature.mutation.listener.UpdateListener;
import uk.ac.ebi.intact.dbupdate.feature.mutation.writer.FileReportHandler;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGenerator;

import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateProcessor {
    private static final Log log = LogFactory.getLog(MutationUpdateProcessor.class);
    private static final String MUTATION_MI_ID = "MI:0118";
    private static final String MUTATION_ENABLING_INTERACTION_MI_ID = "MI:2227";
    private static final String MUTATION_DECREASING_MI_ID = "MI:0119";
    private static final String MUTATION_DECREASING_RATE_MI_ID = "MI:1130";
    private static final String MUTATION_DECREASING_STRENGTH_MI_ID = "MI:1133";
    private static final String MUTATION_DISRUPTING_MI_ID = "MI:0573";
    private static final String MUTATION_DISRUPTING_RATE_MI_ID = "MI:1129";
    private static final String MUTATION_DISRUPTING_STRENGTH_MI_ID = "MI:1128";
    private static final String MUTATION_INCREASING_MI_ID = "MI:0382";
    private static final String MUTATION_INCREASING_RATE_MI_ID = "MI:1131";
    private static final String MUTATION_INCREASING_STRENGTH_MI_ID = "MI:1132";
    private static final String MUTATION_WITH_NO_EFFECT_MI_ID = "MI:2226";
    private ShortlabelGenerator shortlabelGenerator;
    private IntactDao intactDao;
    private FileReportHandler fileReportHandler;

    private void init() {
        MutationUpdateProcessorConfig config = MutationUpdateContext.getInstance().getConfig();
        shortlabelGenerator = config.getShortlabelGenerator();
        intactDao = config.getIntactDao();
        fileReportHandler = config.getFileReportHandler();
        initListener();
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public void updateAll() {
        init();
        Set<IntactFeatureEvidence> intactFeatureEvidences = getAllMutationFeatures();
        updateByACs(intactFeatureEvidences);
    }

//    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    private void updateByACs(Set<IntactFeatureEvidence> intactFeatureEvidences) {
        for (IntactFeatureEvidence intactFeatureEvidence : intactFeatureEvidences) {
            log.info("Generate shortlabel for: " + intactFeatureEvidence.getAc());
            shortlabelGenerator.generateNewShortLabel(intactFeatureEvidence);
        }
    }

    private void initListener() {
        log.info("Initialise event listeners...");
        shortlabelGenerator.addListener(new ReportWriterListener(fileReportHandler));
        shortlabelGenerator.addListener(new UpdateListener());
        shortlabelGenerator.addListener(new LoggingListener());
    }

//    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    private Set<IntactFeatureEvidence> getAllMutationFeatures() {
        log.info("Retrieved all child terms of MI:0118 (mutation).");

        //We store them to avoid calling to OLS for a known subset
        Set<IntactFeatureEvidence> featureEvidences = new HashSet<>();
        //MI:0429(necessary binding region) should not be taken into account
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_ENABLING_INTERACTION_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DECREASING_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DECREASING_RATE_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DECREASING_STRENGTH_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DISRUPTING_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DISRUPTING_RATE_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_DISRUPTING_STRENGTH_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_INCREASING_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_INCREASING_RATE_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_INCREASING_STRENGTH_MI_ID));
        featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, MUTATION_WITH_NO_EFFECT_MI_ID));

        log.info("Retrieved all features of type mutation. Excluded MI:0429(necessary binding region)");
        return featureEvidences;
    }
}
