package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.UpdatedAnnotation;
import uk.ac.ebi.intact.update.persistence.dao.UpdateBaseDao;

import java.util.Collection;

/**
 * Dao for UpdatedAnnotation
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Mockable
public interface UpdatedAnnotationDao<T extends UpdatedAnnotation> extends UpdateBaseDao<T> {

    public Collection<T> getDeletedAnnotationFor(Long eventId);
    public Collection<T> getUpdatedAnnotationFor(Long eventId);
    public Collection<T> getAddedAnnotationFor(Long eventId);
}
