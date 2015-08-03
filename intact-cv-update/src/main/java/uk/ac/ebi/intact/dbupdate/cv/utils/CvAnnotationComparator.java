package uk.ac.ebi.intact.dbupdate.cv.utils;

import uk.ac.ebi.intact.model.Annotation;

import java.util.Comparator;

/**
 * Comparator for CvAnnotation
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/11/11</pre>
 */

public class CvAnnotationComparator implements Comparator<Annotation>{
    @Override
    public int compare(Annotation o1, Annotation o2) {
        final int BEFORE = -1;
        final int EQUAL = 0;
        final int AFTER = 1;

        // both have a topic
        if (o1.getCvTopic() != null && o2.getCvTopic() != null){
            // topic identical, we can sort by text
            if (o1.getCvTopic().getIdentifier().equalsIgnoreCase(o2.getCvTopic().getIdentifier())){

                // both have a text
                if (o1.getAnnotationText() != null && o2.getAnnotationText() != null){
                    return o1.getAnnotationText().compareTo(o2.getAnnotationText());
                }
                else if (o1.getAnnotationText() == null && o2.getAnnotationText() != null){
                    return AFTER;
                }
                else if (o1.getAnnotationText()!= null && o2.getAnnotationText() == null){
                    return BEFORE;
                }
                else {
                    return EQUAL;
                }
            }
            // topics are different, we sort first by topics
            else {
                return o1.getCvTopic().getIdentifier().compareTo(o2.getCvTopic().getIdentifier());
            }
        }
        else if (o1.getCvTopic() == null && o2.getCvTopic() != null){
            return AFTER;
        }
        else if (o1.getCvTopic() != null && o2.getCvTopic()== null){
            return BEFORE;
        }
        else {

            // both have a text
            if (o1.getAnnotationText() != null && o2.getAnnotationText() != null){
                return o1.getAnnotationText().compareTo(o2.getAnnotationText());
            }
            else if (o1.getAnnotationText() == null && o2.getAnnotationText() != null){
                return AFTER;
            }
            else if (o1.getAnnotationText()!= null && o2.getAnnotationText() == null){
                return BEFORE;
            }
            else {
                return EQUAL;
            }
        }
    }
}
