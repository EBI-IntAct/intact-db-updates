package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;

import java.util.Set;

/**
 * Interface to implement for classes selecting protein without interactions to delete
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */
public interface ProtWithoutInteractionDeleter {

    /**
     *
     * @param evt : contains the protein to check
     * @return true if the protein must be deleted
     * @throws ProcessorException
     */
    public boolean hasToBeDeleted(ProteinEvent evt) throws ProcessorException;

    /**
     * The proteins which must be deleted are removed from the list of protein to update and are returned.
     * @param evt
     * @return  list of proteins to delete
     */
    public Set<Protein> collectAndRemoveProteinsWithoutInteractions(UpdateCaseEvent evt);
}
