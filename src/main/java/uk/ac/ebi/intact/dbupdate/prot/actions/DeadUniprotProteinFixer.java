package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;

/**
 * This interface is for classes updating dead proteins in uniprot.
 *
 * A dead protein in uniprot is a uniprot ac which has been deleted in uniprot and which is not matching any uniprot entries anymore
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13-Dec-2010</pre>
 */

public interface DeadUniprotProteinFixer {

    /**
     * Update the protein which is dead in uniprot
     * @param evt : protein event containing the protein to update and the uniprot identifier of this protein
     * @throws ProcessorException
     */
    public void fixDeadProtein(ProteinEvent evt) throws ProcessorException;
}
