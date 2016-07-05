package uk.ac.ebi.intact.tools.feature.shortlabel.generator.exception;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureShortlabelGenerationException extends RuntimeException {
    
    private String message;

    public FeatureShortlabelGenerationException() {
        super();
    }

    public FeatureShortlabelGenerationException(String message) {
        super(message);
        this.message = message;
    }

    public FeatureShortlabelGenerationException(Throwable cause) {
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
