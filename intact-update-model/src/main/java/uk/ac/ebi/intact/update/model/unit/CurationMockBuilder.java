package uk.ac.ebi.intact.update.model.unit;

import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.protein.mapping.actions.ActionName;
import uk.ac.ebi.intact.protein.mapping.actions.status.Status;
import uk.ac.ebi.intact.protein.mapping.actions.status.StatusLabel;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateAnnotation;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.UpdatedAlias;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;
import uk.ac.ebi.intact.update.model.protein.errors.DeadUniprotAc;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.model.protein.errors.ImpossibleParentToReview;
import uk.ac.ebi.intact.update.model.protein.events.DeadProteinEvent;
import uk.ac.ebi.intact.update.model.protein.events.OutOfDateParticipantEvent;
import uk.ac.ebi.intact.update.model.protein.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentBlastReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentMappingReport;
import uk.ac.ebi.intact.update.model.protein.mapping.actions.PersistentPICRReport;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentBlastResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentPICRCrossReferences;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;

import java.util.Date;

/**
 * This class contains a set of methods to create objects for testing
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
public class CurationMockBuilder extends IntactMockBuilder {

    private final static String invalidRangeAc = "EBI-2907496";
    private final static String outOfDateRangeAc = "EBI-3058809";

    /**
     *
     * @return auto-generated Swissprot remapping results
     */
    public PersistentBlastResults createSwissprotRemappingResults(){
        PersistentBlastResults results = new PersistentBlastResults();

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
    public PersistentBlastResults createBlastResults(){
        PersistentBlastResults results = new PersistentBlastResults();

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
    public PersistentBlastResults createSwissprotRemappingResults(String trembl, String swissprotAc, int sartQuery, int endQuery, int startMatch, int endMatch, float identity){
        PersistentBlastResults results = new PersistentBlastResults();

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
     * @return An auto-generated PersistentBlastReport for swissprot remapping
     */
    public PersistentBlastReport createSwissprotRemappingReport(){
        PersistentBlastReport report = new PersistentBlastReport(ActionName.BLAST_Swissprot_Remapping);

        report.setIsASwissprotEntry(true);
        report.setStatus(new Status(StatusLabel.COMPLETED, "mapping successful"));
        report.setQuerySequence("GCAGGT");
        return report;
    }

    /**
     *
     * @return an auto-generated PICRCrossReference instance
     */
    public PersistentPICRCrossReferences createPICRCrossReferences(){
        PersistentPICRCrossReferences pc = new PersistentPICRCrossReferences();

        pc.setDatabase("Ensembl");
        pc.addAccession("ENSG0007777");

        return pc;
    }

    /**
     *
     * @return auto-generated PersistentPICRReport
     */
    public PersistentPICRReport createPICRReport(){
        PersistentPICRReport report = new PersistentPICRReport(ActionName.PICR_accession);

        report.setIsASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.COMPLETED, null));
        return report;
    }

    /**
     *
     * @return auto-generated PersistentMappingReport containing warnings
     */
    public PersistentMappingReport createActionReportWithWarning(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setIsASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");

        return report;
    }

    /**
     *
     * @return auto-generated PersistentMappingReport without any warnings
     */
    public PersistentMappingReport createActionReportWithoutWarning(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setIsASwissprotEntry(false);
        report.addPossibleAccession("P02134");
        report.addPossibleAccession("P12345");

        return report;
    }

    /**
     *
     * @return auto-generated PersistentMappingReport without any possible uniprot ac
     */
    public PersistentMappingReport createActionReportWithoutPossibleUniprot(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.BLAST_uniprot);

        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, null));
        report.setIsASwissprotEntry(false);
        report.addWarning("To be reviewed by a curator");

        return report;
    }

    /**
     *
     * @return auto-generated PersistentBlastReport
     */
    public PersistentBlastReport createBlastReport(){
        PersistentBlastReport report = new PersistentBlastReport(ActionName.BLAST_uniprot);

        report.setIsASwissprotEntry(true);
        report.setQuerySequence("GCAGGT");
        report.setStatus(new Status(StatusLabel.COMPLETED, "mapping successful"));
        return report;
    }

    /**
     *
     * @return auto-generated updateResult
     */
    public PersistentIdentificationResults createUpdateResult(){
        PersistentIdentificationResults results = new PersistentIdentificationResults();

        results.setFinalUniprotId("P01234");
        return results;
    }

    /**
     *
     * @return auto-generated update result without a final uniprot ac
     */
    public PersistentIdentificationResults createUnsuccessfulUpdateResult(){
        PersistentIdentificationResults results = new PersistentIdentificationResults();

        return results;
    }

    /**
     *
     * @return auto-generated action report for a protein without any sequences and without any identity cross references
     */
    public PersistentMappingReport createUpdateReportWithNoSequenceNoIdentityXRef(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.update_checking);

        report.setIsASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is neither a sequence nor an identity xref"));
        return report;
    }

    /**
     *
     * @return auto-generated action report with a conflict during the update
     */
    public PersistentMappingReport createUpdateReportWithConflict(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.update_checking);

        report.setIsASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.TO_BE_REVIEWED, "There is a conflict"));
        return report;
    }

    /**
     *
     * @return auto-generated PersistentMappingReport containing feature range conflicts
     */
    public PersistentMappingReport createFeatureRangeCheckingReportWithConflict(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.feature_range_checking);

        report.setIsASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "There is a conflict"));
        return report;
    }

    /**
     *
     * @return auto-generated PersistentMappingReport with a status FAILED
     */
    public PersistentMappingReport createReportWithStatusFailed(){
        PersistentMappingReport report = new PersistentMappingReport(ActionName.PICR_accession);

        report.setIsASwissprotEntry(false);
        report.setStatus(new Status(StatusLabel.FAILED, "PICR couldn't match the accession to any Uniprot entries"));
        return report;
    }

    public ProteinUpdateAnnotation createInvalidRangeAnnotation(){
        ProteinUpdateAnnotation feature = new ProteinUpdateAnnotation();

        feature.setTopic(invalidRangeAc);
        feature.setText("2..3-7..8");
        feature.setStatus(UpdateStatus.added);

        return feature;
    }

    public ProteinUpdateAnnotation createOutOfDateRangeAnnotation(){
        ProteinUpdateAnnotation feature = new ProteinUpdateAnnotation();

        feature.setTopic(outOfDateRangeAc);
        feature.setText("2-8");
        feature.setStatus(UpdateStatus.added);

        return feature;
    }

    public ProteinUpdateProcess createUpdateProcess(){
        ProteinUpdateProcess process = new ProteinUpdateProcess();

        process.setDate(new Date(System.currentTimeMillis()));

        return process;
    }

    public PersistentInvalidRange createInvalidRange(){

        PersistentInvalidRange invalid = new PersistentInvalidRange();

        invalid.setRangeAc("EBI-xxxxx1");
        invalid.setComponentAc("EBI-xxxxx2");
        invalid.setFeatureAc("EBI-xxxxx5");
        invalid.setInteractionAc("EBI-xxxxx6");
        invalid.setFromStatus("EBI-xxxxxx3");
        invalid.setToStatus("EBI-xxxxxx4");

        invalid.setOldSequence("MAAM");
        invalid.setNewSequence("SSPP");
        invalid.setOldPositions("2..3-7..8");
        invalid.setNewSequence("0-0");
        invalid.setSequenceVersion(-1);

        invalid.setErrorMessage("certain position cannot be a range");


        return invalid;
    }

    public PersistentUpdatedRange createUpdatedRange(){

        PersistentUpdatedRange updated = new PersistentUpdatedRange();

        updated.setRangeAc("EBI-xxxxx1");
        updated.setComponentAc("EBI-xxxxx2");
        updated.setFeatureAc("EBI-xxxxx5");
        updated.setInteractionAc("EBI-xxxxx6");

        updated.setOldSequence("MAAM");
        updated.setNewSequence("MAAM");
        updated.setOldPositions("2-8");
        updated.setNewSequence("4-10");

        return updated;
    }

    public PersistentInvalidRange createOutOfDateRangeWithSequenceVersion(){

        PersistentInvalidRange invalid = new PersistentInvalidRange();

        invalid.setRangeAc("EBI-xxxxx1");
        invalid.setComponentAc("EBI-xxxxx2");
        invalid.setFeatureAc("EBI-xxxxx5");
        invalid.setInteractionAc("EBI-xxxxx6");
        invalid.setFromStatus("EBI-xxxxxx3");
        invalid.setToStatus("EBI-xxxxxx4");

        invalid.setOldSequence("MAAM");
        invalid.setNewSequence("SSPP");
        invalid.setOldPositions("2-8");
        invalid.setNewSequence("0-0");
        invalid.setSequenceVersion(2);

        invalid.setErrorMessage("Out of date range");

        return invalid;
    }

    public ProteinUpdateAnnotation createDefaultUpdatedAnnotation(){
        ProteinUpdateAnnotation ann = new ProteinUpdateAnnotation();

        ann.setStatus(UpdateStatus.added);
        ann.setTopic("EBI-xxxxx6");
        ann.setText("bla");

        return ann;
    }

    public UpdatedCrossReference createDefaultUpdatedCrossReference(){
        UpdatedCrossReference ref = new UpdatedCrossReference();

        ref.setStatus(UpdateStatus.added);
        ref.setDatabase("EBI-xxxxx7");
        ref.setIdentifier("blax");
        ref.setQualifier("EBI-xxxxx8");

        return ref;
    }

    public UpdatedAlias createDefaultUpdatedAlias(){
        UpdatedAlias al = new UpdatedAlias();

        al.setStatus(UpdateStatus.added);
        al.setName("GP2");
        al.setType("EBI-xxxxx9");

        return al;
    }

    public UniprotUpdateEvent createDefaultProteinEvent(){
        UniprotUpdateEvent event = new UniprotUpdateEvent();

        event.setProteinAc("EBI-xx10xx10xx");

        return event;
    }

    public UniprotUpdateEvent createDefaultUniprotProteinEvent(){
        UniprotUpdateEvent event = new UniprotUpdateEvent();

        event.setProteinAc("EBI-xx10xx10xx");

        return event;
    }

    public OutOfDateParticipantEvent createOutOfDateParticipantProcess(){
        OutOfDateParticipantEvent event = new OutOfDateParticipantEvent();

        event.setProteinAc("EBI-xx10xx10xx");

        return event;
    }

    public UniprotUpdateEvent createDefaultProteinEventWithCollection(){
        UniprotUpdateEvent event = createDefaultUniprotProteinEvent();

        event.addUpdatedAnnotation(createDefaultUpdatedAnnotation());
        event.addUpdatedAlias(createDefaultUpdatedAlias());
        event.addUpdatedXRef(createDefaultUpdatedCrossReference());

        return event;
    }

    public DeadProteinEvent createDefaultDeadProteinEvent(){
        DeadProteinEvent proteinEvent = new DeadProteinEvent();

        proteinEvent.setProteinAc("EBI-xxxxx10");
        proteinEvent.setUniprotAc("P12345");

        return proteinEvent;
    }

    public DefaultPersistentUpdateError createDefaultError(){
        DeadUniprotAc dead = new DeadUniprotAc();

        dead.setProteinAc("EBI-xxxxx10");
        dead.setDeadUniprot("Pxxxxx");

        return dead;
    }

    public ImpossibleParentToReview createImpossibleParentToReviewError(){
        ImpossibleParentToReview impossible = new ImpossibleParentToReview();

        impossible.setProteinAc("EBI-xxxxx10");

        return impossible;
    }
}
