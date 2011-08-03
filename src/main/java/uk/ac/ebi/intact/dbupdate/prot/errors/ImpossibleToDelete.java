package uk.ac.ebi.intact.dbupdate.prot.errors;

import uk.ac.ebi.intact.dbupdate.prot.UpdateError;

/**
 * Error for proteins which could not be deleted (usually because didn't have an ac)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleToDelete extends DefaultProteinUpdateError {

    public ImpossibleToDelete(String errorMessage, String proteinAc) {
        super(UpdateError.protein_impossible_to_delete, errorMessage, proteinAc);
    }
}
