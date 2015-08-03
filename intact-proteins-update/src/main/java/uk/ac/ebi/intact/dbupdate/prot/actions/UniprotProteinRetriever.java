package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

/**
 * This interface is for classes retrieving uniprot entries matching an intact protein
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface UniprotProteinRetriever {

    /**
     *
     * @param uniprotAc
     * @return the unique uniprot entry matching this ac, null otherwise
     */
    public UniprotProtein retrieveUniprotEntry(String uniprotAc);

    /**
     *
     * @param evt
     * @return the unique uniprot entry matching the uniprot identity of the event, null otherwise
     * @throws ProcessorException
     */
    public UniprotProtein retrieveUniprotEntry(ProteinEvent evt) throws ProcessorException;

    /**
     * Filter proteins which could not be updated because not matching a single uniprot entry
     * @param evt
     * @throws ProcessorException
     */
    public void filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException;

    /**
     * Process the protein not found in uniprot
     * @param evt
     */
    public void processProteinNotFoundInUniprot(ProteinEvent evt);

    public UniprotService getUniprotService();

    public void setUniprotService(UniprotService uniprotService);

    public DeadUniprotProteinFixer getDeadUniprotFixer();

    public void setDeadUniprotFixer(DeadUniprotProteinFixer deadUniprotFixer);

    public UniprotProteinMapper getProteinMappingManager();

    public void setProteinMappingManager(UniprotProteinMapper proteinMappingManager);
}
