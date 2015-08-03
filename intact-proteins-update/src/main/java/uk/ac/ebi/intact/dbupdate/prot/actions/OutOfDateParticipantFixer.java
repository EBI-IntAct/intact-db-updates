package uk.ac.ebi.intact.dbupdate.prot.actions;

import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

/**
 * The interface to implement for classes dealing with participants having range conflicts. (feature range not valid with the sequence of the protein)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>14-Dec-2010</pre>
 */

public interface OutOfDateParticipantFixer {

    /**
     *
     * @param sequence : the sequence to retrieve
     * @param uniprotProtein : the uniprot entry to look into
     * @return the uniprot transcript with the sequence we wanted to retrieve and which is not the canonical sequence in uniprot
     */
    public UniprotProteinTranscript findTranscriptsWithIdenticalSequence(String sequence, UniprotProtein uniprotProtein);

    /**
     *
     * @param sequence : the sequence to retrieve
     * @param uniprotProteinTranscript : uniprot transcript we want to exclude
     * @param uniprotProtein : the uniprot entry to look into
     * @return the uniprot transcript with the sequence we wanted to retrieve and which is not the transcript we want to exclude
     */
    public UniprotProteinTranscript findTranscriptsWithIdenticalSequence(String sequence, UniprotProteinTranscript uniprotProteinTranscript, UniprotProtein uniprotProtein);

    /**
     *
     * @param evt : contains protein having range conflicts, the list of components affected by range conflicts and the parent ac if a protein transcript need to be created
     * @param createDeprecatedParticipant : to know if we want to create a deprecated protein with the range conflicts if no transcript can be found
     * @return the remapped protein transcript if it has been found, a deprecated protein if createDeprecated and no protein transcript can be found with
     * the same sequence, null otherwise. If fixOutOfDateRange is true, we allows the program to reset the out of date range to undetermined if it cannot be remapped to any proteins
     */
    public ProteinTranscript fixParticipantWithRangeConflicts(OutOfDateParticipantFoundEvent evt, boolean createDeprecatedParticipant, boolean fixOutOfDateRanges);

    /**
     *
     * @param evt : contains protein having range conflicts, the list of components affected by range conflicts and the parent ac if a protein transcript need to be created
     * @return the created deprecated protein
     */
    public ProteinTranscript createDeprecatedProtein(OutOfDateParticipantFoundEvent evt);

    /**
     *
     * @return The range updater which is charged to update ranges if the participant fixer cannot fix the problem
     */
    public RangeFixer getRangeFixer();

    public void setRangeFixer(RangeFixer rangeFixer);
}
