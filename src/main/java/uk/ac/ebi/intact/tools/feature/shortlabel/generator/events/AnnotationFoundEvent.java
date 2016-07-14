package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;


/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class AnnotationFoundEvent {
    private String featureAc;
    private String interactorAc;
    private AnnotationType type;
    private String message;

    public AnnotationFoundEvent(String featureAc, String interactorAc, AnnotationType type) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.type = type;
        this.message = type.getMessage();
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public AnnotationType getType() {
        return type;
    }

    public String getMessage() {
        return message;
    }

    //TODO We mix here two different things. FEATURE_WRONG && FEATURE_CORRECTED are annotation descriptions. NO_MUTATION_UPDATE is a annotation name.
    public enum AnnotationType {
        NO_MUTATION_UPDATE("no-mutation-update"),
        NO_MUTATION_EXPORT("no-mutation-export"),
        NO_UNIPROT_UPDATE("no-uniprot-update");

        private String message;

        AnnotationType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
