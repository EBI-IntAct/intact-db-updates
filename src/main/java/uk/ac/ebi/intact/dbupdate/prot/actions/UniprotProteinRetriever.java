package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface UniprotProteinRetriever {

    public UniprotProtein retrieveUniprotEntry(String uniprotAc);

    public UniprotProtein retrieveUniprotEntry(ProteinEvent evt) throws ProcessorException;

    public void filterAllSecondaryProteinsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException;

    public UniprotService getUniprotService();

    public void setUniprotService(UniprotService uniprotService);

    public DeadUniprotProteinFixer getDeadUniprotFixer();

    public void setDeadUniprotFixer(DeadUniprotProteinFixer deadUniprotFixer);
}
