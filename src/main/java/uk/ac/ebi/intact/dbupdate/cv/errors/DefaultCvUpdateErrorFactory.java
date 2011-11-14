package uk.ac.ebi.intact.dbupdate.cv.errors;

/**
 * Default implementation for CvUpdateErrorFactory
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class DefaultCvUpdateErrorFactory implements CvUpdateErrorFactory{

    public CvUpdateError createCvUpdateError(UpdateError errorLabel, String errorMessage, String termAc, String intactAc, String shortLabel){
        return new DefaultCvUpdateError(errorLabel, errorMessage, termAc, intactAc, shortLabel);
    }
}
