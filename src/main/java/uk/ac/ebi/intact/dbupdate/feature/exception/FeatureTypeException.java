package uk.ac.ebi.intact.dbupdate.feature.exception;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureTypeException extends RuntimeException {
    
    private String message;

    public FeatureTypeException() {
        super();
    }

    public FeatureTypeException(String message) {
        super(message);
        this.message = message;
    }

    public FeatureTypeException(Throwable cause) {
        super(cause);
    }

    @Override
    public String toString() {
        return message;
    }

    @Override
    public String getMessage() {
        return message;
    }
}
