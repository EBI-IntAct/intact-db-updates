package uk.ac.ebi.intact.dbupdate.prot.errors;

import org.apache.commons.lang.exception.ExceptionUtils;

/**
 * This error occurs when an exception is thrown while updating a protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/08/11</pre>
 */

public class FatalErrorDuringUpdate extends DefaultProteinUpdateError implements IntactUpdateError, UniprotUpdateError{

    private String proteinAc;
    private String uniprotAc;

    private Exception exception;

    public FatalErrorDuringUpdate(String proteinAc, String uniprotAc, Exception exception) {
        super(UpdateError.fatal_error_during_update, exception != null ? exception.getMessage() : null);
        this.proteinAc = proteinAc;
        this.uniprotAc = uniprotAc;
        this.exception = exception;
    }

    @Override
    public String getProteinAc() {
        return this.proteinAc;
    }

    @Override
    public String getUniprotAc() {
        return this.uniprotAc;
    }

    public Exception getException() {
        return exception;
    }

    @Override
    public String getErrorMessage(){
        if ((this.proteinAc == null && this.uniprotAc == null) || this.exception == null){
            return super.getErrorMessage();
        }

        StringBuffer error = new StringBuffer();
        if (proteinAc != null){
            error.append("Impossible to update the protein ");
            error.append(proteinAc);
        }
        else{
            error.append("Impossible to update the proteins ");
        }

        if (uniprotAc != null){
            error.append("attached to the uniprot entry ");
            error.append(uniprotAc);
        }

        error.append(" because a ");
        error.append(exception.getMessage());
        error.append(" was thrown : ");

        error.append(ExceptionUtils.getFullStackTrace(exception).replace("\n", ""));

        return error.toString();
    }
}
