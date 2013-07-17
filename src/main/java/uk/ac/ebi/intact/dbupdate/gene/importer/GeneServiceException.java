package uk.ac.ebi.intact.dbupdate.gene.importer;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 14:28
 * To change this template use File | Settings | File Templates.
 */
public class GeneServiceException extends Exception {

    public GeneServiceException() {
    }

    public GeneServiceException(Throwable cause) {
        super(cause);
    }

    public GeneServiceException(String message) {
        super(message);
    }

    public GeneServiceException(String message, Throwable cause) {
        super(message, cause);
    }
}
