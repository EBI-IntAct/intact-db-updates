package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.model.CvObjectXref;

import java.util.Comparator;

/**
 * Comparator for cvXrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class CvXrefComparator implements Comparator<CvObjectXref>{
    @Override
    public int compare(CvObjectXref o1, CvObjectXref o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // both have a database
        if (o1.getCvDatabase() != null && o2.getCvDatabase() != null){
            // databases identical, we can sort by qualifier
            if (o1.getCvDatabase().getIdentifier().equalsIgnoreCase(o2.getCvDatabase().getIdentifier())){

                // both have a qualifier
                if (o1.getCvXrefQualifier() != null && o2.getCvXrefQualifier() != null){
                    // qualifier identical, we can sort by primary id
                    if (o1.getCvXrefQualifier().getIdentifier().equalsIgnoreCase(o2.getCvXrefQualifier().getIdentifier())){

                        return o1.getPrimaryId().toLowerCase().compareTo(o2.getPrimaryId().toLowerCase());
                    }
                    // qualifiers are different, we sort first by qualifiers
                    else {
                        return o1.getCvXrefQualifier().getIdentifier().compareTo(o2.getCvXrefQualifier().getIdentifier());
                    }
                }
                else if (o1.getCvXrefQualifier() == null && o2.getCvXrefQualifier() != null){
                    return AFTER;
                }
                else if (o1.getCvXrefQualifier() != null && o2.getCvXrefQualifier() == null){
                    return BEFORE;
                }
                else {
                    return o1.getPrimaryId().toLowerCase().compareTo(o2.getPrimaryId().toLowerCase());
                }
            }
            // databases are different, we sort first by database
            else {
                return o1.getCvDatabase().getIdentifier().compareTo(o2.getCvDatabase().getIdentifier());
            }
        }
        else if (o1.getCvDatabase() == null && o2.getCvDatabase() != null){
            return AFTER;
        }
        else if (o1.getCvDatabase() != null && o2.getCvDatabase() == null){
            return BEFORE;
        }
        else {
            return o1.getPrimaryId().toLowerCase().compareTo(o2.getPrimaryId().toLowerCase());
        }
    }
}
