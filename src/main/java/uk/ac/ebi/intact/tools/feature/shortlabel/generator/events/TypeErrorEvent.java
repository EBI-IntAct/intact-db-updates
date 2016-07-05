package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;


/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class TypeErrorEvent {
    private String featureAc;
    private String interactorAc;
    private ObjTypeErrorType errorType;
    private String message;

    public TypeErrorEvent(String featureAc, String interactorAc, ObjTypeErrorType error) {
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

    public ObjTypeErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public enum ObjTypeErrorType {
        WRONG_INTERACTOR_TYPE("Interactor is not of type mutation"),
        WRONG_FEATURE_TYPE("Feature is not of type mutation");

        private String message;

        ObjTypeErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
