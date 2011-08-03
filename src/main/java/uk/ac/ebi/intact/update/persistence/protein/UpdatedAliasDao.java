package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.UpdatedAlias;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.util.Collection;

/**
 * Dao for Updated annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */
@Mockable
public interface UpdatedAliasDao extends UpdateBaseDao<UpdatedAlias> {
    Collection<UpdatedAlias> getDeletedAliasesFor(Long eventId);
    Collection<UpdatedAlias> getUpdatedAliasesFor(Long eventId);
    Collection<UpdatedAlias> getAddedAliasesFor(Long eventId);
}
