package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.bridges.ontology_manager.TermDbXref;

import java.util.Comparator;

/**
 * Comparator for ontology xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class OntologyXrefComparator implements Comparator<TermDbXref>{

    @Override
    public int compare(TermDbXref o1, TermDbXref o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o1.getDatabaseId() != null && o2.getDatabaseId() != null){
            // databases identical, we can sort by qualifier
            if (o1.getDatabaseId().equalsIgnoreCase(o2.getDatabaseId())){

                // both have a qualifier
                if (o1.getQualifierId() != null && o2.getQualifierId() != null){
                    // qualifier identical, we can sort by primary id
                    if (o1.getQualifierId().equalsIgnoreCase(o2.getQualifierId())){

                        return o1.getAccession().toLowerCase().compareTo(o2.getAccession().toLowerCase());
                    }
                    // qualifiers are different, we sort first by qualifiers
                    else {
                        return o1.getQualifierId().compareTo(o2.getQualifierId());
                    }
                }
                else if (o1.getQualifierId() == null && o2.getQualifierId() != null){
                    return AFTER;
                }
                else if (o1.getQualifierId() != null && o2.getQualifierId() == null){
                    return BEFORE;
                }
                else {
                    return o1.getQualifierId().toLowerCase().compareTo(o2.getQualifierId().toLowerCase());
                }
            }
            // databases are different, we sort first by database
            else {
                return o1.getDatabaseId().compareTo(o2.getDatabaseId());
            }
        }
        else if (o1.getDatabaseId() == null && o2.getDatabaseId() != null){
            return AFTER;
        }
        else if (o1.getDatabaseId() != null && o2.getDatabaseId() == null){
            return BEFORE;
        }
        else {
            return o1.getAccession().toLowerCase().compareTo(o2.getAccession().toLowerCase());
        }
    }
}
