package uk.ac.ebi.intact.dbupdate.cv.errors;

/**
 * labels for cv update errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public enum UpdateError {

    non_existing_term, ontology_database_no_found, cv_impossible_merge, not_found_intact_ac, multi_identities, null_identifier,
    impossible_import, ontology_access_not_found, several_matching_ontology_accesses, duplicated_cv, cv_class_not_found, invalid_cv_class, fatal
}
