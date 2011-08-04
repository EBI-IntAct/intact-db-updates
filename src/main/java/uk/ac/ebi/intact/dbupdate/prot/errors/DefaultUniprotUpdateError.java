package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04/08/11</pre>
 */

public class DefaultUniprotUpdateError  implements ProteinUpdateError {

    protected String errorMessage;
    protected String uniprotAc;
    private UpdateError errorLabel;

    public DefaultUniprotUpdateError(UpdateError errorLabel, String errorMessage, String uniprotAc){
        this.errorMessage = errorMessage;
        this.uniprotAc = uniprotAc;
        this.errorLabel = errorLabel;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    public String getUniprotAc() {
        return this.uniprotAc;
    }

    @Override
    public String getAccessionAc(){
        return this.uniprotAc;
    }

    @Override
    public UpdateError getErrorLabel(){
        return this.errorLabel;
    }
}
