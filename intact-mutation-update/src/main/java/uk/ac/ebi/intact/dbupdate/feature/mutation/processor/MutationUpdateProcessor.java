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
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.utils.OntologyServiceHelper;

import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateProcessor {
    private static final Log log = LogFactory.getLog(MutationUpdateProcessor.class);

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

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    public void updateByACs(Set<IntactFeatureEvidence> intactFeatureEvidences) {
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

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager", readOnly = true)
    private Set<IntactFeatureEvidence> getAllMutationFeatures() {
        List<String> mutationTerms = OntologyServiceHelper.getOntologyServiceHelper().getAssociatedMITerms("MI:0118", 10);
        log.info("Retrieved all child terms of MI:0118 (mutation).");
        Set<IntactFeatureEvidence> featureEvidences = new HashSet<>();
        for (String term : mutationTerms) {
            //MI:0429(necessary binding region) should not be taken into account
            if (term.equals("MI:0429")) {
                continue;
            }
            featureEvidences.addAll(intactDao.getFeatureEvidenceDao().getByFeatureType(null, term));
        }
        log.info("Retrieved all features of type mutation. Excluded MI:0429(necessary binding region)");
        return featureEvidences;
    }
}
