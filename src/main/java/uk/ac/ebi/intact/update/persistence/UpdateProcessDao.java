package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import java.io.Serializable;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/03/11</pre>
 */

public interface UpdateProcessDao<T extends UpdateProcess> extends UpdateBaseDao<T>, Serializable {
}
