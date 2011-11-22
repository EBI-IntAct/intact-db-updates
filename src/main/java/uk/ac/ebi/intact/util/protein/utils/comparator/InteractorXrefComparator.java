package uk.ac.ebi.intact.util.protein.utils.comparator;

import uk.ac.ebi.intact.model.InteractorXref;

import java.util.Comparator;

/**
 * Comparator for interactor xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/11/11</pre>
 */

public class InteractorXrefComparator implements Comparator<InteractorXref>{

    @Override
    public int compare(InteractorXref o1, InteractorXref o2) {

        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o1.getCvDatabase() != null && o2.getCvDatabase() != null){
            if (o1.getCvDatabase().getIdentifier().equalsIgnoreCase(o2.getCvDatabase().getIdentifier())){
                return o1.getPrimaryId().toLowerCase().compareTo(o2.getPrimaryId().toLowerCase());
            }
            else {
                return o1.getCvDatabase().getIdentifier().toLowerCase().compareTo(o2.getCvDatabase().getIdentifier().toLowerCase());
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
