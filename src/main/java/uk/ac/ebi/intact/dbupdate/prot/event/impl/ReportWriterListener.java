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
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.report.ReportWriter;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.DebugUtil;

import java.io.IOException;
import java.util.Iterator;
import java.util.List;

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
            writeDefaultLine(reportHandler.getPreProcessedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onProteinDuplicationFound(MultiProteinEvent evt) throws ProcessorException {
        try {
            ReportWriter duplicatedWriter = reportHandler.getDuplicatedWriter();
            duplicatedWriter.writeHeaderIfNecessary("Kept", "Duplicated ACs", "Duplicated shortLabels");
            duplicatedWriter.writeColumnValues(evt.getReferenceProtein().getAc(),
                                               DebugUtil.acList(evt.getProteins()).toString(),
                                               DebugUtil.labelList(evt.getProteins()).toString());
            reportHandler.getDuplicatedWriter().flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing protein to stream", e);
        }
    }

    @Override
    public void onProcess(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getProcessedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getDeletedWriter(), evt.getProtein(), evt.getMessage());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getDeadWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getCreatedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getNonUniprotProteinWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        try {
            ReportWriter writer = reportHandler.getUpdateCasesWriter();
            writer.writeHeaderIfNecessary("UniProt ID", "updated (AC)", "updated (labels)", "IA primary c.", "IA secondary c.", "IA primary", "IA secondary");
            String primaryId = evt.getProtein().getPrimaryAc();
            writer.writeColumnValues(primaryId, String.valueOf(evt.getPrimaryProteins().size()),
                                     DebugUtil.acList(evt.getUpdatedProteins()).toString(),
                                     DebugUtil.labelList(evt.getUpdatedProteins()).toString(),
                                     String.valueOf(evt.getSecondaryProteins().size()),
                                     DebugUtil.acList(evt.getPrimaryProteins()).toString(),
                                     DebugUtil.acList(evt.getSecondaryProteins()).toString());
            writer.flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing update case to stream", e);
        }
    }

    private void writeDefaultHeaderIfNecessary(ReportWriter writer) throws IOException {
        if (writer != null) {
            writer.writeHeaderIfNecessary("ac", "shortLabel", "primary ID", "message");
            writer.flush();
        }
    }

    private void writeDefaultLine(ReportWriter writer, Protein protein) throws IOException {
        writeDefaultLine(writer, protein, null);
    }

    private void writeDefaultLine(ReportWriter writer, Protein protein, String message) throws IOException {
        writeDefaultHeaderIfNecessary(writer);
        if (writer != null) {
            String primaryId = getPrimaryIdString(protein);
            message = (message != null)? message : "-";

            writer.writeColumnValues(protein.getAc(), protein.getShortLabel(), primaryId, message);
            writer.flush();
        }
    }

    private String getPrimaryIdString(Protein protein) {
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);

        String primaryId = null;

        if (uniprotXref != null) {
            primaryId = uniprotXref.getPrimaryId();
        } else {
            StringBuilder sb = new StringBuilder();
            List<InteractorXref> xrefs = ProteinUtils.getIdentityXrefs(protein);

            Iterator<InteractorXref> iterator = xrefs.iterator();
            while (iterator.hasNext()) {
                InteractorXref xref =  xrefs.iterator().next();
                sb.append(xref.getCvDatabase().getShortLabel()).append(":").append(xref.getPrimaryId());
                if (iterator.hasNext()) sb.append("|");
                primaryId = sb.toString();
            }

            if (xrefs.isEmpty()) {
                primaryId = "-";
            }
        }
        return primaryId;
    }


}
