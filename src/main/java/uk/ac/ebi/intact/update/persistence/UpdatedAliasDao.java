package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.UpdatedAlias;

import java.util.Collection;

/**
 * Dao for Updated annotations
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19/07/11</pre>
 */

public interface UpdatedAliasDao<T extends UpdatedAlias> extends UpdateBaseDao<T>{
    Collection<T> getDeletedAliasesFor(Long eventId);
    Collection<T> getUpdatedAliasesFor(Long eventId);
    Collection<T> getAddedAliasesFor(Long eventId);
}
