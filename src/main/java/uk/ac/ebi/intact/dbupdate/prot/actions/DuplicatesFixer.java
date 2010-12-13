package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.DuplicateReport;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13-Dec-2010</pre>
 */

public interface DuplicatesFixer {

    /**
     * Merge protein duplicates
     * @param evt : contains the lits of duplicated proteins to merge
     * @return A DuplicateReport containing the protein kept from the merge and the list of components having feature range conflicts
     * @throws ProcessorException
     */
    public DuplicateReport fixProteinDuplicates(DuplicatesFoundEvent evt) throws ProcessorException;

    /**
     *
     * @return The protein deleter which is charged to delete duplicates
     */
    public ProteinDeleter getProteinDeleter();

    /**
     *
     * @return The range updater which is charged to update ranges before the merge when duplicates have different sequences
     */
    public RangeFixer getRangeFixer();

    /**
     *
     * @return the participant fixer which is charged to create deprecated proteins if the original protein have range conflicts with uniprot
     */
    public OutOfDateParticipantFixer getDeprecatedParticipantFixer();
}
