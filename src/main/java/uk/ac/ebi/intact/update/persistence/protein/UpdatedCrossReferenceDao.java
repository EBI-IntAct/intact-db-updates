package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.util.Collection;

/**
 * Dao for updated cross references
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Mockable
public interface UpdatedCrossReferenceDao extends UpdateBaseDao<UpdatedCrossReference> {

     Collection<UpdatedCrossReference> getDeletedXrefsFor(Long eventId);
    Collection<UpdatedCrossReference> getUpdatedXrefsFor(Long eventId);
    Collection<UpdatedCrossReference> getAddedXrefsFor(Long eventId);
}
