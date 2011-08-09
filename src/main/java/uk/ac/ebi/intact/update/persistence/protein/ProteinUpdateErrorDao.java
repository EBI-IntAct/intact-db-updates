package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.persistence.UpdateEventDao;

import java.io.Serializable;
import java.util.List;

/**
 * Dao for protein update errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/08/11</pre>
 */

public interface ProteinUpdateErrorDao<T extends DefaultPersistentUpdateError> extends UpdateEventDao<T>, Serializable {

    public List<T> getUpdateErrorByLabel(long processId, UpdateError label);
    public List<T> getUpdateErrorByReason(long processId, String reason);
}
