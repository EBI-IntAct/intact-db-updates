package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

/**
 * Default implementation for ProteinUpdateError
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public abstract class DefaultProteinUpdateError implements ProteinUpdateError{

    private String errorMessage;
    private String proteinAc;

    public DefaultProteinUpdateError(String errorMessage, String proteinAc){
        this.errorMessage = errorMessage;
        this.proteinAc = proteinAc;
    }

    @Override
    public abstract UpdateError getUpdateErrorName();

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public String getProteinAc() {
        return this.proteinAc;
    }
}
