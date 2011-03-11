package uk.ac.ebi.intact.update.persistence.proteinmapping;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;

import java.util.List;

/**
 * This interface contains methods to query the database and get specific PICRReport
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface PICRReportDao extends ActionReportDao<PICRReport>{

    /**
     *
     * @param actionId
     * @return the list of PICRReports attached to a specific result
     */
    public List<PICRReport> getPICRReportsByResultsId(long actionId);

    /**
     *
     * @param protAc
     * @return the list of PICRReports for a protein
     */
    public List<PICRReport> getActionReportsWithPICRCrossReferencesByProteinAc(String protAc);
}
