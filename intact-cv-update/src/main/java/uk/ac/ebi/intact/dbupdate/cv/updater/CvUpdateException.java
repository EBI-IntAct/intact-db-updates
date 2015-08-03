package uk.ac.ebi.intact.dbupdate.cv.updater;

/**
 * Exception thrown when updating a cv term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20/01/12</pre>
 */

public class CvUpdateException extends Exception{

    public CvUpdateException() {
        super();
    }

    public CvUpdateException(String s) {
        super(s);
    }

    public CvUpdateException(String s, Throwable throwable) {
        super(s, throwable);
    }

    public CvUpdateException(Throwable throwable) {
        super(throwable);
    }
}
