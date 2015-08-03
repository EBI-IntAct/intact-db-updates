package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;

/**
 * Interface to implement for classes dealing with proteins having secondary acs in uniprot.
 * Collect all proteins in the database related to a single uniprot entry
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface UniprotIdentityUpdater {

    /**
     * Collect all proteins related to a single uniprot entry in the event and set the lists of intact proteins related to this uniprot entry
     * @param evt
     * @return
     * @throws ProcessorException
     */
    public UpdateCaseEvent collectPrimaryAndSecondaryProteins(ProteinEvent evt) throws ProcessorException;

    /**
     * Update all the secondary proteins/isoforms in the event.
     * @param evt
     */
    public void updateAllSecondaryProteins(UpdateCaseEvent evt);
}
