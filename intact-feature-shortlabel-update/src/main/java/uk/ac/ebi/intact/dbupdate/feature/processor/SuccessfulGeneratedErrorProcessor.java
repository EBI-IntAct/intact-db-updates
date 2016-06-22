package uk.ac.ebi.intact.dbupdate.feature.processor;

import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class SuccessfulGeneratedErrorProcessor implements Proccesor {

    @Override
    public void process(String featureAc, String interactorAc, Object errorType, String message) {

    }

    public void process(IntactFeatureEvidence featureEvidence, String featureAc, String interactorAc, String originalShortlabel) {
        System.out.println(featureEvidence.getShortName());
        System.out.println("Success");
    }
}
