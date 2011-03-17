package uk.ac.ebi.intact.update.model.unit;

import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.MappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.BlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.Status;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.protein.mapping.results.BlastResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.UpdateMappingResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PICRCrossReferences;

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
        return report;
    }

    /**
     *
     * @return auto-generated MappingReport containing warnings
     */
    public MappingReport createActionReportWithWarning(){
        MappingReport report = new MappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");

        return report;
    }

    /**
     *
     * @return auto-generated MappingReport without any warnings
     */
    public MappingReport createActionReportWithoutWarning(){
        MappingReport report = new MappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");

        return report;
    }

    /**
     *
     * @return auto-generated MappingReport without any possible uniprot ac
     */
    public MappingReport createActionReportWithoutPossibleUniprot(){
        MappingReport report = new MappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");

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
        return report;
    }

    /**
     *
     * @return auto-generated updateResult
     */
    public UpdateMappingResults createUpdateResult(){
         UpdateMappingResults results = new UpdateMappingResults();

        results.setIntactAccession("EBI-0001001");
        results.setFinalUniprotId("P01234");
        return results;
    }

    /**
     *
     * @return auto-generated update result without a final uniprot ac
     */
    public UpdateMappingResults createUnsuccessfulUpdateResult(){
         UpdateMappingResults results = new UpdateMappingResults();

        results.setIntactAccession("EBI-0001002");
        return results;
    }

    /**
     *
     * @return auto-generated action report for a protein without any sequences and without any identity cross references
     */
    public MappingReport createUpdateReportWithNoSequenceNoIdentityXRef(){
         MappingReport report = new MappingReport(ActionName.update_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is neither a sequence nor an identity xref"));
        return report;
    }

    /**
     *
     * @return auto-generated action report with a conflict during the update
     */
    public MappingReport createUpdateReportWithConflict(){
         MappingReport report = new MappingReport(ActionName.update_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, "There is a conflict"));
        return report;
    }

    /**
     *
     * @return auto-generated MappingReport containing feature range conflicts
     */
    public MappingReport createFeatureRangeCheckingReportWithConflict(){
         MappingReport report = new MappingReport(ActionName.feature_range_checking);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is a conflict"));
        return report;
    }

    /**
     *
     * @return auto-generated MappingReport with a status FAILED
     */
    public MappingReport createReportWithStatusFailed(){
         MappingReport report = new MappingReport(ActionName.PICR_accession);

        report.setASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "PICR couldn't match the accession to any Uniprot entries"));
        return report;
    }
}
