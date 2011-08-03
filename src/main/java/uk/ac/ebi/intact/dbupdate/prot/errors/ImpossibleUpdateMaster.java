package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * Error for master proteins impossible to update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleUpdateMaster extends DefaultProteinUpdateError {

    public ImpossibleUpdateMaster(String errorMessage, String proteinAc) {
        super(errorMessage, proteinAc);
    }
}
