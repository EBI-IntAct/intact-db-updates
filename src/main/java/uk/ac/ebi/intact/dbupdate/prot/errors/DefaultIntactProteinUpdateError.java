package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Default implementation for ProteinUpdateError
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public class DefaultIntactProteinUpdateError implements ProteinUpdateError{

    protected String errorMessage;
    protected String proteinAc;
    private UpdateError errorLabel;

    public DefaultIntactProteinUpdateError(UpdateError errorLabel, String errorMessage, String proteinAc){
        this.errorMessage = errorMessage;
        this.proteinAc = proteinAc;
        this.errorLabel = errorLabel;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getProteinAc() {
        return this.proteinAc;
    }

    @Override
    public String getAccessionAc(){
        return this.proteinAc;
    }

    @Override
    public UpdateError getErrorLabel(){
        return this.errorLabel;
    }
}
