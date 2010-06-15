package uk.ac.ebi.intact.dbupdate.dataset;

/**
 * TODO comment this
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

