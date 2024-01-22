package uk.ac.ebi.intact.dbupdate.feature.mutation.writer;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FileReportHandler {
    private static final Log log = LogFactory.getLog(FileReportHandler.class);

    private ReportWriter rangeErrorReport;
    private ReportWriter retrieveObjectErrorReport;
    private ReportWriter sequenceErrorReport;
    private ReportWriter featureTypeErrorReport;

    private ReportWriter featureAnnotationFoundReport;
    private ReportWriter resultingSequenceChangedReport;
    private ReportWriter modifiedMutationsReport;
    private ReportWriter unmodifiedMutationsReport;
    private ReportWriter otherErrorsReport;


    public FileReportHandler(File dirFile) throws IOException {
        log.info("Create report files in: " + dirFile.getPath());
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.isDirectory()) {
            throw new IOException("The file passed to the constructor has to be a directory: " + dirFile);
        }

        this.rangeErrorReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "range_error.tsv")));
        this.retrieveObjectErrorReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "object_retrieve_error.tsv")));
        this.sequenceErrorReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "sequence_error.tsv")));
        this.featureTypeErrorReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "feature_type_error.tsv")));
        this.featureAnnotationFoundReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "annotation_found.tsv")));
        this.resultingSequenceChangedReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "resulting_sequence_changed.tsv")));
        this.modifiedMutationsReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "updated_mutations.tsv")));
        this.unmodifiedMutationsReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "not_updated_mutations.tsv")));
        this.otherErrorsReport = new ReportWriterImpl(new FileWriter(new File(dirFile, "other_errors.tsv")));
    }

    public ReportWriter getRangeErrorReport() throws IOException {
        return rangeErrorReport;
    }

    public ReportWriter getRetrieveObjectErrorReport() throws IOException {
        return retrieveObjectErrorReport;
    }

    public ReportWriter getSequenceErrorReport() throws IOException {
        return sequenceErrorReport;
    }

    public ReportWriter getObjectTypeErrorReport() throws IOException {
        return featureTypeErrorReport;
    }

    public ReportWriter getAnnotationFoundReport() throws IOException {
        return featureAnnotationFoundReport;
    }

    public ReportWriter getResultingSequenceChangedReport() throws IOException {
        return resultingSequenceChangedReport;
    }

    public ReportWriter getModifiedMutationsReport() throws IOException {
        return modifiedMutationsReport;
    }

    public ReportWriter getUnmodifiedMutationsReport() {
        return unmodifiedMutationsReport;
    }

    public ReportWriter getOtherErrorsReport() throws IOException {
        return otherErrorsReport;
    }

    //TODO investigate where should be call
    public void close() throws IOException {
        this.rangeErrorReport.close();
        this.retrieveObjectErrorReport.close();
        this.sequenceErrorReport.close();
        this.featureTypeErrorReport.close();
        this.featureAnnotationFoundReport.close();
        this.resultingSequenceChangedReport.close();
        this.modifiedMutationsReport.close();
        this.unmodifiedMutationsReport.close();
        this.otherErrorsReport.close();
    }
}
