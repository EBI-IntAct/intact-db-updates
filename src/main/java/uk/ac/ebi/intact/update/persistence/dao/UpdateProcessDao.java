package uk.ac.ebi.intact.update.persistence.dao;

import uk.ac.ebi.intact.update.model.UpdateProcess;

import java.io.Serializable;
import java.util.Date;
import java.util.List;

/**
 * This interface allows to query the database to get Update processes
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/03/11</pre>
 */

public interface UpdateProcessDao<T extends UpdateProcess> extends UpdateBaseDao<T>, Serializable {

    public List<T> getByDate(Date date);
    public List<T> getBeforeDate(Date date);
    public List<T> getAfterDate(Date date);
}
