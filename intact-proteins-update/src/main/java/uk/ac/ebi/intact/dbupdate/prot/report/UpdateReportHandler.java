package uk.ac.ebi.intact.dbupdate.prot.report;

import java.io.Closeable;

/**
 * Defines the ReportWriters available to a protein update process.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface UpdateReportHandler extends Closeable {

    ReportWriter getDuplicatedWriter();
    ReportWriter getDeletedWriter();
    ReportWriter getCreatedWriter();
    ReportWriter getNonUniprotProteinWriter();
    ReportWriter getUpdateCasesWriter();
    ReportWriter getSequenceChangedWriter();
    ReportWriter getRangeChangedWriter();
    ReportWriter getFeatureChangedWriter();
    ReportWriter getInvalidRangeWriter();
    ReportWriter getDeadProteinWriter();
    ReportWriter getOutOfDateParticipantWriter();
    ReportWriter getPreProcessErrorWriter();
    ReportWriter getSecondaryProteinsWriter();
    ReportWriter getOutOfDateRangeWriter();
    ReportWriter getTranscriptWithSameSequenceWriter();
    ReportWriter getIntactParentWriter();
    ReportWriter getProteinMappingWriter();
    ReportWriter getSequenceChangedCautionWriter();
    ReportWriter getDeletedComponentWriter();
}