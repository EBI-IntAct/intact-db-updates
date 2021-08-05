package uk.ac.ebi.intact.dbupdate.prot.errors;

/**
 * The list of possible errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public enum UpdateError {
    both_isoform_and_chain_xrefs,
    dead_protein_with_transcripts_not_dead,
    dead_uniprot_ac,
    fatal_error_during_update,
    impossible_merge,
    impossible_protein_remapping,
    impossible_transcript_parent_review,
    impossible_transcript_update,
    impossible_update_master,
    invalid_parent_xref,
    multi_uniprot_identities,
    not_matching_protein_transcript,
    organism_conflict_with_uniprot_protein,
    protein_impossible_to_delete,
    several_intact_parents,
    several_uniprot_entries_different_organisms,
    several_uniprot_entries_same_organim,
    uniprot_sequence_null,

}