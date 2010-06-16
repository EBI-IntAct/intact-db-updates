package uk.ac.ebi.intact.dbupdate.dataset;

/**
 * The exception thrown if an error occured while processing the dataset
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Jun-2010</pre>
 */

public class DatasetException extends Exception{
    public DatasetException() {
        super();
    }

    public DatasetException(String message) {
        super(message);
    }

    public DatasetException(String message, Throwable cause) {
        super(message, cause);
    }

    public DatasetException(Throwable cause) {
        super(cause);
    }
}

