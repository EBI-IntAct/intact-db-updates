package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

/**
 * Interface for errors found during protein update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02/08/11</pre>
 */

public interface ProteinUpdateError {

    public UpdateError getUpdateErrorName();
    public String getErrorMessage();
    public String getProteinAc();
}
