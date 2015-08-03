package uk.ac.ebi.intact.dbupdate.cv.errors;

/**
 * Interface to implement for cv update errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public interface CvUpdateError {

    public UpdateError getErrorLabel();
    public String getErrorMessage();
    public String getTermAc();
    public String getIntactAc();
    public String getIntactLabel();
}
