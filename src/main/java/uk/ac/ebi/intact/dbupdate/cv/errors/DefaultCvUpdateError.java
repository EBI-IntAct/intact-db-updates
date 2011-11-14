package uk.ac.ebi.intact.dbupdate.cv.errors;

/**
 * Term which exists in the database but not in the ontology
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14/11/11</pre>
 */

public class DefaultCvUpdateError implements CvUpdateError{
    protected UpdateError errorLabel;
    protected String errorMessage;
    protected String termAc;
    protected String intactAc;
    protected String intactLabel;

    public DefaultCvUpdateError(UpdateError errorLabel, String errorMessage, String termAc, String intactAc, String shortLabel){
        this.errorLabel = errorLabel;
        this.errorMessage = errorMessage;
        this.termAc = termAc;
        this.intactAc = intactAc;
        this.intactLabel = shortLabel;
    }

    @Override
    public UpdateError getErrorLabel() {
        return errorLabel;
    }

    @Override
    public String getErrorMessage() {
        return errorMessage;
    }

    @Override
    public String getTermAc() {
        return termAc;
    }

    @Override
    public String getIntactAc() {
        return intactAc;
    }

    @Override
    public String getIntactLabel() {
        return intactLabel;
    }
}
