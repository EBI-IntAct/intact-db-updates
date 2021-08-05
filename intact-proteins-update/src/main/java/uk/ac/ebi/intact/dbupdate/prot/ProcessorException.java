package uk.ac.ebi.intact.dbupdate.prot;

/**
 * The Exception thrown by a Processor
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProcessorException extends RuntimeException{

    public ProcessorException() {
        super();
    }

    public ProcessorException(String message) {
        super(message);
    }

    public ProcessorException(String message, Throwable cause) {
        super(message, cause);
    }

    public ProcessorException(Throwable cause) {
        super(cause);
    }
}