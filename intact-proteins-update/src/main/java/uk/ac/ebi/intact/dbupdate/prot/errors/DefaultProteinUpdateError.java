package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Default implementation for ProteinUpdateError
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public class DefaultProteinUpdateError implements ProteinUpdateError{

    protected String errorMessage;
    private UpdateError errorLabel;

    public DefaultProteinUpdateError(UpdateError errorLabel, String errorMessage){
        this.errorMessage = errorMessage;
        this.errorLabel = errorLabel;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public UpdateError getErrorLabel(){
        return this.errorLabel;
    }
}
