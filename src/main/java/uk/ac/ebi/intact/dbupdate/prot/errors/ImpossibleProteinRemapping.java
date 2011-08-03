package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * This error is for proteins for which the remapping was successful but not allowed
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03/08/11</pre>
 */

public class ImpossibleProteinRemapping extends DefaultProteinUpdateError {
    public ImpossibleProteinRemapping(String errorMessage, String proteinAc) {
        super(errorMessage, proteinAc);
    }
}
