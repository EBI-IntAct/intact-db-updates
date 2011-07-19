package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.UpdatedAnnotation;

import java.util.Collection;

/**
 * Dao for UpdatedAnnotation
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Mockable
public interface UpdatedAnnotationDao<T extends UpdatedAnnotation> extends UpdateBaseDao<T>{

    Collection<T> getDeletedAnnotationFor(Long eventId);
    Collection<T> getUpdatedAnnotationFor(Long eventId);
    Collection<T> getAddedAnnotationFor(Long eventId);
}
