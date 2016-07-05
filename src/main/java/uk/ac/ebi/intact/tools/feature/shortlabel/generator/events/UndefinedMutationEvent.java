package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class UndefinedMutationEvent {
    private String featureAc;
    private String interactorAc;

    public UndefinedMutationEvent(String featureAc, String interactorAc) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }
}
