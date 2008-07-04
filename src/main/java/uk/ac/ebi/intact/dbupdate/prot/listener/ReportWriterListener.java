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
package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.lang.StringUtils;
import org.joda.time.DateTime;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.dbupdate.prot.report.ReportWriter;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.dbupdate.prot.util.AdditionalInfoMap;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;

import java.io.IOException;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ReportWriterListener extends AbstractProteinUpdateProcessorListener {

    private static final String EMPTY_VALUE = "-";
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
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {
        try {
            ReportWriter duplicatedWriter = reportHandler.getDuplicatedWriter();
            duplicatedWriter.writeHeaderIfNecessary("Kept", "Active instances", "Duplicates");
            duplicatedWriter.writeColumnValues(evt.getReferenceProtein().getAc(),
                                               String.valueOf(evt.getReferenceProtein().getActiveInstances().size()),
                                               protCollectionToString(evt.getProteins(), false, evt.getOriginalActiveInstancesCount()));
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
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        try {
            writeDefaultLine(reportHandler.getCreatedWriter(), evt.getProtein());
        } catch (IOException e) {
            throw new ProcessorException(e);
        }
    }

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        final Protein protein = evt.getProtein();
        try {
            final ReportWriter writer = reportHandler.getSequenceChangedWriter();

            if (evt.getOldSequence() != null) {
                writer.writeLine(">"+ protein.getAc()+"|OLD|"+
                                 protein.getShortLabel()+"|"+
                                 getPrimaryIdString(protein)
                                 +"|CRC:"+Crc64.getCrc64(evt.getOldSequence())+
                                 "|Length:"+evt.getOldSequence().length());
                writer.writeLine(insertNewLinesIfNecessary(evt.getOldSequence(), 80));
            }

            String state;
            int seqDiff;
            int levenshtein;

            if (evt.getOldSequence() != null) {
                state = "UPDATE";
                seqDiff = protein.getSequence().length()-evt.getOldSequence().length();
                levenshtein = StringUtils.getLevenshteinDistance(protein.getSequence(), evt.getOldSequence());
            } else {
                state = "NEW";
                seqDiff = protein.getSequence().length();
                levenshtein = seqDiff;
            }

            writer.writeLine(">"+ protein.getAc()+"|"+state+"|"+
                             protein.getShortLabel()+"|"+
                             getPrimaryIdString(protein)+
                             "|CRC:"+protein.getCrc64()+
                             "|Length:"+protein.getSequence().length()+
                             "|Diff:"+seqDiff+
                             "|Levenshtein:"+ levenshtein);
            writer.writeLine(insertNewLinesIfNecessary(protein.getSequence(), 80));
            writer.flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing to sequence changed writer", e);
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
            writer.writeHeaderIfNecessary("UniProt ID",
                                          "Updated prots",
                                          "IA primary c.",
                                          "IA secondary c.",
                                          "IA primary",
                                          "IA secondary",
                                          "Xrefs added",
                                          "Xrefs removed",
                                          "Error messages",
                                          "Other messages");
            String primaryId = evt.getProtein().getPrimaryAc();
            writer.writeColumnValues(primaryId,
                                     protCollectionToString(evt.getUniprotServiceResult().getProteins(), true),
                                     String.valueOf(evt.getPrimaryProteins().size()),
                                     String.valueOf(evt.getSecondaryProteins().size()),
                                     protCollectionToString(evt.getPrimaryProteins(), true),
                                     protCollectionToString(evt.getSecondaryProteins(), true),
                                     xrefReportsAddedToString(evt.getUniprotServiceResult().getXrefUpdaterReports()),
                                     xrefReportsRemovedToString(evt.getUniprotServiceResult().getXrefUpdaterReports()),
                                     evt.getUniprotServiceResult().getErrors().toString(),
                                     evt.getUniprotServiceResult().getMessages().toString());
            writer.flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing update case to stream", e);
        }
    }

    @Override
    public void onRangeChanged(RangeChangedEvent evt) throws ProcessorException {
        UpdatedRange updatedRange = evt.getUpdatedRange();
        Feature feature = updatedRange.getNewRange().getFeature();
        Component component = feature.getComponent();
        Interactor interactor = component.getInteractor();

        final InteractorXref xref = ProteinUtils.getUniprotXref(interactor);
        String uniprotAc = (xref != null)? xref.getPrimaryId() : EMPTY_VALUE;
        
        try {
            ReportWriter writer = reportHandler.getRangeChangedWriter();
            writer.writeHeaderIfNecessary("Range AC",
                                          "Old Pos.",
                                          "New Pos.",
                                          "Length Changed",
                                          "Seq. Changed",
                                          "Feature AC",
                                          "Feature Label",
                                          "Comp. AC",
                                          "Prot. AC",
                                          "Prot. Label",
                                          "Prot. Uniprot",
                                          "Message");
            writer.writeColumnValues(updatedRange.getNewRange().getAc(),
                                     updatedRange.getOldRange().toString(),
                                     updatedRange.getNewRange().toString(),
                                     booleanToYesNo(updatedRange.isRangeLengthChanged()),
                                     booleanToYesNo(updatedRange.isSequenceChanged()),
                                     feature.getAc(),
                                     feature.getShortLabel(),
                                     component.getAc(),
                                     interactor.getAc(),
                                     interactor.getShortLabel(),
                                     uniprotAc,
                                     dashIfNull(updatedRange.getMessage()));
            writer.flush();
        } catch (IOException e) {
            throw new ProcessorException("Problem writing update case to stream", e);
        }
    }

    private static String insertNewLinesIfNecessary(String oldSequence, int maxLineLength) {
        StringBuilder sb = new StringBuilder(oldSequence);

        int startIndex = oldSequence.length() - (oldSequence.length() % maxLineLength);

        if (startIndex >= maxLineLength) {
            for (int i = startIndex; i>0; i=i-maxLineLength) {
                sb.insert(i, NEW_LINE);
            }
        }

        return sb.toString();
    }

    private static String protCollectionToString(Collection<? extends Protein> protCollection,
                                                 boolean showActiveInstancesCount) {
        return protCollectionToString(protCollection, showActiveInstancesCount, null);
    }

    private static String protCollectionToString(Collection<? extends Protein> protCollection,
                                                 boolean showInteractionsCount,
                                                 AdditionalInfoMap<?> additionalInfo) {
        StringBuilder sb = new StringBuilder();

        for (Iterator<? extends Protein> iterator = protCollection.iterator(); iterator.hasNext();) {
            Protein protein = iterator.next();

            sb.append(protein.getShortLabel()).append("(").append(protein.getAc()).append(")");

            if (showInteractionsCount) {
                sb.append("[").append(protein.getActiveInstances().size()).append("]");
            }

            if (additionalInfo != null && !additionalInfo.isEmpty()) {
                sb.append("[").append(additionalInfo.get(protein.getAc())).append("]");
            }

            if (iterator.hasNext()) {
                sb.append(", ");
            }
        }

        if (protCollection.isEmpty()) {
            sb.append(EMPTY_VALUE);
        }

        return sb.toString();
    }

    private static String xrefReportsAddedToString(Collection<XrefUpdaterReport> xrefReports) {
        StringBuilder sb = new StringBuilder();

        for (Iterator<XrefUpdaterReport> iterator = xrefReports.iterator(); iterator.hasNext();) {
            XrefUpdaterReport report = iterator.next();

            sb.append(report.getProtein().getAc()).append("[").append(report.addedXrefsToString()).append("]");

            if (iterator.hasNext()) {
                sb.append("|");
            }
        }

        if (xrefReports.isEmpty()) {
            sb.append(EMPTY_VALUE);
        }

        return sb.toString();
    }

    private static String xrefReportsRemovedToString(Collection<XrefUpdaterReport> xrefReports) {
        StringBuilder sb = new StringBuilder();

        for (Iterator<XrefUpdaterReport> iterator = xrefReports.iterator(); iterator.hasNext();) {
            XrefUpdaterReport report = iterator.next();

            sb.append(report.getProtein().getAc()).append("[").append(report.removedXrefsToString()).append("]");

            if (iterator.hasNext()) {
                sb.append("|");
            }
        }

        if (xrefReports.isEmpty()) {
            sb.append(EMPTY_VALUE);
        }

        return sb.toString();
    }

    private void writeDefaultHeaderIfNecessary(ReportWriter writer) throws IOException {
        if (writer != null) {
            writer.writeHeaderIfNecessary("datetime", "ac", "shortLabel", "primary ID", "message");
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
            message = dashIfNull(message);

            writer.writeColumnValues(new DateTime().toString(), protein.getAc(), protein.getShortLabel(), primaryId, message);
            writer.flush();
        }
    }

    private String dashIfNull(String str) {str = (str != null)? str : EMPTY_VALUE;
        return str;
    }

    private String booleanToYesNo(boolean bool) {
        return bool? "Y" : "N";
    }

    private String getPrimaryIdString(Protein protein) {
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);

        String primaryId = null;

        if (uniprotXref != null) {
            primaryId = uniprotXref.getPrimaryId();
        } else {
            StringBuilder sb = new StringBuilder(64);
            List<InteractorXref> xrefs = ProteinUtils.getIdentityXrefs(protein);

            Iterator<InteractorXref> iterator = xrefs.iterator();
            while (iterator.hasNext()) {
                InteractorXref xref =  iterator.next();
                sb.append(xref.getCvDatabase().getShortLabel()).append(":").append(xref.getPrimaryId());
                if (iterator.hasNext()) sb.append("|");
                primaryId = sb.toString();
            }

            if (xrefs.isEmpty()) {
                primaryId = EMPTY_VALUE;
            }
        }
        return primaryId;
    }


}
