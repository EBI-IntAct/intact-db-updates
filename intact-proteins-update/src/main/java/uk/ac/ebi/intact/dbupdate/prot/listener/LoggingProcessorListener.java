package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;

/**
 * Listener for logging which event is fired
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class LoggingProcessorListener extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( LoggingProcessorListener.class );

    public void onDelete(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Deleted protein: "+evt.getProtein().getShortLabel());
    }

    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Duplicated proteins: "+ DebugUtil.acList(evt.getProteins()));
    }

    public void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Dead protein found: "+evt.getProtein().getShortLabel());
    }

    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Sequence for protein has changed: "+evt.getProtein().getShortLabel());
    }

    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Protein created: "+evt.getProtein().getShortLabel());
    }

    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Update case: "+evt.getProtein().getId()+" ("+
                evt.getPrimaryProteins().size()+" primary - "+evt.getSecondaryProteins().size()+" secondary)");
    }

    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Non-uniprot protein found: "+evt.getProtein().getShortLabel());
    }

    public void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Range "+evt.getInvalidRange().getRangeAc()+" wasn't updated because it " +
                "is invalid (range "+evt.getInvalidRange().getOldRange()+")");
    }

    @Override
    public void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Range "+evt.getInvalidRange().getRangeAc()+" wasn't updated because it " +
                "is invalid with the new protein sequence (range "+evt.getInvalidRange().getNewRange()+")");
    }

    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Protein " + evt.getProteinWithConflicts().getAc() + " will be demerged because some features attached to this protein are invalid with the new protein sequence.");
    }

    public void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("An error of type " + evt.getError().toString() + " occured");
    }

    public void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("We found "+evt.getSecondaryProteins().size()+" secondary proteins matching the uniprot Protein " + evt.getProtein().getPrimaryAc());
    }

    public void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException{
        if (log.isDebugEnabled()) log.debug("The protein transcript " + evt.getUniprotTranscriptAc() + " has exactly the same sequence as the protein " + evt.getProtein().getAc());
    }

    public void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException{
        if (log.isDebugEnabled()) log.debug("The protein transcript " + evt.getProtein().getAc() + " has invalid parent cross references.");
    }

    public void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException{
        if (log.isDebugEnabled()) log.debug("The protein " + evt.getContext().getIntactAccession() + " needs to be remapped to a uniprot entry.");
    }

    @Override
    public void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("The protein " + evt.getProtein().getAc() + " has a sequence which has been dramatically changed (sequence conservation = " + evt.getRelativeConservation() +").");
    }

    @Override
    public void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Deleted "+evt.getDeletedComponents().size()+" component(s) involving duplicated protein " + evt.getProtein().getAc() );
    }

}