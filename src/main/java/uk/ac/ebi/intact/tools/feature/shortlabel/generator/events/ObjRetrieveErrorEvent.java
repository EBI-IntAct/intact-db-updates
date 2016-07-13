package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ObjRetrieveErrorEvent {
    private String featureAc;
    private String interactorAc;
    private ErrorType errorType;
    private String message;

    public ObjRetrieveErrorEvent(String featureAc, String interactorAc, ErrorType error) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.errorType = error;
        this.message = error.getMessage();
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public enum ErrorType {
        UNABLE_RETRIEVE_INTERACTOR("Can not receive Interactor from IntactFeatureEvidence object"),
        UNABLE_TO_RETRIEVE_FEATURE("Can not receive IntactFeatureEvidence"),
        UNABLE_TO_RETRIEVE_CV_REMARK_INTERNAL("Can not receive remark-internal IntactCvTerm object"),
        UNABLE_TO_RETRIEVE_CV_NO_MUTATION_UPDATE("Can not receive no-mutation-update IntactCvTerm object"),
        UNABLE_TO_RETRIEVE_CV_NO_MUTATION_EXPORT("Can not receive no-mutation-export IntactCvTerm object"),
        UNABLE_RETRIEVE_INTERACTOR_SEQUENCE("Can not receive Interactor Sequence from Interactor object");

        private String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
