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
package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.*;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.util.ComponentTools;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;

import java.util.*;

/**
 * Fix duplicated proteins in the database
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFixerImpl implements DuplicatesFixer{

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( DuplicatesFixerImpl.class );

    /**
     * The protein deleter for this class
     */
    private ProteinDeleter proteinDeleter;

    /**
     * The out of date participant fixer (when participant have feature range conflicts when trying to merge)
     */
    private OutOfDateParticipantFixer deprecatedParticipantFixer;

    private DuplicatesFinder duplicatesFinder;

    public static String CAUTION_PREFIX = "The protein could not be merged with ";

    /**
     * The range updater
     */
    private RangeFixer rangeFixer;

    public DuplicatesFixerImpl(){
        proteinDeleter = new ProteinDeleterImpl();
        deprecatedParticipantFixer = new OutOfDateParticipantFixerImpl();
        this.rangeFixer = new RangeFixerImpl();
        this.duplicatesFinder = new DuplicatesFinderImpl();
    }

    /**
     *
     * @param evt : contains the list of duplicated proteins
     * @return a DuplicateReport containing the protein kept from the merge and the list of components having feature conflicts
     * and which couldn't be merged
     * @throws ProcessorException
     */
    public DuplicateReport fixProteinDuplicates(DuplicatesFoundEvent evt) throws ProcessorException {
        // merge protein duplicates and shift ranges when necessary
        DuplicateReport report = mergeDuplicates(evt.getProteins(), evt);
        evt.setDuplicateReport(report);

        return report;
    }

    public Protein fixAllProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException {
        Protein masterProtein = null;

        // get the DuplicateFoundEvent with the list of duplicated proteins
        DuplicatesFoundEvent duplicateEvent = duplicatesFinder.findProteinDuplicates(evt);

        // we found real duplicates, we merge them
        if (duplicateEvent != null){
            if (log.isTraceEnabled()) log.trace("Fix the duplicates." );

            // merge duplicates and return a report with : original protein, map of proteins having feature range conflicts and the list
            // of components having feature conflicts
            DuplicateReport report = processDuplicatedProtein(evt, duplicateEvent);

            // the master protein is the result of the merge
            if (report.getOriginalProtein() != null){
                masterProtein = report.getOriginalProtein();

                // all the duplicated proteins have been fixed, clear the list of primary proteins to update and put only the master protein
                evt.getPrimaryProteins().clear();
                evt.getPrimaryProteins().add(masterProtein);
            }

            // log in 'duplicates.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProteinDuplicationFound(duplicateEvent);
            }
        }

        return masterProtein;
    }

    public void fixAllProteinTranscriptDuplicates(UpdateCaseEvent evt, Protein masterProtein) throws ProcessorException {

        Collection<DuplicatesFoundEvent> duplicateEvents = duplicatesFinder.findIsoformDuplicates(evt);

        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
        Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>(evt.getPrimaryIsoforms().size() + evt.getSecondaryIsoforms().size());

        for (DuplicatesFoundEvent duplEvt : duplicateEvents){
            DuplicateReport report = processDuplicatedTranscript(evt, duplEvt, masterProtein, true, mergedIsoforms);

            if (report.getOriginalProtein() != null){
                mergedIsoforms.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
            }

            // log in 'duplicates.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProteinDuplicationFound(duplEvt);
            }
        }

        if (!mergedIsoforms.isEmpty()){
            evt.getPrimaryIsoforms().clear();
            evt.getPrimaryIsoforms().addAll(mergedIsoforms);
        }

        Collection<DuplicatesFoundEvent> duplicateEvents2 = duplicatesFinder.findFeatureChainDuplicates(evt);

        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
        Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>(evt.getPrimaryFeatureChains().size());

        for (DuplicatesFoundEvent duplEvt : duplicateEvents2){
            DuplicateReport report = processDuplicatedTranscript(evt, duplEvt, masterProtein, false, mergedChains);
            if (report.getOriginalProtein() != null){
                mergedChains.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
            }

            // log in 'duplicates.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProteinDuplicationFound(duplEvt);
            }
        }

        if (!mergedChains.isEmpty()){
            evt.getPrimaryFeatureChains().clear();
            evt.getPrimaryFeatureChains().addAll(mergedChains);
        }
    }

    /**
     * Merge the duplicates, the interactions are moved from the duplicate to the original protein.
     * If there are feature range conflucts, the duplicate is not merged and the interactions having range conflicts are still attached to the duplicate
     * @param duplicates : the list of duplicates to merge
     * @param evt : the event
     */
    private DuplicateReport mergeDuplicates(Collection<Protein> duplicates, DuplicatesFoundEvent evt) {
        if (log.isDebugEnabled()) log.debug("Merging duplicates: "+ DebugUtil.acList(duplicates));

        // the list of duplicated proteins
        List<Protein> duplicatesAsList = new ArrayList<Protein>(duplicates);

        // the report
        DuplicateReport report = new DuplicateReport();

        // the collection which will contain the duplicates having the same sequence
        List<Protein> duplicatesHavingSameSequence = new ArrayList<Protein>(duplicatesAsList.size());

        // the collection which will contain the duplicates having different sequences
        List<Protein> duplicatesHavingDifferentSequence = new ArrayList<Protein>(duplicatesAsList.size());

        // while the list of possible duplicates has not been fully treated, we need to check the duplicates
        while (duplicatesAsList.size() > 0){
            // clear the list of duplicates having the same sequence
            duplicatesHavingSameSequence.clear();

            // pick the first protein of the list and add it in the list of duplicates having the same sequence
            Iterator<Protein> iterator = duplicatesAsList.iterator();
            Protein protToCompare = iterator.next();
            duplicatesHavingSameSequence.add(protToCompare);

            // the sequence of the protein
            String originalSequence = protToCompare.getSequence();

            // we compare the sequence of this first protein against the sequence of the other proteins
            while (iterator.hasNext()){
                // we extract the sequence of the next protein to compare
                Protein proteinCompared = iterator.next();
                String sequenceToCompare = proteinCompared.getSequence();

                // if the sequences are identical, we add the protein to the list of duplicates having the same sequence
                if (originalSequence != null && sequenceToCompare != null){
                    if (originalSequence.equalsIgnoreCase(sequenceToCompare)){
                        duplicatesHavingSameSequence.add(proteinCompared);
                    }
                }
                else if (originalSequence == null && sequenceToCompare == null){
                    duplicatesHavingSameSequence.add(proteinCompared);
                }
            }

            // if we have more than two proteins in the duplicate list having the exact same sequence, we merge them
            // without having to shift the ranges first
            if (duplicatesHavingSameSequence.size() > 1){
                // in the list of duplicates having different sequences, we can add the final protein which is the result of the merge of several proteins having the same sequence
                duplicatesHavingDifferentSequence.add(merge(duplicatesHavingSameSequence, Collections.EMPTY_MAP, evt, false, report));
            }
            // the duplicates didn't have the same sequence, we add it to the list of duplicates having different sequences
            else{
                duplicatesHavingDifferentSequence.addAll(duplicatesHavingSameSequence);
            }

            // we remove the processed proteins from the list of protein to process
            duplicatesAsList.removeAll(duplicatesHavingSameSequence);
        }

        // we still have to merge duplicates having different sequences
        if (duplicatesHavingDifferentSequence.size() > 1){
            // the uniprot protein has been found previously, it is possible to shift the ranges first using the uniprot sequence
            if (evt.getUniprotSequence() != null){
                // in case of feature conflicts, we will only attach the interactions without feature conflicts to the original protein and keep the
                // duplicate as no-uniprot-update with the interactions having feature conflicts
                Map<String, Collection<Component>> proteinNeedingPartialMerge = new HashMap<String, Collection<Component>>();

                // we try to shift the ranges of each protein to merge and collect the components with feature conflicts
                for (Protein p : duplicatesHavingDifferentSequence){
                    // update the ranges with the new uniprot sequence for each duplicate
                    RangeUpdateReport rangeReport = rangeFixer.updateRanges(p, evt.getUniprotSequence(), (ProteinUpdateProcessor) evt.getSource(), evt.getDataContext());
                    // get the list of components with feature range conflicts
                    Collection<Component> componentWithRangeConflicts = rangeReport.getInvalidComponents().keySet();

                    // if we have feature range conflicts before the merge
                    if (!componentWithRangeConflicts.isEmpty()){

                        log.info( "We found " + componentWithRangeConflicts.size() + " components with feature conflicts for the protein " + p.getAc() );
                        // add this duplicate with the list of components with conflicts to the map of protein which couldn't be merged
                        proteinNeedingPartialMerge.put(p.getAc(), componentWithRangeConflicts);
                        // add the components with feature conflicts in the report
                        report.getComponentsWithFeatureConflicts().put(p, rangeReport);
                    }
                }

                // we merge the proteins taking into account the possible feature conflicts
                Protein finalProt = merge(duplicatesHavingDifferentSequence, proteinNeedingPartialMerge, evt, true, report);
                log.info( "The protein " + finalProt.getAc() + "has been kept as original protein.");
                // we set the original protein in the report
                report.setOriginalProtein(finalProt);
            }
            // we cannot merge because we don't have a uniprot sequence as reference for range shifting. Log in 'process_errors.csv'
            else {
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "It is impossible to merge all the duplicates ("+duplicatesHavingDifferentSequence.size()+") because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.", UpdateError.impossible_merge, calculateOriginalProtein(new ArrayList(evt.getProteins()))));
                }
                log.error("It is impossible to merge all the duplicates ("+duplicatesHavingDifferentSequence.size()+") because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.");
            }
        }
        // all the duplicates had the same sequence and have been merged. Set the original protein in the report
        else if (duplicatesHavingDifferentSequence.size() == 1){
            report.setOriginalProtein(duplicatesHavingDifferentSequence.iterator().next());
        }

        // return the report
        return report;
    }

    /**
     * add a caution and 'no-uniprot-update' to the protein
     * @param protein : the duplicate with range conflicts
     * @param previousAc : the original protein to keep
     */
    private Collection<Annotation> addAnnotationsForBadParticipant(Protein protein, String previousAc, DaoFactory factory){

        Collection<Annotation> badAnnotations = new ArrayList<Annotation>(2);

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution = false;
        String cautionMessage = CAUTION_PREFIX + previousAc + " because of some conflicts with the protein sequence (features which cannot be shifted).";

        for (Annotation annotation : protein.getAnnotations()){
            if (no_uniprot_update.equals(annotation.getCvTopic())){
                has_no_uniprot_update = true;
            }
            else if (caution.equals(annotation.getCvTopic())){
                if (annotation.getAnnotationText() != null){
                    if (annotation.getAnnotationText().equalsIgnoreCase(cautionMessage)){
                        has_caution = true;
                    }
                }
            }
        }

        AnnotationDao annotationDao = factory.getAnnotationDao();

        if (!has_no_uniprot_update){
            Annotation no_uniprot = new Annotation(no_uniprot_update, null);
            annotationDao.persist(no_uniprot);

            protein.addAnnotation(no_uniprot);
            badAnnotations.add(no_uniprot);
        }

        if (!has_caution){
            Annotation demerge = new Annotation(caution, cautionMessage);
            annotationDao.persist(demerge);

            protein.addAnnotation(demerge);
            badAnnotations.add(demerge);
        }

        return badAnnotations;
    }

    /**
     * Merge the duplicates, the interactions are moved (not the cross references as they will be deleted)
     * @param duplicates
     */
    protected Protein merge(List<Protein> duplicates, Map<String, Collection<Component>> proteinsNeedingPartialMerge, DuplicatesFoundEvent evt, boolean isSequenceChanged, DuplicateReport report) {
        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // calculate the original protein (the oldest is kept as original)
        Protein originalProt = calculateOriginalProtein(duplicates);
        // set the protein kept from the merge
        evt.setReferenceProtein(originalProt);

        report.setOriginalProtein(originalProt);
        report.setUniprotSequence(evt.getUniprotSequence());
        report.setCrc64(evt.getUniprotCrc64());

        // the merge can be done without looking at the sequence of the duplicates
        if (!isSequenceChanged){
            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // report the interactions to move before moving them
                    reportMovedInteraction(duplicate, duplicate.getActiveInstances(), report);

                    // move the interactions
                    ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource());

                    // add the intact secondary references
                    Collection<InteractorXref> addedXRef = ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);

                    // update the protein transcripts if necessary
                    Collection<String> updatedTranscripts = ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

                    report.getAddedXRefs().put(originalProt.getAc(), addedXRef);
                    report.getUpdatedTranscripts().put(duplicate.getAc(), updatedTranscripts);

                    // update the duplicate
                    factory.getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate if no active instances are attached to it
                    if (duplicate.getActiveInstances().isEmpty()) {
                        deleteProtein(new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                    }
                    else {
                        throw new ProcessorException("The duplicate " + duplicate.getAc() + " still have " + duplicate.getActiveInstances().size() + " active instances and should not.");
                    }
                }
            }
        }
        // before merging, we need to check the feature conflicts because the sequence needs to be updated
        else {
            // even if the ranges were not shifted, the sequence has been updated
            report.setHasShiftedRanges(true);
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {
                // sequence of the duplicate
                String sequence = duplicate.getSequence();

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // we have feature conflicts for this protein which cannot be merged and becomes deprecated
                    if (proteinsNeedingPartialMerge.containsKey(duplicate.getAc())){
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                                "The duplicate " + duplicate.getAc() + " cannot be merged with " +
                                        originalProt.getAc() + " because we have " +
                                        proteinsNeedingPartialMerge.get(duplicate.getAc()).size() + " components with range conflicts. The protein is now deprecated.", UpdateError.impossible_merge, duplicate));
                        // add no-uniprot-update and caution
                        Collection<Annotation> addedAnnotations = addAnnotationsForBadParticipant(duplicate, originalProt.getAc(), factory);
                        // components to let on the current protein
                        Collection<Component> componentToFix = proteinsNeedingPartialMerge.get(duplicate.getAc());
                        // components without conflicts to move on the original protein
                        Collection<Component> componentToMove = CollectionUtils.subtract(duplicate.getActiveInstances(), componentToFix);

                        // report the interactions to move before moving them
                        reportMovedInteraction(duplicate, componentToMove, report);
                        // move components without conflicts
                        ComponentTools.moveComponents(originalProt, duplicate, evt.getDataContext(), processor, componentToMove);

                        report.getAddedAnnotations().put(duplicate.getAc(), addedAnnotations);

                        double relativeConservation = computesRequenceConservation(sequence, evt.getUniprotSequence());
                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64(), relativeConservation));

                    }
                    // we don't have feature conflicts, we can merge the proteins normally
                    else {
                        // report the interactions to move before moving them
                        reportMovedInteraction(duplicate, duplicate.getActiveInstances(), report);
                        // move the interactions
                        ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext(), processor);

                        // the duplicate will be deleted, add intact secondary references
                        Collection<InteractorXref> addedXRef = ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);
                        report.getAddedXRefs().put(originalProt.getAc(), addedXRef);

                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        if (ProteinTools.isSequenceChanged(sequence, evt.getUniprotSequence())){
                            double relativeConservation = computesRequenceConservation(sequence, evt.getUniprotSequence());
                            processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64(), relativeConservation));
                        }
                    }

                    // update isoforms and feature chains
                    Collection<String> updatedTranscripts = ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

                    report.getUpdatedTranscripts().put(duplicate.getAc(), updatedTranscripts);

                    // update duplicate
                    factory.getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate if no active instances are still attached to it
                    if (duplicate.getActiveInstances().isEmpty()) {
                        deleteProtein(new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                    }
                    else {
                        log.trace("The duplicate " + duplicate.getAc() + " still have " + duplicate.getActiveInstances().size() + " active instances and cannot be deleted.");
                    }
                }
                else {
                    // if the original protein contains range conflict, we need to demerge it to keep the bad ranges attached to a no-uniprot-update protein
                    if (proteinsNeedingPartialMerge.containsKey(originalProt.getAc())){
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                                "The duplicate " + duplicate.getAc() + " has been kept as original protein but a new protein has been created with " +
                                        proteinsNeedingPartialMerge.get(duplicate.getAc()).size() + " components with range conflicts.", UpdateError.original_protein_feature_conflicts, duplicate));
                    }
                }
            }
        }

        // update the original protein
        factory.getProteinDao().update((ProteinImpl) originalProt);

        return originalProt;
    }

    private double computesRequenceConservation(String oldSequence, String newSequence) {
        double relativeConservation = 0;
        if (oldSequence != null && newSequence != null){
            relativeConservation = ProteinTools.calculateSequenceConservation(oldSequence, newSequence);
        }
        return relativeConservation;
    }

    /**
     *
     * @param duplicates
     * @return  the first protein created
     */
    protected static Protein calculateOriginalProtein(List<? extends Protein> duplicates) {
        Protein originalProt = duplicates.get(0);

        for (int i = 1; i < duplicates.size(); i++) {
            Protein duplicate =  duplicates.get(i);

            if (duplicate.getCreated().before(originalProt.getCreated())) {
                originalProt = duplicate;
            }
        }

        return originalProt;
    }

    /**
     * Fire a delete event for this protein
     * @param evt
     */
    private void deleteProtein(ProteinEvent evt) {
        boolean isDeletedFromDatabase = proteinDeleter.delete(evt);

        if (!isDeletedFromDatabase && evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            processor.fireOnProcessErrorFound(new UpdateErrorEvent(evt.getSource(), evt.getDataContext(), "The protein " + evt.getProtein().getShortLabel() + " cannot be deleted because doesn't have any intact ac.", UpdateError.protein_with_ac_null_to_delete, evt.getProtein(), evt.getUniprotIdentity()));
        }
    }

    public ProteinDeleter getProteinDeleter() {
        return proteinDeleter;
    }

    public RangeFixer getRangeFixer() {
        return rangeFixer;
    }

    public OutOfDateParticipantFixer getDeprecatedParticipantFixer() {
        return deprecatedParticipantFixer;
    }

    private DuplicateReport processDuplicatedTranscript(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplEvt, Protein masterProtein, boolean isIsoform, Collection<ProteinTranscript> mergedTranscripts) {
        DuplicateReport report = fixProteinDuplicates(duplEvt);

        String originalAc = report.getOriginalProtein().getAc();

        for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
            if (originalAc.equals(p.getProtein().getAc())){
                report.setTranscript(p.getUniprotVariant());
            }
        }

        if (report.getTranscript() == null){
            for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                if (originalAc.equals(p.getProtein().getAc())){
                    report.setTranscript(p.getUniprotVariant());
                }
            }

            if (report.getTranscript() == null){
                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                    if (originalAc.equals(p.getProtein().getAc())){
                        report.setTranscript(p.getUniprotVariant());
                    }
                }
            }
        }

        // we had range conflicts during merge, we need to process them
        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                // the parent ac is the original protein ac if no master protein is given
                String validParentAc = report.getOriginalProtein() != null ? report.getOriginalProtein().getAc() : entry.getKey().getAc();

                if (masterProtein != null){
                    validParentAc = masterProtein.getAc();
                }

                // we want to create a deprecated protein if the protein with range confilcts is a protein transcripts because unisave does not keep information about isoforms sequence versions
                boolean enableCreationDeprecatedProtein = false;

                if (report.getOriginalProtein() != null){
                    if (report.getOriginalProtein().getAc().equalsIgnoreCase(entry.getKey().getAc())){
                        enableCreationDeprecatedProtein = true;
                    }
                }

                // try to fix the protein with range conflicts
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);

                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, enableCreationDeprecatedProtein);

                // protein has been fixed or a deprecated protein has been created
                if (fixedProtein != null){
                    rangeFixer.processInvalidRanges(fixedProtein.getProtein(), caseEvent, caseEvent.getQuerySentToService(), fixedProtein.getProtein().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                    if (fixedProtein.getUniprotVariant() == null){
                        fixedProtein.setUniprotVariant(report.getTranscript());
                    }

                    // if protein is deprecated, it means that the original protein had range conflicts, We don't need to add the deprecated protein to the list of proteins to update
                    boolean isFromUniprot = ProteinUtils.isFromUniprot(fixedProtein.getProtein());

                    if (isFromUniprot){
                        // we identified a new transcript (isoform)
                        if (IdentifierChecker.isSpliceVariantId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                            String ac = fixedProtein.getProtein().getAc();

                            if (ac != null){
                                boolean hasFoundSpliceVariant = false;

                                for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundSpliceVariant = true;
                                    }
                                }

                                if (!hasFoundSpliceVariant){
                                    for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                                        if (ac.equals(p.getProtein().getAc())){
                                            hasFoundSpliceVariant = true;
                                        }
                                    }

                                    if (!hasFoundSpliceVariant){
                                        caseEvent.getPrimaryIsoforms().add(fixedProtein);
                                        caseEvent.getProteins().add(fixedProtein.getProtein().getAc());

                                        if (isIsoform){
                                            mergedTranscripts.add(fixedProtein);
                                        }
                                    }
                                }
                            }
                        }
                        // we identified a new feature chain
                        else if (IdentifierChecker.isFeatureChainId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                            String ac = fixedProtein.getProtein().getAc();

                            if (ac != null){
                                boolean hasFoundChain = false;

                                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundChain = true;
                                    }
                                }

                                if (!hasFoundChain){
                                    caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                    caseEvent.getProteins().add(fixedProtein.getProtein().getAc());

                                    if (!isIsoform){
                                        mergedTranscripts.add(fixedProtein);
                                    }
                                }
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);
                }
            }
        }

        // we have a valid original protein
        if (report.getOriginalProtein() != null){
            if (report.hasShiftedRanges() && ProteinTools.isSequenceChanged(report.getOriginalProtein().getSequence(), report.getUniprotSequence())){
                String oldSequence = report.getOriginalProtein().getSequence();

                log.debug( "sequence of "+report.getOriginalProtein().getAc()+" requires update." );
                report.getOriginalProtein().setSequence( report.getUniprotSequence() );

                // CRC64
                String crc64 = report.getCrc64();
                if ( report.getOriginalProtein().getCrc64() == null || !report.getOriginalProtein().getCrc64().equals( crc64 ) ) {
                    log.debug( "CRC64 requires update." );
                    report.getOriginalProtein().setCrc64( crc64 );
                }

                caseEvent.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) report.getOriginalProtein());

                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                    double relativeConservation = computesRequenceConservation(oldSequence, report.getUniprotSequence());

                    processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, caseEvent.getDataContext(), report.getOriginalProtein(), oldSequence, report.getUniprotSequence(), report.getCrc64(), relativeConservation));
                }
            }
        }

        return report;
    }

    private DuplicateReport processDuplicatedProtein(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplicateEvent) {
        DuplicateReport report = fixProteinDuplicates(duplicateEvent);

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                // the valid parent ac is the ac of the protein having range conflicts
                String validParentAc = entry.getKey().getAc();

                // if merge not successful, we create a deprecated protein for original protein
                boolean enableCreationDeprecatedProtein = false;

                if (report.getOriginalProtein() != null){
                    validParentAc = report.getOriginalProtein().getAc();
                    if (report.getOriginalProtein().getAc().equalsIgnoreCase(entry.getKey().getAc())){
                        enableCreationDeprecatedProtein = true;
                    }
                }

                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);

                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, enableCreationDeprecatedProtein);

                if (fixedProtein != null){
                    rangeFixer.processInvalidRanges(fixedProtein.getProtein(), caseEvent, caseEvent.getQuerySentToService(), fixedProtein.getProtein().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                    // if uniprot variant is null, it means that a deprecated protein has been created because the protein with range conflicts is a master protein
                    if (fixedProtein.getUniprotVariant() != null){
                        if (IdentifierChecker.isSpliceVariantId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                            String ac = fixedProtein.getProtein().getAc();

                            if (ac != null){
                                boolean hasFoundSpliceVariant = false;

                                for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundSpliceVariant = true;
                                    }
                                }

                                if (!hasFoundSpliceVariant){
                                    for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                                        if (ac.equals(p.getProtein().getAc())){
                                            hasFoundSpliceVariant = true;
                                        }
                                    }

                                    if (!hasFoundSpliceVariant){
                                        caseEvent.getPrimaryIsoforms().add(fixedProtein);
                                        caseEvent.getProteins().add(fixedProtein.getProtein().getAc());
                                    }
                                }
                            }
                        }
                        else if (IdentifierChecker.isFeatureChainId(fixedProtein.getUniprotVariant().getPrimaryAc())){
                            String ac = fixedProtein.getProtein().getAc();

                            if (ac != null){
                                boolean hasFoundChain = false;

                                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                                    if (ac.equals(p.getProtein().getAc())){
                                        hasFoundChain = true;
                                    }
                                }

                                if (!hasFoundChain){
                                    caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                    caseEvent.getProteins().add(fixedProtein.getProtein().getAc());
                                }
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);
                }
            }
        }

        if (report.getOriginalProtein() != null){
            if (report.hasShiftedRanges() && ProteinTools.isSequenceChanged(report.getOriginalProtein().getSequence(), report.getUniprotSequence())){
                String oldSequence = report.getOriginalProtein().getSequence();

                log.debug( "sequence of "+report.getOriginalProtein().getAc()+" requires update." );
                report.getOriginalProtein().setSequence( report.getUniprotSequence() );

                // CRC64
                String crc64 = report.getCrc64();
                if ( report.getOriginalProtein().getCrc64() == null || !report.getOriginalProtein().getCrc64().equals( crc64 ) ) {
                    log.debug( "CRC64 requires update." );
                    report.getOriginalProtein().setCrc64( crc64 );
                }

                caseEvent.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) report.getOriginalProtein());

                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                    double relativeConservation = computesRequenceConservation(oldSequence, report.getUniprotSequence());

                    processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, caseEvent.getDataContext(), report.getOriginalProtein(), oldSequence, report.getUniprotSequence(), report.getCrc64(), relativeConservation));
                }
            }
        }

        return report;
    }

    public DuplicatesFinder getDuplicatesFinder() {
        return duplicatesFinder;
    }

    public void setDuplicatesFinder(DuplicatesFinder duplicatesFinder) {
        this.duplicatesFinder = duplicatesFinder;
    }

    private void reportMovedInteraction(Protein sourceProtein, Collection<Component> componentsToMove, DuplicateReport report) {
        Map<String, Collection<String>> movedInteractions = report.getMovedInteractions();

        if (!movedInteractions.containsKey(sourceProtein.getAc())){
            Collection<String> moved = new ArrayList<String>(componentsToMove.size());
            movedInteractions.put(sourceProtein.getAc(), moved);
        }

        for (Component c : componentsToMove){
            movedInteractions.get(sourceProtein.getAc()).add(c.getAc());
        }
    }
}