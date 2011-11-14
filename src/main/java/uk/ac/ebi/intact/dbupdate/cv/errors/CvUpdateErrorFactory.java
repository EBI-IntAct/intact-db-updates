package uk.ac.ebi.intact.dbupdate.cv.errors;

/**
 * Interface for Cv update error factories
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public interface CvUpdateErrorFactory {

    public CvUpdateError createCvUpdateError(UpdateError errorLabel, String errorMessage, String termAc, String intactAc, String shortLabel);
}
