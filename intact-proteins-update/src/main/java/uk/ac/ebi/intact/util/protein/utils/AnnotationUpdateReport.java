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

    private Collection<Annotation> addedAnnotations = new ArrayList<Annotation>();
    private Collection<Annotation> removedAnnotations = new ArrayList<Annotation>();
    private Collection<Annotation> updatedAnnotations = new ArrayList<Annotation>();

    public Collection<Annotation> getAddedAnnotations() {
        return addedAnnotations;
    }

    public Collection<Annotation> getRemovedAnnotations() {
        return removedAnnotations;
    }

    public Collection<Annotation> getUpdatedAnnotations() {
        return updatedAnnotations;
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
