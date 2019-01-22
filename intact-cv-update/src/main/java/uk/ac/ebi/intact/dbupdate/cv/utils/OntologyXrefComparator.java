package uk.ac.ebi.intact.dbupdate.cv.utils;

import psidev.psi.mi.jami.model.Xref;

import java.util.Comparator;

/**
 * Comparator for ontology xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class OntologyXrefComparator implements Comparator<Xref>{

    @Override
    public int compare(Xref o1, Xref o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        //We keep the sa,e logic for the comparation, but maybe this can be replace with one of the
        // jami-core comparators
        if (o1.getDatabase().getMIIdentifier() != null && o2.getDatabase().getMIIdentifier() != null){
            // databases identical, we can sort by qualifier
            if (o1.getDatabase().getMIIdentifier().equalsIgnoreCase(o2.getDatabase().getMIIdentifier())){

                // both have a qualifier
                if (o1.getQualifier().getMIIdentifier() != null && o2.getQualifier().getMIIdentifier() != null){
                    // qualifier identical, we can sort by primary id
                    if (o1.getQualifier().getMIIdentifier().equalsIgnoreCase(o2.getQualifier().getMIIdentifier())){

                        return o1.getId().toLowerCase().compareTo(o2.getId().toLowerCase());
                    }
                    // qualifiers are different, we sort first by qualifiers
                    else {
                        return o1.getQualifier().getMIIdentifier().compareTo(o2.getQualifier().getMIIdentifier());
                    }
                }
                else if (o1.getQualifier().getMIIdentifier() == null && o2.getQualifier().getMIIdentifier() != null){
                    return AFTER;
                }
                else if (o1.getQualifier().getMIIdentifier() != null && o2.getQualifier().getMIIdentifier() == null){
                    return BEFORE;
                }
                else {
                    return o1.getQualifier().getMIIdentifier().toLowerCase().compareTo(o2.getQualifier().getMIIdentifier().toLowerCase());
                }
            }
            // databases are different, we sort first by database
            else {
                return o1.getDatabase().getMIIdentifier().compareTo(o2.getDatabase().getMIIdentifier());
            }
        }
        else if (o1.getDatabase().getMIIdentifier() == null && o2.getDatabase().getMIIdentifier() != null){
            return AFTER;
        }
        else if (o1.getDatabase().getMIIdentifier() != null && o2.getDatabase().getMIIdentifier() == null){
            return BEFORE;
        }
        else {
            return o1.getId().toLowerCase().compareTo(o2.getId().toLowerCase());
        }
    }
}
