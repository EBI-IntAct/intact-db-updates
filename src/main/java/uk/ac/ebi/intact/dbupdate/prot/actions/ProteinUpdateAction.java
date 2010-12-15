package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;

/**
 * During a protein update, specific actions can be run, either from a Proteinevent or an UpdateCaseEvent
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface ProteinUpdateAction {

    public boolean runAction(ProteinEvent evt);

    public void runActionForAll(UpdateCaseEvent evt);
}
