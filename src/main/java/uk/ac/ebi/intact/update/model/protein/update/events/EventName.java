package uk.ac.ebi.intact.update.model.protein.update.events;

/**
 * Name of a proteinAc update event
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */

public enum EventName {

    dead_protein, uniprot_update, deleted_protein, created_protein, non_uniprot_protein, update_error, secondary_protein, protein_duplicate,
    participant_with_feature_conflicts, transcript_parent_update

}
