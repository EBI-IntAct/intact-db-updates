package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;

import java.util.Collection;

/**
 * The interface to implement for each class which aims at finding protein duplicates
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13-Dec-2010</pre>
 */
public interface DuplicatesFinder {

    /**
     *
     * @param evt : the update case event containing the list of primary proteins and secondary proteins
     * @return DuplicatesFoundEvent if the evt contained duplicated proteins, null otherwise
     * @throws ProcessorException
     */
    public DuplicatesFoundEvent findProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException;

    /**
     *
     * @param evt : the update case event containing the list of primary isoforms and secondary isoforms
     * @return collection of DuplicatesFoundEvent for each set of duplicated isoforms, empty list otherwise
     * @throws ProcessorException
     */
    public Collection<DuplicatesFoundEvent> findIsoformDuplicates(UpdateCaseEvent evt) throws ProcessorException;

    /**
     *
     * @param evt : the update case event containing the list of primary feature chains
     * @return collection of DuplicatesFoundEvent for each set of duplicated feature chains, empty list otherwise
     * @throws ProcessorException
     */
    public Collection<DuplicatesFoundEvent> findFeatureChainDuplicates(UpdateCaseEvent evt) throws ProcessorException;
}
