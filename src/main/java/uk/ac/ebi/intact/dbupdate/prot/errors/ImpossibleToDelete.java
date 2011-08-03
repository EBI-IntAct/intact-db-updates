package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for proteins which could not be deleted (usually because didn't have an ac)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleToDelete extends DefaultProteinUpdateError {

    public ImpossibleToDelete(String errorMessage, String proteinAc) {
        super(errorMessage, proteinAc);
    }
}
