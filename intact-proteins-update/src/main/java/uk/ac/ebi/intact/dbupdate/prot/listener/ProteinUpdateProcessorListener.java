package uk.ac.ebi.intact.dbupdate.prot.listener;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;

import java.util.EventListener;

/**
 * Listener for ProteinProcessors
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface ProteinUpdateProcessorListener extends EventListener {

    void onDelete(ProteinEvent evt) throws ProcessorException;

    void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException;

    void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException;

    void onProteinCreated(ProteinEvent evt) throws ProcessorException;

    void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException;

    void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException;

    void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException;

    void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException;

    void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException;

    void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException;

    void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException;

    void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException;

    void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException;

    void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException;

    void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException;

    void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException;

    void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException;
}