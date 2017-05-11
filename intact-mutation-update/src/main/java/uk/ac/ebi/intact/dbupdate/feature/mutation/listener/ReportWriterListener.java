package uk.ac.ebi.intact.dbupdate.feature.mutation.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import psidev.psi.mi.jami.model.Range;
import uk.ac.ebi.intact.dbupdate.feature.mutation.writer.FileReportHandler;
import uk.ac.ebi.intact.dbupdate.feature.mutation.writer.ReportWriter;
import uk.ac.ebi.intact.jami.model.extension.ExperimentalRange;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ReportWriterListener extends AbstractShortlabelGeneratorListener {

    private static final Log log = LogFactory.getLog(ReportWriterListener.class);

    private FileReportHandler fileReportHandler;

    public ReportWriterListener(FileReportHandler reportHandler) {
        this.fileReportHandler = reportHandler;
    }

    public void onRangeError(RangeErrorEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String rangeAc = event.getRangeAc();
        String errorType = event.getErrorType().name();
        String errorMessage = event.getMessage();
        try {
            ReportWriter rangeErrorWriter = fileReportHandler.getRangeErrorReport();
            rangeErrorWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "range_ac", "error_type", "error_message");
            rangeErrorWriter.writeColumnValues(featureAc, interactorAc, rangeAc, errorType, errorMessage);
            rangeErrorWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onModifiedMutationShortlabel(ModifiedMutationShortlabelEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String originalShortlabel = event.getOriginalShortlabel();
        String calculatedShortlabel = event.getFeatureEvidence().getShortName();
        String rangeAc;
        String rangeStart;
        String rangeEnd;
        String orgSeq;
        String resSeq;

        try {
            ReportWriter modifiedMutationsReport = fileReportHandler.getModifiedMutationsReport();
            modifiedMutationsReport.writeHeaderIfNecessary("feature_ac", "interactor_ac", "original_shortlabel", "new_shortlabel", "range_ac", "start_range", "end_range", "org_seq", "res_seq");
            for (Range range : event.getFeatureEvidence().getRanges()) {
                rangeAc = ((ExperimentalRange) range).getAc();
                rangeStart = String.valueOf(range.getStart().getStart());
                rangeEnd = String.valueOf(range.getEnd().getEnd());
                orgSeq = range.getResultingSequence().getOriginalSequence();
                resSeq = range.getResultingSequence().getNewSequence();

                modifiedMutationsReport.writeColumnValues(featureAc, interactorAc, originalShortlabel, calculatedShortlabel, rangeAc, rangeStart, rangeEnd, orgSeq, resSeq);
            }
            modifiedMutationsReport.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onUnmodifiedMutationShortlabel(UnmodifiedMutationShortlabelEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String originalShortlabel = event.getFeatureEvidence().getShortName();
        String rangeAc;
        String rangeStart;
        String rangeEnd;
        String orgSeq;
        String resSeq;

        try {
            ReportWriter unmodifiedMutationsReport = fileReportHandler.getUnmodifiedMutationsReport();
            unmodifiedMutationsReport.writeHeaderIfNecessary("feature_ac", "interactor_ac", "original_shortlabel", "range_ac", "start_range", "end_range", "org_seq", "res_seq");
            for (Range range : event.getFeatureEvidence().getRanges()) {
                rangeAc = ((ExperimentalRange) range).getAc();
                rangeStart = String.valueOf(range.getStart().getStart());
                rangeEnd = String.valueOf(range.getEnd().getEnd());
                orgSeq = range.getResultingSequence().getOriginalSequence();
                resSeq = range.getResultingSequence().getNewSequence();

                unmodifiedMutationsReport.writeColumnValues(featureAc, interactorAc, originalShortlabel, rangeAc, rangeStart, rangeEnd, orgSeq, resSeq);
            }
            unmodifiedMutationsReport.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onRetrieveObjectError(ObjRetrieveErrorEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String errorType = event.getErrorType().name();
        String errorMessage = event.getMessage();
        try {
            ReportWriter retrieveObjectErrorWriter = fileReportHandler.getRetrieveObjectErrorReport();
            retrieveObjectErrorWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "error_type", "error_message");
            retrieveObjectErrorWriter.writeColumnValues(featureAc, interactorAc, errorType, errorMessage);
            retrieveObjectErrorWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onAnnotationFound(AnnotationFoundEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String annotationType = event.getType().name();
        String message = event.getMessage();
        try {
            ReportWriter annotationFoundWriter = fileReportHandler.getAnnotationFoundReport();
            annotationFoundWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "annotation_type", "message");
            annotationFoundWriter.writeColumnValues(featureAc, interactorAc, annotationType, message);
            annotationFoundWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onSequenceError(SequenceErrorEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String rangeAc = event.getRangeAc();
        String errorType = event.getErrorType().name();
        String errorMessage = event.getMessage();
        try {
            ReportWriter sequenceErrorWriter = fileReportHandler.getSequenceErrorReport();
            sequenceErrorWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "range_ac", "error_type", "error_message");
            sequenceErrorWriter.writeColumnValues(featureAc, interactorAc, rangeAc, errorType, errorMessage);
            sequenceErrorWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onResultingSequenceChanged(ResultingSequenceChangedEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String rangeAc = event.getRangeAc();
        String changeType = event.getChangeType().name();
        String changeMessage = event.getMessage();
        try {
            ReportWriter resultingSequenceChangedWriter = fileReportHandler.getResultingSequenceChangedReport();
            resultingSequenceChangedWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "range_ac", "change_type", "change_message");
            resultingSequenceChangedWriter.writeColumnValues(featureAc, interactorAc, rangeAc, changeType, changeMessage);
            resultingSequenceChangedWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public void onObjectTypeError(TypeErrorEvent event) {
        String featureAc = event.getFeatureAc();
        String interactorAc = event.getInteractorAc();
        String errorType = event.getErrorType().name();
        String errorMessage = event.getMessage();

        try {
            ReportWriter objectTypeErrorWriter = fileReportHandler.getObjectTypeErrorReport();
            objectTypeErrorWriter.writeHeaderIfNecessary("feature_ac", "interactor_ac", "error_type", "error_message");
            objectTypeErrorWriter.writeColumnValues(featureAc, interactorAc, errorType, errorMessage);
            objectTypeErrorWriter.flush();
        } catch (IOException e) {
            e.printStackTrace();
        }
    }
}
