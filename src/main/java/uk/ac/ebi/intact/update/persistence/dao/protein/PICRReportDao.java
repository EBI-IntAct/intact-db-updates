package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;

import java.util.List;

/**
 * This interface contains methods to query the database and get specific PersistentPICRReport
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface PICRReportDao extends MappingReportDao<PersistentPICRReport> {

    /**
     *
     * @param actionId
     * @return the list of PICRReports attached to a specific result
     */
    public List<PersistentPICRReport> getPICRReportsByResultsId(long actionId);

}
