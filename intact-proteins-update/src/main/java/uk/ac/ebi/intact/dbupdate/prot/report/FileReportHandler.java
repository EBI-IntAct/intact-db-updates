package uk.ac.ebi.intact.dbupdate.prot.report;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * The handler containing the possible writers for a protein update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class FileReportHandler implements UpdateReportHandler{

    private final ReportWriter duplicatesWriter;
    private final ReportWriter deletedWriter;
    private final ReportWriter createdWriter;
    private final ReportWriter nonUniprotProteinWriter;
    private final ReportWriter updateCasesWriter;
    private final ReportWriter sequenceChangedWriter;
    private final ReportWriter rangeChangedWriter;
    private final ReportWriter featureChangedWriter;
    private final ReportWriter invalidRangeWriter;
    private final ReportWriter outOfDateRangeWriter;
    private final ReportWriter deadProteinWriter;
    private final ReportWriter outOfDateParticipantWriter;
    private final ReportWriter preprocessErrorWriter;
    private final ReportWriter secondaryProteinsWriter;
    private final ReportWriter transcriptWithSameSequenceWriter;
    private final ReportWriter updatedIntactParentWriter;
    private final ReportWriter proteinMappingWriter;
    private final ReportWriter sequenceChangedCautionWriter;
    private final ReportWriter deletedComponentWriter;

    public FileReportHandler(File dirFile) throws IOException {
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.isDirectory()) {
            throw new IOException("The file passed to the constructor has to be a directory: "+dirFile);
        }

        this.duplicatesWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "duplicates.csv")));
        this.deletedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "deleted.csv")));
        this.createdWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "created.csv")));
        this.nonUniprotProteinWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "non_uniprot.csv")));
        this.updateCasesWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "update_cases.csv")));
        this.sequenceChangedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "sequence_changed.fasta")));
        this.rangeChangedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "range_changed.csv")));
        this.featureChangedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "feature_changed.csv")));
        this.invalidRangeWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "invalid_range.csv")));
        this.outOfDateRangeWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "out_of_date_range.csv")));
        this.deadProteinWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "dead_proteins.csv")));
        this.outOfDateParticipantWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "out_of_date_participants.csv")));
        this.preprocessErrorWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "process_errors.csv")));
        this.secondaryProteinsWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "secondary_proteins.csv")));
        this.transcriptWithSameSequenceWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "transcript_same_sequence.csv")));
        this.updatedIntactParentWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "updated_intact_parents.csv")));
        this.proteinMappingWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "protein_mapping.csv")));
        this.sequenceChangedCautionWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "sequence_changed_caution.csv")));
        this.deletedComponentWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "deleted_component.csv")));
    }

    public ReportWriter getDuplicatedWriter() {
        return duplicatesWriter;
    }

    public ReportWriter getDeletedWriter() {
        return deletedWriter;
    }

    public ReportWriter getCreatedWriter() {
        return createdWriter;
    }

    public ReportWriter getNonUniprotProteinWriter() {
        return nonUniprotProteinWriter;
    }

    public ReportWriter getUpdateCasesWriter() {
        return updateCasesWriter;
    }

    public ReportWriter getSequenceChangedWriter() {
        return sequenceChangedWriter;
    }

    public ReportWriter getRangeChangedWriter() {
        return rangeChangedWriter;
    }

    public ReportWriter getInvalidRangeWriter() {
        return invalidRangeWriter;
    }

    public ReportWriter getDeadProteinWriter() {
        return deadProteinWriter;
    }

    public ReportWriter getOutOfDateParticipantWriter() {
        return outOfDateParticipantWriter;
    }

    public ReportWriter getPreProcessErrorWriter() {
        return preprocessErrorWriter;
    }

    public ReportWriter getSecondaryProteinsWriter() {
        return secondaryProteinsWriter;
    }

    public ReportWriter getTranscriptWithSameSequenceWriter() {
        return transcriptWithSameSequenceWriter;
    }

    @Override
    public ReportWriter getIntactParentWriter() {
        return updatedIntactParentWriter;
    }

    @Override
    public ReportWriter getProteinMappingWriter() {
        return this.proteinMappingWriter;
    }

    @Override
    public ReportWriter getOutOfDateRangeWriter() {
        return this.outOfDateRangeWriter;
    }

    @Override
    public ReportWriter getSequenceChangedCautionWriter() {
        return this.sequenceChangedCautionWriter;
    }

    @Override
    public ReportWriter getDeletedComponentWriter() {
        return this.deletedComponentWriter;
    }

    @Override
    public ReportWriter getFeatureChangedWriter() {
        return this.featureChangedWriter;
    }

    public void close() throws IOException {
        this.duplicatesWriter.close();
        this.deletedWriter.close();
        this.createdWriter.close();
        this.nonUniprotProteinWriter.close();
        this.updateCasesWriter.close();
        this.sequenceChangedWriter.close();
        this.rangeChangedWriter.close();
        this.featureChangedWriter.close();
        this.invalidRangeWriter.close();
        this.outOfDateRangeWriter.close();
        this.deadProteinWriter.close();
        this.outOfDateParticipantWriter.close();
        this.preprocessErrorWriter.close();
        this.secondaryProteinsWriter.close();
        this.transcriptWithSameSequenceWriter.close();
        this.updatedIntactParentWriter.close();
        this.proteinMappingWriter.close();
        this.sequenceChangedCautionWriter.close();
        this.deletedComponentWriter.close();
    }
}