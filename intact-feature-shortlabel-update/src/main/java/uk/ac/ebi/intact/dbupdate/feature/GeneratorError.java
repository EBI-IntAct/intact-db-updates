package uk.ac.ebi.intact.dbupdate.feature;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class GeneratorError {
    private String featureAc;
    private String interactorAc;
    private String errorType;
    private String message;

    public GeneratorError(String featureAc, String interactorAc, String errorType, String message) {
        this.featureAc = featureAc;
        this.interactorAc = interactorAc;
        this.errorType = errorType;
        this.message = message;
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public Object getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    @Override
    public String toString() {
        return "GeneratorError{" +
                "featureAc='" + featureAc + '\'' +
                ", interactorAc='" + interactorAc + '\'' +
                ", errorType='" + errorType + '\'' +
                ", message='" + message + '\'' +
                '}';
    }
}
