package uk.ac.ebi.intact.dbupdate.prot.listener;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;

/**
 * Basic implementation of the ProteinUpdateProcessorListener
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class AbstractProteinUpdateProcessorListener implements ProteinUpdateProcessorListener {

    public void onDelete(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {
         // nothing
    }

    public void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        // nothing
    }

    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        // nothing
    }

    public void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException {
        // nothing
    }

    public void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException {
        // nothing
    }

    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException {
        // nothing
    }

    public void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException {
        // nothing
    }

    public void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException {
        // nothing
    }

    public void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException{
        // nothing
    }

    public void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException{
        // nothing
    }

    public void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException{
        // nothing
    }

    public void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException{
        // nothing
    }

}