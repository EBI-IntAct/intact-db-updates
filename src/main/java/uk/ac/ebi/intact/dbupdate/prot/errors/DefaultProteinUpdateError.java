package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Default implementation for ProteinUpdateError
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public abstract class DefaultProteinUpdateError implements ProteinUpdateError{

    protected String errorMessage;
    protected String proteinAc;

    public DefaultProteinUpdateError(String errorMessage, String proteinAc){
        this.errorMessage = errorMessage;
        this.proteinAc = proteinAc;
    }

    @Override
    public String getErrorMessage() {
        return this.errorMessage;
    }

    @Override
    public String getProteinAc() {
        return this.proteinAc;
    }
}
