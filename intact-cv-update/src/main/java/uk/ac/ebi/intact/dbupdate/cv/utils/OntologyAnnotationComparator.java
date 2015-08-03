package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.bridges.ontology_manager.TermAnnotation;

import java.util.Comparator;

/**
 * Comparator for ontology annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class OntologyAnnotationComparator implements Comparator<TermAnnotation>{
    @Override
    public int compare(TermAnnotation o1, TermAnnotation o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // both have a topic
        if (o1.getTopicId() != null && o2.getTopicId() != null){
            // topic identical, we can sort by text
            if (o1.getTopicId().equalsIgnoreCase(o2.getTopicId())){

                // both have a text
                if (o1.getDescription() != null && o2.getDescription() != null){
                    return o1.getDescription().compareTo(o2.getDescription());
                }
                else if (o1.getDescription() == null && o2.getDescription() != null){
                    return AFTER;
                }
                else if (o1.getDescription()!= null && o2.getDescription() == null){
                    return BEFORE;
                }
                else {
                    return EQUAL;
                }
            }
            // topics are different, we sort first by topics
            else {
                return o1.getTopicId().compareTo(o2.getTopicId());
            }
        }
        else if (o1.getTopicId() == null && o2.getTopicId() != null){
            return AFTER;
        }
        else if (o1.getTopicId() != null && o2.getTopicId()== null){
            return BEFORE;
        }
        else {

            // both have a text
            if (o1.getDescription() != null && o2.getDescription() != null){
                return o1.getDescription().compareTo(o2.getDescription());
            }
            else if (o1.getDescription() == null && o2.getDescription() != null){
                return AFTER;
            }
            else if (o1.getDescription()!= null && o2.getDescription() == null){
                return BEFORE;
            }
            else {
                return EQUAL;
            }
        }
    }
}
