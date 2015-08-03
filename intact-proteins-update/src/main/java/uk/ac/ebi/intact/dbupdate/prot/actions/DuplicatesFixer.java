package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;

/**
 * Interface for classes fixing protein duplicates
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13-Dec-2010</pre>
 */

public interface DuplicatesFixer {

    /**
     *
     * @return The protein deleter which is charged to delete duplicates
     */
    public ProteinDeleter getProteinDeleter();

    public void setProteinDeleter(ProteinDeleter deleter);

    /**
     *
     * @return The range updater which is charged to update ranges before the merge when duplicates have different sequences
     */
    public RangeFixer getRangeFixer();

    public void setRangeFixer(RangeFixer rangeFixer);

    /**
     *
     * @return the participant fixer which is charged to create deprecated proteins if the original protein have range conflicts with uniprot
     */
    public OutOfDateParticipantFixer getDeprecatedParticipantFixer();

    public void setDeprecatedParticipantFixer(OutOfDateParticipantFixer participantFixer);

    public Protein fixAllProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException;

    public void fixAllProteinTranscriptDuplicates(UpdateCaseEvent evt, Protein masterProtein) throws ProcessorException;

    public DuplicatesFinder getDuplicatesFinder();

    public void setDuplicatesFinder(DuplicatesFinder finder);


}
