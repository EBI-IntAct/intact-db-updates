package uk.ac.ebi.intact.tools.feature.shortlabel.generator.events;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ResultingSequenceChangedEvent {
    private String featureAc;
    private String interactorAc;
    private String rangeAc;
    private ChangeType changeType;
    private String message;

    public ResultingSequenceChangedEvent(String featureAc, String interactorAc, String rangeAc, ChangeType change) {
        this.featureAc = (featureAc == null) ? "undefined" : featureAc;
        this.interactorAc = (interactorAc == null) ? "undefined" : interactorAc;
        this.rangeAc = (rangeAc == null) ? "undefined" : rangeAc;
        this.changeType = change;
        this.message = change.getMessage();
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

    public ChangeType getChangeType() {
        return changeType;
    }

    public String getMessage() {
        return message;
    }

    public enum ChangeType {
        DELETION("Resulting sequence contains deletions"),
        INCREASE("Resulting sequence has increased"),
        DECREASE("Resulting sequence has decreased"),
        STABLE("Resulting sequence hasn't changed in length");

        private String message;

        ChangeType(String message) {
            this.message = message;
        }

        public String getMessage() {
            return message;
        }
    }
}
