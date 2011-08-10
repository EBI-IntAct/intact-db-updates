package uk.ac.ebi.intact.update.persistence.dao.protein;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.persistence.dao.UpdateBaseDao;

import java.io.Serializable;
import java.util.List;

/**
 * This interface allows to query the database to get ActionReports
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-May-2010</pre>
 */
@Mockable
public interface MappingReportDao<T extends PersistentMappingReport> extends UpdateBaseDao<T>, Serializable{

    /**
     *
     * @param name
     * @return the list of PersistentMappingReport with a specific name
     */
    public List<T> getByActionName(ActionName name);

    /**
     *
     * @param status
     * @return the list of PersistentMappingReport with a specific status
     */
    public List<T> getByReportStatus(StatusLabel status);

    /**
     *
     * @return the list of PersistentMappingReport containing warnings
     */
    public List<T> getAllReportsWithWarnings();

    /**
     *
     * @return the list of PersistentMappingReport containing several possible uniprot accessions
     */
    public List<T> getAllReportsWithSeveralPossibleUniprot();

    /**
     *
     * @param id
     * @return the list of ActionReports containing warnings and attached to a specific UpdateResult
     */
    public List<T> getReportsWithWarningsByResultsId(long id);

    /**
     *
     * @param id
     * @return all the ActionReports attached to a specific UpdateResult
     */
    public List<T> getAllReportsByResultsId(long id);

    /**
     *
     * @param id
     * @return the list of ActionReports containing several uniprot accessions and attached to a specific UpdateResult
     */
    public List<T> getReportsWithSeveralPossibleUniprotByResultId(long id);

    /**
     *
     * @param name
     * @param resultId
     * @return the list of ActionReports attached to a result and with a specific name
     */
    public List<T> getActionReportsByNameAndResultId(ActionName name, long resultId);

    /**
     *
     * @param label
     * @param resultId
     * @return the list of ActionReports attached to a result and with a specific status
     */
    public List<T> getActionReportsByStatusAndResultId(StatusLabel label, long resultId);
}
