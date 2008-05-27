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
package uk.ac.ebi.intact.dbupdate.prot.event.impl;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.MultiProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.DebugUtil;

import java.io.IOException;
import java.io.Writer;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ReportWriterListener extends AbstractProteinUpdateProcessorListener {


    private static final String NEW_LINE = System.getProperty("line.separator");

    private UpdateReportHandler reportHandler;


    public ReportWriterListener(UpdateReportHandler reportHandler) {
        this.reportHandler = reportHandler;
    }

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        try {
            writeLine(reportHandler.getPreProcessedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onProteinDuplicationFound(MultiProteinEvent evt) throws ProcessorException {
        try {
            Writer duplicatedWriter = reportHandler.getDuplicatedWriter();
            duplicatedWriter.write((DebugUtil.acList(evt.getProteins())+ NEW_LINE));
            reportHandler.getDuplicatedWriter().flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing protein to stream", e);
        }
    }

    @Override
    public void onProcess(ProteinEvent evt) throws ProcessorException {
        try {
            writeLine(reportHandler.getProcessedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        try {
            writeLine(reportHandler.getDeletedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {
        try {
            writeLine(reportHandler.getDeadWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        try {
            writeLine(reportHandler.getCreatedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    private void writeLine(Writer writer, Protein protein) throws IOException {
        if (writer != null) {
            InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
            String primaryId = (uniprotXref != null)? uniprotXref.getPrimaryId() : "-";
            writer.write((protein.getAc()+"\t"+protein.getShortLabel()+"\t"+primaryId+NEW_LINE));
            writer.flush();
        }
    }


}
