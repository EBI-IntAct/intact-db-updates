package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;

import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class SuccessfulGeneratedEvent {
    private String featureAc;
    private String interactorAc;
    private IntactFeatureEvidence featureEvidence;
    private String originalShortlabel;

    public SuccessfulGeneratedEvent(String featureAc, String interactorAc, IntactFeatureEvidence featureEvidence, String originalShortlabel) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.featureEvidence = featureEvidence;
        this.originalShortlabel = originalShortlabel;
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public IntactFeatureEvidence getFeatureEvidence() {
        return featureEvidence;
    }

    public String getOriginalShortlabel() {
        return originalShortlabel;
    }
}
