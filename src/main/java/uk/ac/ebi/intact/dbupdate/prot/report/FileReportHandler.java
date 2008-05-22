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
import java.io.Writer;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class FileReportHandler implements UpdateReportHandler{

    private Writer preProcessedWriter;
    private Writer processedWriter;
    private Writer duplicatesWriter;
    private Writer deletedWriter;
    private Writer deadWriter;
    private Writer createdWriter;


    public FileReportHandler(File dirFile) throws IOException {
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.isDirectory()) {
            throw new IOException("The file passed to the constructor has to be a directory: "+dirFile);
        }

        this.preProcessedWriter = new FileWriter(new File(dirFile, "pre_processed.csv"));
        this.processedWriter = new FileWriter(new File(dirFile, "processed.csv"));
        this.duplicatesWriter = new FileWriter(new File(dirFile, "duplicates.txt"));
        this.deletedWriter = new FileWriter(new File(dirFile, "deleted.csv"));
        this.deadWriter = new FileWriter(new File(dirFile, "dead.csv"));
        this.createdWriter = new FileWriter(new File(dirFile, "created.csv"));
    }

    public Writer getPreProcessedWriter() throws IOException {
        return preProcessedWriter;
    }

    public Writer getProcessedWriter() throws IOException {
        return processedWriter;
    }

    public Writer getDuplicatedWriter() throws IOException {
        return duplicatesWriter;
    }

    public Writer getDeletedWriter() throws IOException {
        return deletedWriter;
    }

    public Writer getDeadWriter() throws IOException {
        return deadWriter;
    }

    public Writer getCreatedWriter() throws IOException {
        return createdWriter;
    }

    public void close() throws IOException {
         this.preProcessedWriter.close();
        this.processedWriter.close();
        this.duplicatesWriter.close();
        this.deletedWriter.close();
        this.deadWriter.close();
        this.createdWriter.close();
    }
}
