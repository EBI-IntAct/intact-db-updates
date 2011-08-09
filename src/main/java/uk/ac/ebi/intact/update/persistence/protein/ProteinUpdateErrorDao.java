package uk.ac.ebi.intact.update.persistence.protein;

import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.persistence.UpdateBaseDao;

import java.io.Serializable;

/**
 * Dao for protein update errors
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>09/08/11</pre>
 */

public interface ProteinUpdateErrorDao<T extends DefaultPersistentUpdateError> extends UpdateBaseDao<T>, Serializable {
}
