package uk.ac.ebi.intact.dbupdate.prot;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public enum UpdateError {

    multi_uniprot_identities, dead_uniprot_ac, several_uniprot_entries_same_organim, several_uniprot_entries_different_organisms, impossible_merge, not_matching_protein_transcript,
    protein_with_ac_null_to_delete, organism_conflict_with_uniprot_protein, uniprot_sequence_null, uniprot_sequence_null_intact_sequence_not_null,
    feature_conflicts, both_isoform_and_chain_xrefs, dead_parent_xref, several_intact_parents, transcript_without_parent, alias_duplicates,
    xref_duplicates, annotations_duplicates
}
