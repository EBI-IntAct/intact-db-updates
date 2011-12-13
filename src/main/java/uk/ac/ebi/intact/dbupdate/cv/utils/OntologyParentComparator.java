package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyTermI;

import java.util.Comparator;

/**
 * Comparator for parents of an ontology term
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/12/11</pre>
 */

public class OntologyParentComparator implements Comparator<IntactOntologyTermI> {

    @Override
    public int compare(IntactOntologyTermI o1, IntactOntologyTermI o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o1.getTermAccession() != null && o2.getTermAccession() != null){
            return o1.getTermAccession().compareTo(o2.getTermAccession());
        }
        else if (o1.getTermAccession() == null && o2.getTermAccession() != null){
            return AFTER;
        }
        else if (o1.getTermAccession() != null && o2.getTermAccession() == null){
            return BEFORE;
        }
        else {
            return EQUAL;
        }
    }
}