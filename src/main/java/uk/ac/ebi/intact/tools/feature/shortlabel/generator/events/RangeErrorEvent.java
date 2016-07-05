package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class RangeErrorEvent {
    private String featureAc;
    private String interactorAc;
    private String rangeAc;
    private ErrorType errorType;
    private String message;

    public RangeErrorEvent(String featureAc, String interactorAc, String rangeAc, ErrorType error) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.rangeAc = (rangeAc == null) ? "undefined" : rangeAc;
        this.errorType = error;
        this.message = error.getMessage();
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getInteractorAc() {
        return interactorAc;
    }

    public String getRangeAc() {
        return rangeAc;
    }

    public ErrorType getErrorType() {
        return errorType;
    }

    public String getMessage() {
        return message;
    }

    public enum ErrorType {
        RANGE_NULL("No ranges could be found"),
        START_POS_ZERO("Starting position is 0"),
        START_POS_UNDETERMINED("Positions are undetermined"),
        RES_SEQ_NULL("Resulting sequence is null"),
        ORG_SEQ_NULL("Original sequence is null");

        private String message;

        ErrorType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
