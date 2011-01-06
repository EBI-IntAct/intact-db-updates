package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;

/**
 * Interface to implement for classes filtering proteins we want to exclude from the protein-update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface ProteinUpdateFilter {

    /**
     *
     * @param evt : contains protein to look at
     * @return  a unique uniprot ac for this protein if possible, null otherwise. The uniprot ac returned will
     * be used for updating the protein later
     * @throws ProcessorException
     */
    public String filterOnUniprotIdentity(ProteinEvent evt) throws ProcessorException;

    /**
     * Filter all the proteins to exclude from the protein update and remove them from the list of proteins to update
     * @param evt : case event with all the proteins matching a single uniprot entry
     */
    public void filterNonUniprotAndMultipleUniprot(UpdateCaseEvent evt);

    public UniprotProteinMapper getProteinMappingManager();

    public void setProteinMappingManager(UniprotProteinMapper proteinMappingManager);
}
