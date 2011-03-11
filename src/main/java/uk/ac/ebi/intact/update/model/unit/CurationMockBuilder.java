package uk.ac.ebi.intact.update.model.unit;

import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.Status;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.BlastResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PICRCrossReferences;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateResults;

import java.util.Date;

/**
 * This class contains a set of methods to create objects for testing
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
public class CurationMockBuilder extends IntactMockBuilder {

    /**
     *
     * @return auto-generated Swissprot remapping results
     */
    public BlastResults createSwissprotRemappingResults(){
        BlastResults results = new BlastResults();

        results.setTremblAccession("Q8R3H6");
        results.setAccession("P01867");
        results.setDatabase("SP");
        results.setDescription("Ig gamma-2B chain C region");
        results.setStartQuery(140);
        results.setEndQuery(174);
        results.setStartMatch(1);
        results.setEndQuery(335);
        results.setIdentity(99);
        results.setCreated(new Date(1));

        return results;
    }

    /**
     *
     * @return auto-generated blast results
     */
    public BlastResults createBlastResults(){
        BlastResults results = new BlastResults();

        results.setAccession("Q8R3H6");
        results.setDatabase("TR");
        results.setStartQuery(140);
        results.setEndQuery(174);
        results.setStartMatch(1);
        results.setEndQuery(335);
        results.setIdentity(99);
        results.setCreated(new Date(1));

        return results;
    }

    /**
     *
     * @param trembl
     * @param swissprotAc
     * @param sartQuery
     * @param endQuery
     * @param startMatch
     * @param endMatch
     * @param identity
     * @return a Blast results instance with trembl. swissprotAc. start and end query, start and end match, identity
     */
    public BlastResults createSwissprotRemappingResults(String trembl, String swissprotAc, int sartQuery, int endQuery, int startMatch, int endMatch, float identity){
        BlastResults results = new BlastResults();

        results.setTremblAccession(trembl);
        results.setAccession(swissprotAc);
        results.setDatabase("SP");
        results.setStartQuery(sartQuery);
        results.setEndQuery(endQuery);
        results.setStartMatch(startMatch);
        results.setEndQuery(endMatch);
        results.setIdentity(identity);
        results.setCreated(new Date(1));

        return results;
    }

    /**
     *
     * @return An auto-generated BlastReport for swissprot remapping
     */
    public BlastReport createSwissprotRemappingReport(){
        BlastReport report = new BlastReport(ActionName.BLAST_Swissprot_Remapping);

        report.setASwissprotEntry(true);
        report.setStatus(new Status(StatusLabel.COMPLETED, "mapping successful"));
        report.setQuerySequence("GCAGGT");
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return an auto-generated PICRCrossReference instance
     */
    public PICRCrossReferences createPICRCrossReferences(){
        PICRCrossReferences pc = new PICRCrossReferences();

        pc.setDatabase("Ensembl");
        pc.addAccession("ENSG0007777");
        pc.setCreated(new Date(1));

        return pc;
    }

    /**
     *
     * @return auto-generated PICRReport
     */
    public PICRReport createPICRReport(){
        PICRReport report = new PICRReport(ActionName.PICR_accession);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.COMPLETED, null));
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return auto-generated ActionReport containing warnings
     */
    public ActionReport createActionReportWithWarning(){
        ActionReport report = new ActionReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");
        report.setCreated(new Date(1));

        return report;
    }

    /**
     *
     * @return auto-generated ActionReport without any warnings
     */
    public ActionReport createActionReportWithoutWarning(){
        ActionReport report = new ActionReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");
        report.setCreated(new Date(1));

        return report;
    }

    /**
     *
     * @return auto-generated ActionReport without any possible uniprot ac
     */
    public ActionReport createActionReportWithoutPossibleUniprot(){
        ActionReport report = new ActionReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");
        report.setCreated(new Date(1));

        return report;
    }

    /**
     *
     * @return auto-generated BlastReport
     */
    public BlastReport createBlastReport(){
         BlastReport report = new BlastReport(ActionName.BLAST_uniprot);

        report.setASwissprotEntry(true);
        report.setQuerySequence("GCAGGT");        
        report.setStatus(new Status(StatusLabel.COMPLETED, "mapping successful"));
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return auto-generated updateResult
     */
    public UpdateResults createUpdateResult(){
         UpdateResults results = new UpdateResults();

        results.setIntactAccession("EBI-0001001");
        results.setFinalUniprotId("P01234");
        results.setCreated(new Date(1));
        return results;
    }

    /**
     *
     * @return auto-generated update result without a final uniprot ac
     */
    public UpdateResults createUnsuccessfulUpdateResult(){
         UpdateResults results = new UpdateResults();

        results.setIntactAccession("EBI-0001002");
        results.setCreated(new Date(1));
        return results;
    }

    /**
     *
     * @return auto-generated action report for a protein without any sequences and without any identity cross references
     */
    public ActionReport createUpdateReportWithNoSequenceNoIdentityXRef(){
         ActionReport report = new ActionReport(ActionName.update_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is neither a sequence nor an identity xref"));
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return auto-generated action report with a conflict during the update
     */
    public ActionReport createUpdateReportWithConflict(){
         ActionReport report = new ActionReport(ActionName.update_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, "There is a conflict"));
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return auto-generated ActionReport containing feature range conflicts
     */
    public ActionReport createFeatureRangeCheckingReportWithConflict(){
         ActionReport report = new ActionReport(ActionName.feature_range_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is a conflict"));
        report.setCreated(new Date(1));
        return report;
    }

    /**
     *
     * @return auto-generated ActionReport with a status FAILED
     */
    public ActionReport createReportWithStatusFailed(){
         ActionReport report = new ActionReport(ActionName.PICR_accession);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "PICR couldn't match the accession to any Uniprot entries"));
        report.setCreated(new Date(1));
        return report;
    }
}
