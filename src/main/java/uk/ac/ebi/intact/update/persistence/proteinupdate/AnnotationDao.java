package uk.ac.ebi.intact.update.persistence.proteinupdate;

import uk.ac.ebi.intact.update.model.protein.update.updatedAnnotation;
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

public interface AnnotationDao<T extends updatedAnnotation> extends UpdateBaseDao<T>, Serializable {

    List<updatedAnnotation> getAnnotationsByIntactAnnotationAc(String intactAnnotationAc);
    List<updatedAnnotation> getAnnotationsByAnnotationTextLike(String annotationText);
    List<updatedAnnotation> getAnnotationsByAnnotationText(String annotationText);
    List<updatedAnnotation> getAnnotationsByCvTopicAc(String cvTopicAc);
    List<updatedAnnotation> getAnnotationsByProteinId(long proteinId);
}
