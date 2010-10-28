package uk.ac.ebi.intact.update.model.proteinupdate;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */

public enum EventName {

    created_protein, duplicated_protein, dead_protein, protein_impossible_to_merge, uniprot_update, sequence_update, delete_protein_without_interactions,
    merged_protein, protein_impossible_to_update_sequence, range_shifted, impossible_to_shift_ranges, invalid_ranges
}
