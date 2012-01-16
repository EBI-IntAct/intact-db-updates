package uk.ac.ebi.intact.dbupdate.cv.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.cv.errors.CvUpdateError;
import uk.ac.ebi.intact.dbupdate.cv.events.*;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvObjectAlias;
import uk.ac.ebi.intact.model.CvObjectXref;

import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.Collection;

/**
 * This listener will write info about different events
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/11/11</pre>
 */

public class ReportWriterListener implements CvUpdateListener{

    private FileWriter obsoleteRemappedWriter;
    private FileWriter obsoleteImpossibleToRemapWriter;
    private FileWriter updatedCvWriter;
    private FileWriter createdCvWriter;
    private FileWriter updateErrorWriter;
    private FileWriter deletedCvWriter;

    private static final Log log = LogFactory.getLog(ReportWriterListener.class);

    private static final String COL_SEPARATOR = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final char HEADER_CHAR = '#';
    private static final String EMPTY_VALUE = "-";

    private boolean isObsoleteRemappedTermStarted = false;
    private boolean isObsoleteTermImpossibleToRemapStarted= false;
    private boolean isUpdatedCvTermStarted = false;
    private boolean isCreatedCvTermStarted = false;
    private boolean isUpdateErrorStarted = false;
    private boolean isDeletedCvStarted = false;

    public ReportWriterListener(File dirFile) throws IOException {
        if (!dirFile.exists()) {
            dirFile.mkdirs();
        }
        if (!dirFile.isDirectory()) {
            throw new IOException("The file passed to the constructor has to be a directory: "+dirFile);
        }

        this.obsoleteRemappedWriter = new FileWriter(new File(dirFile, "obsolete_remapped.csv"));
        this.obsoleteImpossibleToRemapWriter = new FileWriter(new File(dirFile, "obsolete_impossible_remap.csv"));
        this.updatedCvWriter = new FileWriter(new File(dirFile, "updated.csv"));
        this.createdCvWriter = new FileWriter(new File(dirFile, "created.csv"));
        this.updateErrorWriter = new FileWriter(new File(dirFile, "update_errors.csv"));
        this.updateErrorWriter = new FileWriter(new File(dirFile, "deleted.csv"));
    }

    public void close() throws IOException {
        obsoleteRemappedWriter.close();
        obsoleteImpossibleToRemapWriter.close();
        updatedCvWriter.close();
        createdCvWriter.close();
        updateErrorWriter.close();
        deletedCvWriter.close();

        isCreatedCvTermStarted = false;
        isObsoleteRemappedTermStarted = false;
        isObsoleteTermImpossibleToRemapStarted = false;
        isUpdatedCvTermStarted = false;
        isUpdateErrorStarted = false;
        isDeletedCvStarted = false;
    }


    @Override
    public void onObsoleteRemappedTerm(ObsoleteRemappedEvent evt) {
        try {
            FileWriter writer = obsoleteRemappedWriter;

            if (!isObsoleteRemappedTermStarted){
                writeHeader(writer, "Old term", "New term", "Old Intact Ac", "Merged Intact Ac", "Number of updates", "message");
            }

            String oldTerm = evt.getOldTerm() != null ? evt.getOldTerm() : EMPTY_VALUE;
            String newTerm = evt.getNewTerm() != null ? evt.getNewTerm() : EMPTY_VALUE;
            String oldIntact = evt.getOldIntactAc() != null ? evt.getOldIntactAc() : EMPTY_VALUE;
            String newIntact = evt.getMergedIntactAc()!= null ? evt.getMergedIntactAc() : EMPTY_VALUE;
            String numberOfUpdate = Integer.toString(evt.getNumberOfUpdates());
            String message = evt.getUpdateMessage() != null ? evt.getUpdateMessage() : EMPTY_VALUE;

            writeColumnValues(writer, oldTerm, newTerm, oldIntact, newIntact, numberOfUpdate, message);


        } catch (Exception e) {
            log.fatal("Problem writing obsolete term successfully remapped.", e);
        }
    }

    @Override
    public void onObsoleteTermImpossibleToRemap(ObsoleteTermImpossibleToRemapEvent evt) {
        try {
            FileWriter writer = obsoleteImpossibleToRemapWriter;

            if (!isObsoleteTermImpossibleToRemapStarted){
                writeHeader(writer, "Obsolete Id", "Intact Ac", "Intact shortLabel", "Message", "Possible terms to remap to");
            }

            String obsoleteId = evt.getObsoleteId() != null ? evt.getObsoleteId() : EMPTY_VALUE;
            String intactAc = evt.getCvIntactAc() != null ? evt.getCvIntactAc() : EMPTY_VALUE;
            String intactLabel = evt.getCvLabel() != null ? evt.getCvLabel() : EMPTY_VALUE;
            String message = evt.getMessage()!= null ? evt.getMessage() : EMPTY_VALUE;

            StringBuffer buffer = new StringBuffer(1024);
            writeStrings(buffer, evt.getPossibleTerms());

            writeColumnValues(writer, obsoleteId, intactAc, intactLabel, message, buffer.toString());


        } catch (Exception e) {
            log.fatal("Problem writing obsolete term impossible to remapped.", e);
        }
    }

    @Override
    public void onUpdatedCvTerm(UpdatedEvent evt) {
        try {
            FileWriter writer = updatedCvWriter;

            if (!isUpdatedCvTermStarted){
                writeHeader(writer, "Term accession", "Intact Ac", "Updated shortLabel", "Updated fullName", "Identifier updated",
                        "Created xrefs", "Updated xrefs", "Deleted xrefs",
                        "Created aliases", "Updated aliases", "Deleted aliases",
                        "Created annotations", "Updated annotations", "Deleted annotations",
                        "Created parents", "Deleted parents");
            }

            String termAccession = evt.getTermAccession() != null ? evt.getTermAccession() : EMPTY_VALUE;
            String intactAc = evt.getIntactAc() != null ? evt.getIntactAc() : EMPTY_VALUE;
            String updatedLabel = evt.getUpdatedShortLabel()!= null ? evt.getUpdatedShortLabel() : EMPTY_VALUE;
            String updatedFullName = evt.getUpdatedFullName()!= null ? evt.getUpdatedFullName() : EMPTY_VALUE;
            String updatedIdentifier= Boolean.toString(evt.hasUpdatedIdentifier());

            StringBuffer createdXrefs = new StringBuffer(1024);
            writeXrefs(createdXrefs, evt.getCreatedXrefs());

            StringBuffer updatedXrefs = new StringBuffer(1024);
            writeXrefs(updatedXrefs, evt.getUpdatedXrefs());

            StringBuffer deletedXrefs = new StringBuffer(1024);
            writeXrefs(deletedXrefs, evt.getDeletedXrefs());

            StringBuffer createdAliases = new StringBuffer(1024);
            writeAliases(createdAliases, evt.getCreatedAliases());

            StringBuffer updatedAliases = new StringBuffer(1024);
            writeAliases(updatedAliases, evt.getUpdatedAliases());

            StringBuffer deletedAliases = new StringBuffer(1024);
            writeAliases(deletedAliases, evt.getDeletedAliases());

            StringBuffer createdAnnotations = new StringBuffer(1024);
            writeAnnotations(createdAnnotations, evt.getCreatedAnnotations());

            StringBuffer updatedAnnotations = new StringBuffer(1024);
            writeAnnotations(updatedAnnotations, evt.getUpdatedAnnotations());

            StringBuffer deletedAnnotations = new StringBuffer(1024);
            writeAnnotations(deletedAnnotations, evt.getDeletedAnnotations());

            StringBuffer createdParents = new StringBuffer(1024);
            writeParents(createdParents, evt.getCreatedParents());

            StringBuffer deletedParents = new StringBuffer(1024);
            writeParents(deletedParents, evt.getDeletedParents());

            writeColumnValues(writer, termAccession, intactAc, updatedLabel, updatedFullName, updatedIdentifier, createdXrefs.toString(),
                    updatedXrefs.toString(), deletedXrefs.toString(), createdAliases.toString(), updatedAliases.toString(),
                    deletedAliases.toString(), createdAnnotations.toString(), updatedAnnotations.toString(), deletedAnnotations.toString(),
                    createdParents.toString(), deletedParents.toString());


        } catch (Exception e) {
            log.fatal("Problem writing updated term.", e);
        }
    }

    private void writeXrefs(StringBuffer createdXrefs, Collection<CvObjectXref> refs) {
        int index = 0;
        int size = refs.size();

        if (refs.isEmpty()){
            createdXrefs.append(EMPTY_VALUE);
        }
        else {
            for (CvObjectXref ref : refs){

                createdXrefs.append(ref.getCvDatabase().getShortLabel());
                createdXrefs.append(":");
                createdXrefs.append(ref.getPrimaryId());
                createdXrefs.append(":");
                createdXrefs.append(ref.getCvXrefQualifier().getShortLabel());

                if (index < size - 1){
                    createdXrefs.append(", ");
                }
                index ++;
            }
        }
    }

    private void writeAliases(StringBuffer alias, Collection<CvObjectAlias> aliases) {
        int index = 0;
        int size = aliases.size();

        if (aliases.isEmpty()){
            alias.append(EMPTY_VALUE);
        }
        else {
            for (CvObjectAlias ref : aliases){

                alias.append(ref.getCvAliasType().getShortLabel());
                alias.append(":");
                alias.append(ref.getName());

                if (index < size - 1){
                    alias.append(", ");
                }
                index ++;
            }
        }
    }

    private void writeAnnotations(StringBuffer annotation, Collection<Annotation> annotations) {
        int index = 0;
        int size = annotations.size();

        if (annotations.isEmpty()){
            annotation.append(EMPTY_VALUE);
        }
        else {
            for (Annotation annot : annotations){

                annotation.append(annot.getCvTopic().getShortLabel());
                annotation.append(":");
                annotation.append(annot.getAnnotationText() != null ? annot.getAnnotationText() : EMPTY_VALUE);

                if (index < size - 1){
                    annotation.append(", ");
                }
                index ++;
            }
        }
    }

    private void writeParents(StringBuffer buffer, Collection<CvDagObject> collection) {
        int index = 0;
        int size = collection.size();

        if (collection.isEmpty()){
            buffer.append(EMPTY_VALUE);
        }
        else {
            for (CvDagObject annot : collection){

                buffer.append(annot.getAc());
                buffer.append(":");
                buffer.append(annot.getShortLabel());
                buffer.append(":");
                buffer.append(annot.getIdentifier());

                if (index < size - 1){
                    buffer.append(", ");
                }
                index ++;
            }
        }
    }

    private void writeStrings(StringBuffer buffer, Collection<String> collection) {
        int index = 0;
        int size = collection.size();

        if (collection.isEmpty()){
            buffer.append(EMPTY_VALUE);
        }
        else {
            for (String annot : collection){

                buffer.append(annot);

                if (index < size - 1){
                    buffer.append(", ");
                }
                index ++;
            }
        }
    }

    @Override
    public void onCreatedCvTerm(CreatedTermEvent evt) {
        try {
            FileWriter writer = createdCvWriter;

            if (!isCreatedCvTermStarted){
                writeHeader(writer, "Term Id", "Intact Ac", "Intact shortLabel", "Message", "Is Hidden");
            }

            String termAc = evt.getTermAc() != null ? evt.getTermAc() : EMPTY_VALUE;
            String intactAc = evt.getIntactAc() != null ? evt.getIntactAc() : EMPTY_VALUE;
            String intactLabel = evt.getShortLabel() != null ? evt.getShortLabel() : EMPTY_VALUE;
            String message = evt.getMessage()!= null ? evt.getMessage() : EMPTY_VALUE;
            String isHidden = Boolean.toString(evt.isHidden());

            writeColumnValues(writer, termAc, intactAc, intactLabel, message, isHidden);


        } catch (Exception e) {
            log.fatal("Problem writing obsolete term impossible to remapped.", e);
        }
    }

    @Override
    public void onUpdateError(UpdateErrorEvent evt) {
        try {
            FileWriter writer = updateErrorWriter;

            if (!isUpdateErrorStarted){
                writeHeader(writer, "Term Id", "Intact Ac", "Intact shortLabel", "Error type", "Error Message");
            }

            CvUpdateError error = evt.getUpdateError();

            if (error != null){
                String termAc = error.getTermAc() != null ? error.getTermAc() : EMPTY_VALUE;
                String intactAc = error.getIntactAc() != null ? error.getIntactAc() : EMPTY_VALUE;
                String intactLabel = error.getIntactLabel() != null ? error.getIntactLabel() : EMPTY_VALUE;
                String message = error.getErrorMessage()!= null ? error.getErrorMessage() : EMPTY_VALUE;
                String label = error.getErrorLabel()!= null ? error.getErrorLabel().toString() : EMPTY_VALUE;

                writeColumnValues(writer, termAc, intactAc, intactLabel, label, message);
            }

        } catch (Exception e) {
            log.fatal("Problem writing obsolete term impossible to remapped.", e);
        }
    }

    @Override
    public void onDeletedCvTerm(DeletedTermEvent evt) {
        try {
            FileWriter writer = deletedCvWriter;

            if (!isDeletedCvStarted){
                writeHeader(writer, "Term Id", "Intact Ac", "Intact shortLabel", "Message");
            }

            String termAc = evt.getTermAc() != null ? evt.getTermAc() : EMPTY_VALUE;
            String intactAc = evt.getIntactAc() != null ? evt.getIntactAc() : EMPTY_VALUE;
            String intactLabel = evt.getShortLabel() != null ? evt.getShortLabel() : EMPTY_VALUE;
            String message = evt.getMessage()!= null ? evt.getMessage() : EMPTY_VALUE;

            writeColumnValues(writer, termAc, intactAc, intactLabel, message);


        } catch (Exception e) {
            log.fatal("Problem writing obsolete term impossible to remapped.", e);
        }
    }

    public void writeHeader(FileWriter writer, String ... colHeaderTexts) throws IOException {
        writer.write(HEADER_CHAR);
        writeColumnValues(writer, colHeaderTexts);
    }

    public void writeColumnValues(FileWriter writer, String ... colValues) throws IOException {

        StringBuilder sb = new StringBuilder();

        for (int i=0; i<colValues.length; i++) {
            if (i > 0) {
                sb.append(COL_SEPARATOR);
            }
            sb.append(colValues[i]);
        }

        sb.append(NEW_LINE);

        writer.write(sb.toString());

        writer.flush();
    }

    public void writeLine(FileWriter writer, String str) throws IOException {
        writer.write(str + NEW_LINE);
    }
}
