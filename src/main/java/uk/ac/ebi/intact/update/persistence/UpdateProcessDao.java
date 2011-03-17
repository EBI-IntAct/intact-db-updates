package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

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

public interface UpdateProcessDao extends UpdateBaseDao<UpdateProcess>, Serializable {

    public List<UpdateProcess> getAllUpdateProcessesByDate(Date date);
    public List<UpdateProcess> getAllUpdateProcessesBeforeDate(Date date);
    public List<UpdateProcess> getAllUpdateProcessesAfterDate(Date date);
}
