package uk.ac.ebi.intact.dbupdate.feature.exception;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class InteractorTypeException extends RuntimeException {

    private String message;

    public InteractorTypeException() {
        super();
    }

    public InteractorTypeException(String message) {
        super(message);
        this.message = message;
    }

    public InteractorTypeException(Throwable cause) {
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
