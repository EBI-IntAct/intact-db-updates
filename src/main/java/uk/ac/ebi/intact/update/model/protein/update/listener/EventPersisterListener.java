package uk.ac.ebi.intact.update.model.protein.update.listener;

import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.IntactUpdateContext;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;
import uk.ac.ebi.intact.update.model.protein.update.ProteinUpdateAlias;
import uk.ac.ebi.intact.update.model.protein.update.ProteinUpdateAnnotation;
import uk.ac.ebi.intact.update.model.protein.update.ProteinUpdateCrossReference;
import uk.ac.ebi.intact.update.model.protein.update.events.DuplicatedProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.PersistentProteinEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.SequenceUpdateEvent;
import uk.ac.ebi.intact.update.model.protein.update.events.UniprotUpdateEvent;
import uk.ac.ebi.intact.util.protein.utils.AliasUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.ProteinNameUpdateReport;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;

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
public class EventPersisterListener implements ProteinUpdateProcessorListener {

    private ProteinUpdateProcess updateProcess;

    @Transactional( "update" )
    public void createUpdateProcess(){
        this.updateProcess = new ProteinUpdateProcess(new Date(System.currentTimeMillis()), "PROTEIN_UPDATE_RUNNER");

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinUpdateProcessDao().persist(this.updateProcess);
    }

    @Transactional( "update" )
    public void onDelete(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        PersistentProteinEvent proteinEvt = new PersistentProteinEvent(this.updateProcess, ProteinEventName.deleted_protein, protein, evt.getUniprotIdentity());
        proteinEvt.setMessage(evt.getMessage());

        // all aliases deleted
        for (InteractorAlias alias : protein.getAliases()){
            proteinEvt.addUpdatedAlias(new ProteinUpdateAlias(alias, UpdateStatus.deleted));
        }
        // all xrefs deleted
        for (InteractorXref xref : protein.getXrefs()){
            proteinEvt.addUpdatedXRef(new ProteinUpdateCrossReference(xref, UpdateStatus.deleted));
        }
        // all annotations deleted
        for (Annotation annotation : protein.getAnnotations()){
            proteinEvt.addUpdatedAnnotation(new ProteinUpdateAnnotation(annotation, UpdateStatus.deleted));
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(PersistentProteinEvent.class).persist(proteinEvt);
    }

    @Transactional( "update" )
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {

        Protein originalProtein = evt.getReferenceProtein();
        boolean sequenceUpdate = evt.hasShiftedRanges();

        for (Protein duplicate : evt.getProteins()){

            Collection<String> movedInteractions = evt.getMovedInteractions().get(duplicate.getAc());
            RangeUpdateReport rangeReport = evt.getComponentsWithFeatureConflicts().get(duplicate);
            Collection<Annotation> addedAnnotations = evt.getAddedAnnotations().get(duplicate.getAc());
            Collection<String> updatedTranscripts = evt.getUpdatedTranscripts().get(duplicate.getAc());
            Collection<InteractorXref> addedXrefs = evt.getAddedXRefs().get(duplicate.getAc());

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
                    duplicatedEvent.addUpdatedAnnotation(new ProteinUpdateAnnotation(annotation, UpdateStatus.added));
                }
            }

            // added xrefs
            if (addedXrefs != null){
                for (InteractorXref xref : addedXrefs){
                    duplicatedEvent.addUpdatedXRef(new ProteinUpdateCrossReference(xref, UpdateStatus.added));
                }
            }

            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(DuplicatedProteinEvent.class).persist(duplicatedEvent);
        }
    }

    @Override
    @Transactional( "update" )
    public void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        String identity = evt.getUniprotIdentityXref() != null ? evt.getUniprotIdentityXref().getPrimaryId() : null;

        PersistentProteinEvent proteinEvt = new PersistentProteinEvent(this.updateProcess, ProteinEventName.dead_protein, protein, identity);

        proteinEvt.addUpdatedXRef(new ProteinUpdateCrossReference(evt.getUniprotIdentityXref(), UpdateStatus.updated));

        // all xrefs deleted
        for (InteractorXref xref : evt.getDeletedXrefs()){
            proteinEvt.addUpdatedXRef(new ProteinUpdateCrossReference(xref, UpdateStatus.deleted));
        }
        // all annotations deleted
        for (Annotation annotation : evt.getAddedAnnotations()){
            proteinEvt.addUpdatedAnnotation(new ProteinUpdateAnnotation(annotation, UpdateStatus.added));
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(PersistentProteinEvent.class).persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        SequenceUpdateEvent proteinEvt = new SequenceUpdateEvent(this.updateProcess, protein, evt.getNewSequence(), evt.getOldSequence(), evt.getRelativeConservation());

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(SequenceUpdateEvent.class).persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onProteinCreated(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        PersistentProteinEvent proteinEvt = new PersistentProteinEvent(this.updateProcess, ProteinEventName.created_protein, protein, evt.getUniprotIdentity());
        proteinEvt.setMessage(evt.getMessage());

        // all aliases deleted
        for (InteractorAlias alias : protein.getAliases()){
            proteinEvt.addUpdatedAlias(new ProteinUpdateAlias(alias, UpdateStatus.added));
        }
        // all xrefs deleted
        for (InteractorXref xref : protein.getXrefs()){
            proteinEvt.addUpdatedXRef(new ProteinUpdateCrossReference(xref, UpdateStatus.added));
        }
        // all annotations deleted
        for (Annotation annotation : protein.getAnnotations()){
            proteinEvt.addUpdatedAnnotation(new ProteinUpdateAnnotation(annotation, UpdateStatus.added));
        }

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(PersistentProteinEvent.class).persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException {

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
    @Transactional( "update" )
    public void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException {

        Protein protein = evt.getProtein();

        PersistentProteinEvent proteinEvt = new PersistentProteinEvent(this.updateProcess, ProteinEventName.non_uniprot_protein, protein);
        proteinEvt.setMessage(evt.getMessage());

        IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(PersistentProteinEvent.class).persist(proteinEvt);
    }

    @Override
    @Transactional( "update" )
    public void onRangeChanged(RangeChangedEvent evt) throws ProcessorException {
    }

    @Override
    public void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException {
    }

    @Override
    public void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException {
    }

    @Override
    public void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException {
    }

    @Override
    public void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException {
    }

    @Override
    public void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException {
    }

    @Override
    public void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException {
    }

    @Override
    public void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException {
    }

    @Override
    public void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException {
    }

    @Override
    public void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException {
    }

    @Override
    public void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException {
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

        if (needToBePersisted){
            IntactUpdateContext.getCurrentInstance().getUpdateFactory().getProteinEventDao(UniprotUpdateEvent.class).persist(proteinEvent);
        }
    }
}
