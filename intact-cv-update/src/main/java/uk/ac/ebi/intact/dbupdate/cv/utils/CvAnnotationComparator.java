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

public class CvAnnotationComparator implements Comparator<Annotation> {

    private static final int BEFORE = -1;
    private static final  int EQUAL = 0;
    private static final  int AFTER = 1;

    @Override
    public int compare(Annotation o1, Annotation o2) {

        // both have a topic
        if (o1.getCvTopic() != null && o2.getCvTopic() != null) {
            // topic identical, we can sort by text
            if (o1.getCvTopic().getIdentifier().equalsIgnoreCase(o2.getCvTopic().getIdentifier())) {
                return compareByText(o1, o2);
            }
            // topics are different, we sort first by topics
            else {
                return o1.getCvTopic().getIdentifier().compareTo(o2.getCvTopic().getIdentifier());
            }
        }
        else if (o1.getCvTopic() == null && o2.getCvTopic() != null) {
            return AFTER;
        }
        else if (o1.getCvTopic() != null && o2.getCvTopic()== null) {
            return BEFORE;
        }
        else {
            return compareByText(o1, o2);
        }
    }

    private int compareByText(Annotation o1, Annotation o2) {
        // both have a text
        if (o1.getAnnotationText() != null && o2.getAnnotationText() != null) {
            // text identical, we can sort by AC
            if (o1.getAnnotationText().compareTo(o2.getAnnotationText()) == EQUAL) {
                return compareByAc(o1, o2);
            } else {
                return o1.getAnnotationText().compareTo(o2.getAnnotationText());
            }
        }
        else if (o1.getAnnotationText() == null && o2.getAnnotationText() != null) {
            return AFTER;
        }
        else if (o1.getAnnotationText()!= null && o2.getAnnotationText() == null) {
            return BEFORE;
        }
        else {
            return compareByAc(o1, o2);
        }
    }

    private int compareByAc(Annotation o1, Annotation o2) {
        // both have AC
        if (o1.getAc() != null && o2.getAc() != null) {
            return o1.getAc().compareTo(o2.getAc());
        }
        else if (o1.getAc() == null && o2.getAc() != null) {
            return AFTER;
        }
        else if (o1.getAc()!= null && o2.getAc() == null) {
            return BEFORE;
        }
        else {
            return EQUAL;
        }
    }
}
