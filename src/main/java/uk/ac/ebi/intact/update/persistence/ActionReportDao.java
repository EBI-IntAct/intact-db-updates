package uk.ac.ebi.intact.update.persistence;

import uk.ac.ebi.intact.annotation.Mockable;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.proteinmapping.actions.status.StatusLabel;

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
public interface ActionReportDao<T extends ActionReport> extends UpdateBaseDao<T>, Serializable{

    /**
     *
     * @param id
     * @return the actionReport with this unique identifier in the database, null if it doesn't exist
     */
    public ActionReport getByReportId(long id);

    /**
     *
     * @param name
     * @return the list of ActionReport with a specific name
     */
    public List<ActionReport> getByActionName(ActionName name);

    /**
     *
     * @param status
     * @return the list of ActionReport with a specific status
     */
    public List<ActionReport> getByReportStatus(StatusLabel status);

    /**
     *
     * @return the list of ActionReport containing warnings
     */
    public List<ActionReport> getAllReportsWithWarnings();

    /**
     *
     * @return the list of ActionReport containing several possible uniprot accessions
     */
    public List<ActionReport> getAllReportsWithSeveralPossibleUniprot();

    /**
     *
     * @return all the PICRReports
     */
    public List<PICRReport> getAllPICRReports();

    /**
     *
     * @return all the BlastReports
     */
    public List<BlastReport> getAllBlastReports();

    /**
     *
     * @return all the swissprot remapping reports
     */
    public List<BlastReport> getAllSwissprotRemappingReports();

    /**
     *
     * @param id
     * @return the list of ActionReports containing warnings and attached to a specific UpdateResult
     */
    public List<ActionReport> getReportsWithWarningsByResultsId(long id);

    /**
     *
     * @param id
     * @return all the ActionReports attached to a specific UpdateResult
     */
    public List<ActionReport> getAllReportsByResultsId(long id);

    /**
     *
     * @param id
     * @return the list of ActionReports containing several uniprot accessions and attached to a specific UpdateResult
     */
    public List<ActionReport> getReportsWithSeveralPossibleUniprotByResultId(long id);

    /**
     * 
     * @param name
     * @param proteinAc
     * @return the list of ActionReports existing for a protein with a specific name
     */
    public List<ActionReport> getActionReportsByNameAndProteinAc(ActionName name, String proteinAc);

    /**
     *
     * @param name
     * @param resultId
     * @return the list of ActionReports attached to a result and with a specific name
     */
    public List<ActionReport> getActionReportsByNameAndResultId(ActionName name, long resultId);

    /**
     *
     * @param status
     * @param proteinAc
     * @return the list of ActionReports existing for a protein and with a specific status
     */
    public List<ActionReport> getActionReportsByStatusAndProteinAc(StatusLabel status, String proteinAc);

    /**
     *
     * @param label
     * @param resultId
     * @return the list of ActionReports attached to a result and with a specific status
     */
    public List<ActionReport> getActionReportsByStatusAndResultId(StatusLabel label, long resultId);

    /**
     *
     * @param proteinAc
     * @return the list of ActionReports containing warnings for this protein
     */
    public List<ActionReport> getActionReportsWithWarningsByProteinAc(String proteinAc);

}
