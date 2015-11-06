package uk.ac.ebi.intact.dbupdate.feature.exception;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class RangeException extends RuntimeException {

    private String message;

    public RangeException() {
        super();
    }

    public RangeException(String message) {
        super(message);
        this.message = message;
    }

    public RangeException(Throwable cause) {
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
