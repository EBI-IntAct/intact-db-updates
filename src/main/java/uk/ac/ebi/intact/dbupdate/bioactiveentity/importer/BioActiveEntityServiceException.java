package uk.ac.ebi.intact.dbupdate.bioactiveentity.importer;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 23/05/2013
 * Time: 09:48
 * To change this template use File | Settings | File Templates.
 */
public class BioActiveEntityServiceException extends Exception {

    public BioActiveEntityServiceException() {
    }

    public BioActiveEntityServiceException(Throwable cause) {
        super(cause);
    }

    public BioActiveEntityServiceException(String message) {
        super(message);
    }

    public BioActiveEntityServiceException(String message, Throwable cause) {
        super(message, cause);
    }

}
