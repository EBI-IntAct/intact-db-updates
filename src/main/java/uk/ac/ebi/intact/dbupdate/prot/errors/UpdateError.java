package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * The list of possible errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public enum UpdateError {

    multi_uniprot_identities, dead_uniprot_ac, several_uniprot_entries_same_organim, several_uniprot_entries_different_organisms, impossible_merge, not_matching_protein_transcript,
    protein_impossible_to_delete, organism_conflict_with_uniprot_protein, uniprot_sequence_null, both_isoform_and_chain_xrefs, invalid_parent_xref, several_intact_parents, impossible_transcript_parent_review,
    impossible_update_master, dead_protein_with_transcripts_not_dead, impossible_protein_remapping, impossible_transcript_update, fatal_error_during_update
}
