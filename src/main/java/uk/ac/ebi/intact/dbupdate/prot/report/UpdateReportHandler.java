/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot.report;

import java.io.Closeable;
import java.io.IOException;

/**
 * Defines the ReportWriters available to a protein update process.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface UpdateReportHandler extends Closeable {

    ReportWriter getDuplicatedWriter() throws IOException;
    ReportWriter getDeletedWriter() throws IOException;
    ReportWriter getCreatedWriter() throws IOException;
    ReportWriter getNonUniprotProteinWriter() throws IOException;
    ReportWriter getUpdateCasesWriter() throws IOException;
    ReportWriter getSequenceChangedWriter() throws IOException;
    ReportWriter getRangeChangedWriter() throws IOException;
    ReportWriter getInvalidRangeWriter() throws IOException;
    ReportWriter getDeadProteinWriter() throws IOException;
    ReportWriter getOutOfDateParticipantWriter() throws IOException;
    ReportWriter getPreProcessErrorWriter() throws IOException;
    ReportWriter getSecondaryProteinsWriter() throws IOException;
    ReportWriter getOutOfDateRangeWriter() throws IOException;
    ReportWriter getTranscriptWithSameSequenceWriter() throws IOException;
    ReportWriter getIntactParentWriter() throws IOException;
    ReportWriter getProteinMappingWriter() throws IOException;
    ReportWriter getSequenceChangedCautionWriter() throws IOException;
    ReportWriter getDeletedComponentWriter() throws IOException;
}
