package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.protein.Annotation;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.io.Serializable;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Dec-2010</pre>
 */

public interface AnnotationDao<T extends Annotation> extends UpdateBaseDao<T>, Serializable {

    List<Annotation> getAnnotationsByIntactAnnotationAc(String intactAnnotationAc);
    List<Annotation> getAnnotationsByAnnotationTextLike(String annotationText);
    List<Annotation> getAnnotationsByAnnotationText(String annotationText);
    List<Annotation> getAnnotationsByCvTopicAc(String cvTopicAc);
    List<Annotation> getAnnotationsByProteinId(long proteinId);
}
