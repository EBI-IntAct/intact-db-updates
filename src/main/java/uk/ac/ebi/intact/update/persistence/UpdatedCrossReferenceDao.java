package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.UpdatedCrossReference;

import java.util.Collection;

/**
 * Dao for updated cross references
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */

public interface UpdatedCrossReferenceDao<T extends UpdatedCrossReference> extends UpdateBaseDao<T> {

     Collection<T> getDeletedXrefsFor(Long eventId);
    Collection<T> getUpdatedXrefsFor(Long eventId);
    Collection<T> getAddedXrefsFor(Long eventId);
}
