package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;

import java.util.List;

/**
 * This class contains methods to query the database and get specific BlastReports
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface BlastReportDao extends MappingReportDao<BlastReport> {

    /**
     *
     * @param id
     * @return The list of BlastReports attached to a specific result
     */
    public List<BlastReport> getBlastReportsByResultsId(long id);

    /**
     *
     * @param id
     * @return the list of BlastReports containing swissprot remapping information for a specific update result
     */
    public List<BlastReport> getActionReportsWithSwissprotRemappingResultsByResultsId(long id);

}
