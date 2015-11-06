package uk.ac.ebi.intact.dbupdate.feature.exception;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class SequenceException extends RuntimeException {
    
    private String message;

    public SequenceException() {
        super();
    }

    public SequenceException(String message) {
        super(message);
        this.message = message;
    }

    public SequenceException(Throwable cause) {
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
