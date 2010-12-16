package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.DuplicateReport;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;

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

    public Protein fixAllProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException;

    public void fixAllProteinTranscriptDuplicates(UpdateCaseEvent evt) throws ProcessorException;

    public DuplicatesFinder getDuplicatesFinder();

    public void setDuplicatesFinder(DuplicatesFinder duplicatesFinder);
}
