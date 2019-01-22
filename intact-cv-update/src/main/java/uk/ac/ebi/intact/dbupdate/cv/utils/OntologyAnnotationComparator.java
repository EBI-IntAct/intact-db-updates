package uk.ac.ebi.intact.dbupdate.cv.utils;

import psidev.psi.mi.jami.model.Annotation;

import java.util.Comparator;

/**
 * Comparator for ontology annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class OntologyAnnotationComparator implements Comparator<Annotation>{
    
    @Override
    public int compare(Annotation o1, Annotation o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // both have a topic
        if (o1.getTopic().getMIIdentifier() != null && o2.getTopic().getMIIdentifier() != null){
            // topic identical, we can sort by text
            if (o1.getTopic().getMIIdentifier().equalsIgnoreCase(o2.getTopic().getMIIdentifier())){

                // both have a text
                if (o1.getValue() != null && o2.getValue() != null){
                    return o1.getValue().compareTo(o2.getValue());
                }
                else if (o1.getValue() == null && o2.getValue() != null){
                    return AFTER;
                }
                else if (o1.getValue()!= null && o2.getValue() == null){
                    return BEFORE;
                }
                else {
                    return EQUAL;
                }
            }
            // topics are different, we sort first by topics
            else {
                return o1.getTopic().getMIIdentifier().compareTo(o2.getTopic().getMIIdentifier());
            }
        }
        else if (o1.getTopic().getMIIdentifier() == null && o2.getTopic().getMIIdentifier() != null){
            return AFTER;
        }
        else if (o1.getTopic().getMIIdentifier() != null && o2.getTopic().getMIIdentifier()== null){
            return BEFORE;
        }
        else {

            // both have a text
            if (o1.getValue() != null && o2.getValue() != null){
                return o1.getValue().compareTo(o2.getValue());
            }
            else if (o1.getValue() == null && o2.getValue() != null){
                return AFTER;
            }
            else if (o1.getValue()!= null && o2.getValue() == null){
                return BEFORE;
            }
            else {
                return EQUAL;
            }
        }
    }
}
