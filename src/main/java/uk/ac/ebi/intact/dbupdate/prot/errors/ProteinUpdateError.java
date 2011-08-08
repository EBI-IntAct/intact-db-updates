package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Interface for errors found during protein update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public interface ProteinUpdateError {

    public UpdateError getErrorLabel();
    public String getErrorMessage();
}
