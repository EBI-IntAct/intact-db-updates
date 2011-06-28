package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.prot.DuplicateReport;
import uk.ac.ebi.intact.dbupdate.prot.IntactUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.*;
import uk.ac.ebi.intact.update.model.protein.update.events.DuplicatedProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.EventName;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEventWithMessage;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;
import java.util.Collection;
import java.util.Date;

/**
 * This listener will persist each event of the protein update using update-modal persistence unit
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */
@Component
public class ProteinEventPersisterListener extends AbstractProteinUpdateProcessorListener {

    /**
     * The entity manager
     */
    @PersistenceContext(unitName = "intact-update")
    private EntityManager entityManager;

    private UpdateProcess updateProcess;

    @Transactional( "update" )
    public void createUpdateProcess(){
        this.updateProcess = new UpdateProcess(new Date(System.currentTimeMillis()));

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getUpdateProcessDao().persist(this.updateProcess);
    }

    @Transactional( "update" )
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        ProteinEventWithMessage proteinEvt = new ProteinEventWithMessage(this.updateProcess, EventName.deleted_protein, protein, evt.getMessage());

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

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(ProteinEventWithMessage.class).persist(proteinEvt);
    }

    @Transactional( "update" )
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {

        DuplicateReport report = evt.getDuplicateReport();
        Protein originalProtein = report.getOriginalProtein();
        boolean sequenceUpdate = report.hasShiftedRanges();

        for (Protein duplicate : evt.getProteins()){

            Collection<String> movedInteractions = report.getMovedInteractions().get(duplicate.getAc());
            RangeUpdateReport rangeReport = report.getComponentsWithFeatureConflicts().get(duplicate);
            Collection<Annotation> addedAnnotations = report.getAddedAnnotations().get(duplicate.getAc());
            Collection<String> updatedTranscripts = report.getUpdatedTranscripts().get(duplicate.getAc());
            Collection<InteractorXref> addedXrefs = report.getAddedXRefs().get(duplicate.getAc());

            boolean isMergeSuccessful = (rangeReport == null);

            DuplicatedProteinEvent duplicatedEvent = new DuplicatedProteinEvent(this.updateProcess, duplicate, originalProtein, sequenceUpdate, isMergeSuccessful);

            // add the moved interactions
            if (movedInteractions != null){
                duplicatedEvent.getMovedInteractions().addAll(movedInteractions);
            }

            // add the updated transcripts
            if (updatedTranscripts != null){
                duplicatedEvent.getUpdatedTranscripts().addAll(updatedTranscripts);
            }

            // added annotations
            if (addedAnnotations != null){
                for (Annotation annotation : addedAnnotations){
                    duplicatedEvent.addUpdatedAnnotation(new UpdatedAnnotation(annotation, UpdateStatus.added));
                }
            }

            // added xrefs
            if (addedXrefs != null){
                for (InteractorXref xref : addedXrefs){
                    duplicatedEvent.addUpdatedXRef(new UpdatedCrossReference(xref, UpdateStatus.added));
                }
            }

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(DuplicatedProteinEvent.class).persist(duplicatedEvent);
        }
    }

    @Override
    @Transactional( "update" )
    public void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException {

    }

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        super.onProteinSequenceChanged(evt);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @Transactional( "update" )
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        ProteinEventWithMessage proteinEvt = new ProteinEventWithMessage(this.updateProcess, EventName.created_protein, protein, evt.getMessage());

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

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(ProteinEventWithMessage.class).persist(proteinEvt);
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

    public EntityManager getEntityManager() {
        return entityManager;
    }
}
