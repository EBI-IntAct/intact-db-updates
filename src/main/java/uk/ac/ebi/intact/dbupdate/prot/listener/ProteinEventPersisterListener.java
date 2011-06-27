package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.*;
import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEventWithMessage;
import uk.ac.ebi.intact.update.persistence.UpdateDaoFactory;

import java.util.Date;

/**
 * This listener will persist each event of the protein update using update-modal persistence unit
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */

public class ProteinEventPersisterListener extends AbstractProteinUpdateProcessorListener {

    /**
     * The dao factory for update model persistence unit
     */
    @Autowired
    private UpdateDaoFactory updateFactory;

    private UpdateProcess updateProcess;

    public ProteinEventPersisterListener(){
       createUpdateProcess();
    }

    @Transactional( "update" )
    private void createUpdateProcess(){
        this.updateProcess = new UpdateProcess(new Date(System.currentTimeMillis()));

        getUpdateFactory().getUpdateProcessDao().persist(this.updateProcess);
    }

    @Transactional( "update" )
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        ProteinEventWithMessage proteinEvt = new ProteinEventWithMessage(this.updateProcess, EventName.deleted_protein, protein, evt.getIndex(), evt.getMessage());

        // all aliases deleted
        for (InteractorAlias alias : protein.getAliases()){
            proteinEvt.addUpdatedAlias(new UpdatedAlias(alias, UpdateStatus.deleted));
        }
        // all xrefs deleted
        for (InteractorXref xref : protein.getXrefs()){
            proteinEvt.addUpdatedXRef(new UpdatedCrossReference(xref, UpdateStatus.deleted));
        }
        // all annotations deleted
        for (Annotation annotation : protein.getAnnotations()){
            proteinEvt.addUpdatedAnnotation(new UpdatedAnnotation(annotation, UpdateStatus.deleted));
        }

        getUpdateFactory().getProteinEventDao(ProteinEventWithMessage.class).saveOrUpdate(proteinEvt);
    }

    @Transactional( "update" )
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {

    }

    @Override
    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {
        super.onDeadProteinFound(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        super.onProteinSequenceChanged(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Transactional( "update" )
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        ProteinEventWithMessage proteinEvt = new ProteinEventWithMessage(this.updateProcess, EventName.created_protein, protein, evt.getIndex(), evt.getMessage());

        // all aliases deleted
        for (InteractorAlias alias : protein.getAliases()){
            proteinEvt.addUpdatedAlias(new UpdatedAlias(alias, UpdateStatus.added));
        }
        // all xrefs deleted
        for (InteractorXref xref : protein.getXrefs()){
            proteinEvt.addUpdatedXRef(new UpdatedCrossReference(xref, UpdateStatus.added));
        }
        // all annotations deleted
        for (Annotation annotation : protein.getAnnotations()){
            proteinEvt.addUpdatedAnnotation(new UpdatedAnnotation(annotation, UpdateStatus.added));
        }
    }

    @Override
    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        super.onUpdateCase(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        super.onNonUniprotProteinFound(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onRangeChanged(RangeChangedEvent evt) throws ProcessorException {
        super.onRangeChanged(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException {
        super.onInvalidRange(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException {
        super.onOutOfDateRange(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {
        super.onOutOfDateParticipantFound(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException {
        super.onProcessErrorFound(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException {
        super.onSecondaryAcsFound(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException {
        super.onProteinTranscriptWithSameSequence(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException {
        super.onInvalidIntactParent(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException {
        super.onProteinRemapping(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onProteinSequenceCaution(ProteinSequenceCautionEvent evt) throws ProcessorException {
        super.onProteinSequenceCaution(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Override
    public void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException {
        super.onDeletedComponent(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public UpdateDaoFactory getUpdateFactory() {
        return updateFactory;
    }
}
