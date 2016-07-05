package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;


/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class SequenceErrorEvent {
    private String featureAc;
    private String interactorAc;
    private String rangeAc;
    private ErrorType errorType;
    private String message;


    public SequenceErrorEvent(String featureAc, String interactorAc, String rangeAc, ErrorType error) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.rangeAc = (rangeAc == null) ? "undefined" : rangeAc;
        this.errorType = error;
        this.message = error.getMessage();
    }

    public SequenceErrorEvent(String featureAc, String interactorAc, String rangeAc, ErrorType error, String message) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.rangeAc = (rangeAc == null) ? "undefined" : rangeAc;
        this.errorType = error;
        this.message = message;
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public void setFeatureAc(String featureAc) {
        this.featureAc = featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public void setInteractorAc(String interactorAc) {
        this.interactorAc = interactorAc;
    }

    public String getRangeAc() {
        return rangeAc;
    }

    public void setRangeAc(String rangeAc) {
        this.rangeAc = rangeAc;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public void setErrorType(ErrorType errorType) {
        this.errorType = errorType;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public enum ErrorType {
        UNABLE_CALCULATE_ORG_SEQ("Couldn't calculate original sequence from whole sequence"),
        ORG_SEQ_WRONG("Original sequence does not match interactor sequence"),
        RES_SEQ_CONTAINS_LOWER_CASE("Resulting sequence contains lower case letters");

        private String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
