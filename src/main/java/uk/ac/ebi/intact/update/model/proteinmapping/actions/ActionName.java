package uk.ac.ebi.intact.update.model.proteinmapping.actions;

/**
 * The possible names of an action
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>01-Apr-2010</pre>
 */

public enum ActionName {
    PICR_accession, PICR_sequence_Swissprot, PICR_sequence_Trembl, BLAST_swissprot, SEARCH_uniprot_name, SEARCH_intact_exact_shortLabel,
    SEARCH_intact_shortLabel, SEARCH_intact_fullName, wide_SEARCH_uniprot, BLAST_uniprot, SEARCH_intact_crc64, BLAST_Swissprot_Remapping,
    BLAST_Swissprot_Total_Identity, BLAST_Uniprot_Total_Identity, update_checking, feature_range_checking, SEARCH_Swissprot_CrossReference,
    SEARCH_Uniprot_CrossReference, SEARCH_Uniparc_CrossReference
}
