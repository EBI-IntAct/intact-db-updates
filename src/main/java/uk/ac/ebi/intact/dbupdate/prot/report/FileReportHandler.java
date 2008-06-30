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

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class FileReportHandler implements UpdateReportHandler{

    private ReportWriter preProcessedWriter;
    private ReportWriter processedWriter;
    private ReportWriter duplicatesWriter;
    private ReportWriter deletedWriter;
    private ReportWriter createdWriter;
    private ReportWriter nonUniprotProteinWriter;
    private ReportWriter updateCasesWriter;
    private ReportWriter sequenceChangedWriter;
    private ReportWriter rangeChangedWriter;


    public FileReportHandler(File dirFile) throws IOException {
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.isDirectory()) {
            throw new IOException("The file passed to the constructor has to be a directory: "+dirFile);
        }

        this.preProcessedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "pre_processed.csv")));
        this.processedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "processed.csv")));
        this.duplicatesWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "duplicates.csv")));
        this.deletedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "deleted.csv")));
        this.createdWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "created.csv")));
        this.nonUniprotProteinWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "non_uniprot.csv")));
        this.updateCasesWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "update_cases.csv")));
        this.sequenceChangedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "sequence_changed.fasta")));
        this.rangeChangedWriter = new ReportWriterImpl(new FileWriter(new File(dirFile, "range_changed.csv")));
    }

    public ReportWriter getPreProcessedWriter() throws IOException {
        return preProcessedWriter;
    }

    public ReportWriter getProcessedWriter() throws IOException {
        return processedWriter;
    }

    public ReportWriter getDuplicatedWriter() throws IOException {
        return duplicatesWriter;
    }

    public ReportWriter getDeletedWriter() throws IOException {
        return deletedWriter;
    }

    public ReportWriter getCreatedWriter() throws IOException {
        return createdWriter;
    }

    public ReportWriter getNonUniprotProteinWriter() {
        return nonUniprotProteinWriter;
    }

    public ReportWriter getUpdateCasesWriter() throws IOException {
        return updateCasesWriter;
    }

    public ReportWriter getSequenceChangedWriter() throws IOException {
        return sequenceChangedWriter;
    }

    public ReportWriter getRangeChangedWriter() {
        return rangeChangedWriter;
    }

    public void close() throws IOException {
        this.preProcessedWriter.close();
        this.processedWriter.close();
        this.duplicatesWriter.close();
        this.deletedWriter.close();
        this.createdWriter.close();
        this.nonUniprotProteinWriter.close();
        this.sequenceChangedWriter.close();
        this.rangeChangedWriter.close();
    }
}
