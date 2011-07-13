package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.Annotation;

import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>12/07/11</pre>
 */

public class AnnotationUpdateReport {

    private String range;
    private Collection<Annotation> addedAnnotations = new ArrayList<Annotation>();
    private Collection<Annotation> removedAnnotations = new ArrayList<Annotation>();

    public AnnotationUpdateReport(String range){
        this.range = range;
    }

    public String getRange() {
        return range;
    }

    public Collection<Annotation> getAddedAnnotations() {
        return addedAnnotations;
    }

    public Collection<Annotation> getRemovedAnnotations() {
        return removedAnnotations;
    }

    public static String annotationsToString(Collection<Annotation> annotations) {
        StringBuilder sb = new StringBuilder();
        int i = 1;
        for (Annotation a : annotations) {

            if (i < annotations.size()) {
                sb.append(", ");
            }

            String qual = (a.getCvTopic() != null)? "("+a.getCvTopic().getShortLabel()+")" : "";

            sb.append(qual+":"+ (a.getAnnotationText() != null ? a.getAnnotationText() : ""));

            i++;
        }

        return sb.toString();
    }

}
