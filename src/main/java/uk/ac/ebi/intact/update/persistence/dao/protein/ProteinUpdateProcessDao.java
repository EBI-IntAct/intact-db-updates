package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.persistence.dao.UpdateProcessDao;

import java.io.Serializable;
import java.util.List;

/**
 * This interface allows to query the database to get Update processes
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15/03/11</pre>
 */
@Mockable
public interface ProteinUpdateProcessDao extends UpdateProcessDao<ProteinUpdateProcess>, Serializable {

    public List<ProteinUpdateProcess> getUpdateProcessHavingErrors();
    public List<ProteinUpdateProcess> getUpdateProcessWithoutErrors();
}
