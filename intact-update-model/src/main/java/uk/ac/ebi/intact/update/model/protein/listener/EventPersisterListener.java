package uk.ac.ebi.intact.update.model.protein.listener;

import org.springframework.stereotype.Component;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.event.DeletedComponentEvent;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.FeatureUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.update.IntactUpdateContext;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.errors.DefaultPersistentUpdateError;
import uk.ac.ebi.intact.update.model.protein.events.*;
import uk.ac.ebi.intact.update.model.protein.mapping.results.PersistentIdentificationResults;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.util.protein.utils.AliasUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.AnnotationUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.ProteinNameUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;

import java.util.Collection;
import java.util.Date;
import java.util.Map;
import java.util.Set;

/**
 * This listener will persist each event of the protein update using update-modal persistence unit
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */
@Component
@TransactionConfiguration( transactionManager = "updateTransactionManager" )
@Transactional
public class EventPersisterListener implements ProteinUpdateProcessorListener {

    private ProteinUpdateProcess updateProcess;

    @Transactional( "update" )
    public void createUpdateProcess(){
        this.updateProcess = new ProteinUpdateProcess(new Date(System.currentTimeMillis()), "PROTEIN_UPDATE_RUNNER");

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinUpdateProcessDao().persist(this.updateProcess);
    }

    @Transactional( "update" )
    public void createUpdateProcess(String userstamp, Date date){
        this.updateProcess = new ProteinUpdateProcess(date, userstamp);

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinUpdateProcessDao().persist(this.updateProcess);
    }

    @Transactional( "update" )
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);
        DeletedProteinEvent proteinEvt = new DeletedProteinEvent(updateProcess, protein, evt.getUniprotIdentity());
        proteinEvt.setMessage(evt.getMessage());

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getDeletedProteinEventDao().persist(proteinEvt);
    }

    @Transactional( "update" )
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {

        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein originalProtein = evt.getReferenceProtein();

        for (Protein duplicate : evt.getProteins()){

            Set<String> movedInteractions = evt.getMovedInteractions().get(duplicate.getAc());
            RangeUpdateReport invalidRangeReport = evt.getComponentsWithFeatureConflicts().get(duplicate);
            Collection<String> updatedTranscripts = evt.getUpdatedTranscripts().get(duplicate.getAc());
            Collection<InteractorXref> movedXrefs = evt.getMovedXrefs().get(duplicate.getAc());
            RangeUpdateReport rangeReport = evt.getUpdatedRanges().get(duplicate.getAc());
            String uniprotAc = evt.getPrimaryUniprotAc();

            boolean isMergeSuccessful = (invalidRangeReport == null);

            DuplicatedProteinEvent duplicatedEvent = new DuplicatedProteinEvent(updateProcess, duplicate, originalProtein, uniprotAc, isMergeSuccessful);

            // add the moved interactions
            if (movedInteractions != null){
                duplicatedEvent.getMovedInteractions().addAll(movedInteractions);
            }

            // add the updated transcripts
            if (updatedTranscripts != null){
                duplicatedEvent.getUpdatedTranscripts().addAll(updatedTranscripts);
            }

            if (movedXrefs != null){
                duplicatedEvent.addMovedReferencesFromXref(movedXrefs);
            }

            collectRangeUpdateEvents(rangeReport, duplicatedEvent);

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getDuplicatedProteinEventDao().persist(duplicatedEvent);
        }
    }

    /**
     * Append all range events to the protein event to persist
     * @param rangeReport
     * @param duplicatedEvent
     */
    private void collectRangeUpdateEvents(RangeUpdateReport rangeReport, ProteinEventWithShiftedRanges duplicatedEvent) {
        if (rangeReport != null){
            Map<String, AnnotationUpdateReport> featureReport = rangeReport.getUpdatedFeatureAnnotations();

            for (Map.Entry<String, AnnotationUpdateReport> entry : featureReport.entrySet()){
                String featureAc = entry.getKey();

                if (featureAc != null){
                    if (!entry.getValue().getAddedAnnotations().isEmpty()){
                        duplicatedEvent.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getAddedAnnotations(), UpdateStatus.added);
                    }
                    if (!entry.getValue().getRemovedAnnotations().isEmpty()){
                        duplicatedEvent.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getRemovedAnnotations(), UpdateStatus.deleted);
                    }
                    if (!entry.getValue().getUpdatedAnnotations().isEmpty()){
                        duplicatedEvent.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getUpdatedAnnotations(), UpdateStatus.updated);
                    }
                }
            }

            Collection<UpdatedRange> shiftedRanges = rangeReport.getShiftedRanges();
            for (UpdatedRange updated : shiftedRanges){
                String oldSequence = updated.getOldRange() != null ? updated.getOldRange().getFullSequence() : null;
                String newSequence = updated.getNewRange() != null ? updated.getNewRange().getFullSequence() : null;

                String oldPositions = updated.getOldRange() != null ? FeatureUtils.convertRangeIntoString(updated.getOldRange()) : null;
                String newPositions = updated.getNewRange() != null ? FeatureUtils.convertRangeIntoString(updated.getNewRange()) : null;

                if (oldPositions != null && updated.getComponentAc() != null && updated.getFeatureAc() != null && updated.getInteractionAc() != null && updated.getRangeAc() != null){
                    PersistentUpdatedRange persistentRange = new PersistentUpdatedRange(duplicatedEvent, updated.getComponentAc(), updated.getFeatureAc(), updated.getInteractionAc(), updated.getRangeAc(), oldSequence, newSequence, oldPositions, newPositions);
                    duplicatedEvent.addRangeUpdate(persistentRange);
                }
            }
        }
    }

    @Override
    @Transactional( "update" )
    public void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein protein = evt.getProtein();

        String identity = evt.getUniprotIdentityXref() != null ? evt.getUniprotIdentityXref().getPrimaryId() : null;
        Collection<InteractorXref> deletedXrefs = evt.getDeletedXrefs();

        DeadProteinEvent proteinEvt = new DeadProteinEvent(updateProcess, protein, identity);

        if (deletedXrefs != null && !deletedXrefs.isEmpty()){
            proteinEvt.addDeletedReferencesFromXref(deletedXrefs);
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getDeadProteinEventDao().persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein protein = evt.getProtein();

        SequenceUpdateEvent proteinEvt = new SequenceUpdateEvent(updateProcess, protein, evt.getUniprotIdentity(), evt.getNewSequence(), evt.getOldSequence(), evt.getRelativeConservation());

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getSequenceUpdateEventDao().persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein protein = evt.getProtein();

        CreatedProteinEvent proteinEvt = new CreatedProteinEvent(updateProcess, protein, evt.getUniprotIdentity());
        proteinEvt.setMessage(evt.getMessage());

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getCreatedProteinEventDao().persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        this.updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        for (Protein prot : evt.getPrimaryProteins()){
            processUpdatedProtein(prot, evt);
        }

        for (Protein prot : evt.getSecondaryProteins()){
            processUpdatedProtein(prot, evt);
        }

        for (ProteinTranscript prot : evt.getPrimaryIsoforms()){
            processUpdatedProtein(prot.getProtein(), evt);
        }

        for (ProteinTranscript prot : evt.getSecondaryIsoforms()){
            processUpdatedProtein(prot.getProtein(), evt);
        }

        for (ProteinTranscript prot : evt.getPrimaryFeatureChains()){
            processUpdatedProtein(prot.getProtein(), evt);
        }
    }

    @Override
    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {
        // no need to keep this information in the database
    }

    @Override
    public void onRangeChanged(RangeChangedEvent evt) throws ProcessorException {
        // the listener does not take into account this event as it is taken into account when processing onDuplicateFound events and onUpdateCase events
    }

    @Override
    public void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException {
        // the listener does not take into account this event as it is taken into account when processing onOutOfDateParticipant events
    }

    @Override
    public void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException {
        // the listener does not take into account this event as it is taken into account when processing onOutOfDateParticipant events
    }

    @Override
    @Transactional( "update" )
    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        String remappedProteinAc = evt.getRemappedProteinAc();
        String remappedParentAc = evt.getValidParentAc();
        Protein currentProteinHavingConflicts = evt.getProteinWithConflicts();

        RangeUpdateReport invalidRanges = evt.getInvalidRangeReport();
        String uniprotId = evt.getProtein() != null ? evt.getProtein().getPrimaryAc() : null;

        OutOfDateParticipantEvent protEvt = new OutOfDateParticipantEvent(updateProcess, currentProteinHavingConflicts, uniprotId, remappedProteinAc, remappedParentAc);

        if (invalidRanges != null){
            Map<String, AnnotationUpdateReport> featureReport = invalidRanges.getUpdatedFeatureAnnotations();

            for (Map.Entry<String, AnnotationUpdateReport> entry : featureReport.entrySet()){
                String featureAc = entry.getKey();

                if (featureAc != null){
                    if (!entry.getValue().getAddedAnnotations().isEmpty()){
                        protEvt.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getAddedAnnotations(), UpdateStatus.added);
                    }
                    if (!entry.getValue().getRemovedAnnotations().isEmpty()){
                        protEvt.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getRemovedAnnotations(), UpdateStatus.deleted);
                    }
                    if (!entry.getValue().getUpdatedAnnotations().isEmpty()){
                        protEvt.addUpdatedFeatureAnnotationFromAnnotation(featureAc, entry.getValue().getUpdatedAnnotations(), UpdateStatus.updated);
                    }
                }
            }

            Map<uk.ac.ebi.intact.model.Component, Collection<InvalidRange>> invalidComponents = invalidRanges.getInvalidComponents();

            for (Map.Entry<uk.ac.ebi.intact.model.Component, Collection<InvalidRange>> entry : invalidComponents.entrySet()){
                Collection<InvalidRange> invalidRangesList = entry.getValue();

                for (InvalidRange inv : invalidRangesList){
                    String oldSequence = inv.getOldRange() != null ? inv.getOldRange().getFullSequence() : null;
                    String newSequence = inv.getNewRange() != null ? inv.getNewRange().getFullSequence() : null;

                    String oldPositions = inv.getOldPositions();
                    String newPositions = inv.getNewRangePositions();

                    if (oldPositions != null && inv.getComponentAc() != null && inv.getFeatureAc() != null && inv.getInteractionAc() != null && inv.getRangeAc() != null){
                        PersistentInvalidRange persistentRange = new PersistentInvalidRange(protEvt, inv.getComponentAc(), inv.getFeatureAc(), inv.getInteractionAc(), inv.getRangeAc(), oldSequence, newSequence, inv.getFromStatus(), inv.getToStatus(), oldPositions, newPositions, inv.getMessage(), inv.getValidSequenceVersion());
                        protEvt.addRangeUpdate(persistentRange);
                    }
                }
            }
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getOutOfDateParticipantEventDao().persist(protEvt);
    }

    @Override
    @Transactional( "update" )
    public void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        ProteinUpdateError error = evt.getError();

        if (error instanceof DefaultPersistentUpdateError){
            DefaultPersistentUpdateError persistentError = (DefaultPersistentUpdateError) error;

            persistentError.setUpdateProcess(updateProcess);

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinUpdateErrorDao(DefaultPersistentUpdateError.class).persist(persistentError);
        }
    }

    @Override
    @Transactional( "update" )
    public void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();

        Collection<XrefUpdaterReport> secondaryXrefReports = evt.getXrefUpdaterReports();

        for (Protein prot : secondaryProteins){

            InteractorXref oldPrimary = ProteinUtils.getUniprotXref(prot);
            String oldAc = oldPrimary != null ? oldPrimary.getPrimaryId() : null;

            SecondaryProteinEvent protEvt = new SecondaryProteinEvent(updateProcess, prot, oldAc, evt.getProtein().getPrimaryAc());

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getSecondaryProteinEventDao().persist(protEvt);
        }

        for (ProteinTranscript protTrans : secondaryIsoforms){

            Protein prot = protTrans.getProtein();

            InteractorXref oldPrimary = ProteinUtils.getUniprotXref(prot);
            String oldAc = oldPrimary != null ? oldPrimary.getPrimaryId() : null;

            SecondaryProteinEvent protEvt = new SecondaryProteinEvent(updateProcess, prot, oldAc, protTrans.getUniprotVariant().getPrimaryAc());

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getSecondaryProteinEventDao().persist(protEvt);
        }
    }

    @Override
    @Transactional( "update" )
    public void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        String currentAc = evt.getUniprotIdentity();
        String transcriptAc = evt.getUniprotTranscriptAc();
        Protein protein = evt.getProtein();

        SequenceIdenticalToTranscriptEvent protEvt = new SequenceIdenticalToTranscriptEvent(updateProcess, protein, currentAc, transcriptAc);

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getSequenceIdenticalToTranscriptEventDao().persist(protEvt);
    }

    @Override
    @Transactional( "update" )
    public void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein protein = evt.getProtein();
        String newParentAc = evt.getNewParentAc();
        String oldParentAc = evt.getOldParentAc();

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(evt.getProtein());
        String uniprotAc= uniprotXref != null ? uniprotXref.getPrimaryId() : null;

        IntactTranscriptUpdateEvent protEvt = new IntactTranscriptUpdateEvent(updateProcess, protein, uniprotAc, oldParentAc, newParentAc);

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getIntactTranscriptEventDao().persist(protEvt);

    }

    @Override
    @Transactional( "update" )
    public void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        if (evt.getResult() instanceof PersistentIdentificationResults){
            PersistentIdentificationResults result = (PersistentIdentificationResults) evt.getResult();

            Protein protein = evt.getProtein();

            UniprotProteinMapperEvent protEvt = new UniprotProteinMapperEvent(updateProcess, protein, result);
            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getUniprotProteinMapperEventDao().persist(protEvt);
        }
    }

    @Override
    public void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException {
        // do nothing. The sequence update releative conservation is kept in the database anyway with onProteinSequenceChanged event
    }

    @Override
    @Transactional( "update" )
    public void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException {
        // reattach the updateProcess to the entity manager
        ProteinUpdateProcess updateProcess = IntactUpdateContext.getCurrentInstance().getUpdateFactory().getEntityManager().merge(this.updateProcess);

        Protein protein = evt.getProtein();
        String uniprot = evt.getUniprotIdentity();

        Collection<uk.ac.ebi.intact.model.Component> deletedComponents = evt.getDeletedComponents();

        uk.ac.ebi.intact.update.model.protein.events.DeletedComponentEvent protEvt = new uk.ac.ebi.intact.update.model.protein.events.DeletedComponentEvent(updateProcess, protein, uniprot);

        for (uk.ac.ebi.intact.model.Component c : deletedComponents){
            protEvt.getDeletedComponents().add(c.getAc());
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getDeletedComponentEventDao().persist(protEvt);
    }

    private void processUpdatedProtein(Protein protein, UpdateCaseEvent evt){

        boolean needToBePersisted = false;

        UniprotUpdateEvent proteinEvent = new UniprotUpdateEvent(this.updateProcess, protein, evt.getQuerySentToService());

        if (evt.getNewAnnotations().containsKey(protein.getAc())){
            needToBePersisted = true;

            proteinEvent.addUpdatedAnnotationFromAnnotation(evt.getNewAnnotations().get(protein.getAc()), UpdateStatus.added);
        }

        for (AliasUpdateReport report : evt.getAliasUpdaterReports()){
            if (protein.getAc().equals(report.getProtein())){
                needToBePersisted = true;

                proteinEvent.addUpdatedAliasesFromAlias(report.getAddedAliases(), UpdateStatus.added);
                proteinEvent.addUpdatedAliasesFromAlias(report.getRemovedAliases(), UpdateStatus.deleted);
            }
        }

        for (XrefUpdaterReport report : evt.getXrefUpdaterReports()){
            if (protein.getAc().equals(report.getProtein())){
                needToBePersisted = true;

                proteinEvent.addUpdatedReferencesFromXref(report.getAddedXrefs(), UpdateStatus.added);
                proteinEvent.addUpdatedReferencesFromXref(report.getRemovedXrefs(), UpdateStatus.deleted);
            }
        }

        for (ProteinNameUpdateReport report : evt.getNameUpdaterReports()){
            if (protein.getAc().equals(report.getProtein())){
                needToBePersisted = true;

                proteinEvent.setUpdatedShortLabel(report.getShortLabel());
                proteinEvent.setUpdatedFullName(report.getFullName());
            }
        }

        RangeUpdateReport rangeReport = evt.getUpdatedRanges().get(protein.getAc());

        if (rangeReport != null){
            needToBePersisted = true;

            collectRangeUpdateEvents(rangeReport, proteinEvent);
        }

        if (needToBePersisted){
            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getUniprotUpdateEventDao().persist(proteinEvent);
        }
    }
}
