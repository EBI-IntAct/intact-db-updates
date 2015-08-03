package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.model.CvObjectAlias;

import java.util.Comparator;

/**
 * Comparato for cv aliases
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class CvAliasComparator implements Comparator<CvObjectAlias> {

    @Override
    public int compare(CvObjectAlias o1, CvObjectAlias o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        if (o1.getName() != null && o2.getName() != null){
            return o1.getName().compareTo(o2.getName());
        }
        else if (o1.getName() == null && o2.getName() != null){
            return AFTER;
        }
        else if (o1.getName() != null && o2.getName() == null){
            return BEFORE;
        }
        else {
            return EQUAL;
        }
    }
}
