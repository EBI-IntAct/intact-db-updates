package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentUniprotProteinAPIReport;

import java.util.List;

/**
 * This interface contains methods to query the database and get specific PersistentUniprotProteinAPIReport
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@Mockable
public interface UniprotProteinAPIReportDao extends MappingReportDao<PersistentUniprotProteinAPIReport> {

    /**
     *
     * @param actionId
     * @return the list of UniprotProteinAPIReports attached to a specific result
     */
    public List<PersistentUniprotProteinAPIReport> getUniprotProteinAPIReportsByResultsId(long actionId);

}
