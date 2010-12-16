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
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.*;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
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
    private DuplicateReport fixProteinDuplicates(DuplicatesFoundEvent evt) throws ProcessorException {
        // merge protein duplicates and shift ranges when necessary
        DuplicateReport report = mergeDuplicates(evt.getProteins(), evt);

        // log in 'duplicates.csv'
        if (evt.getSource() instanceof ProteinUpdateProcessor) {
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnProteinDuplicationFound(evt);
        }

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
        }

        return masterProtein;
    }

    public void fixAllProteinTranscriptDuplicates(UpdateCaseEvent evt) throws ProcessorException {

        if (evt.getPrimaryProteins().size() == 1){
            Protein masterProtein = evt.getPrimaryProteins().iterator().next();

            if (evt.getPrimaryIsoforms().size() > 1){
                Collection<DuplicatesFoundEvent> duplicateEvents = duplicatesFinder.findIsoformDuplicates(evt);

                if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>();

                for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                    processDuplicatedTranscript(evt, mergedIsoforms, duplEvt, masterProtein);
                }

                if (!mergedIsoforms.isEmpty()){
                    evt.getPrimaryIsoforms().clear();
                    evt.getPrimaryIsoforms().addAll(mergedIsoforms);
                }
            }

            if (evt.getPrimaryFeatureChains().size() > 1){
                Collection<DuplicatesFoundEvent> duplicateEvents2 = duplicatesFinder.findFeatureChainDuplicates(evt);

                if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>();

                for (DuplicatesFoundEvent duplEvt : duplicateEvents2){
                    processDuplicatedTranscript(evt, mergedChains, duplEvt, masterProtein);
                }

                if (!mergedChains.isEmpty()){
                    evt.getPrimaryFeatureChains().clear();
                    evt.getPrimaryFeatureChains().addAll(mergedChains);
                }
            }
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
        List<Protein> duplicatesHavingSameSequence = new ArrayList<Protein>();

        // the collection which will contain the duplicates having different sequences
        List<Protein> duplicatesHavingDifferentSequence = new ArrayList<Protein>();

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
                        // log in 'process_errors.csv'
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(),
                                    "The protein " + p.getAc() + " contains " +
                                            componentWithRangeConflicts.size() + " components with range conflicts.", UpdateError.feature_conflicts, p));
                        }

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
    private void addAnnotationsForBadParticipant(Protein protein, String previousAc, DaoFactory factory){

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            factory.getCvObjectDao(CvTopic.class).persist(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).persist(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution = false;
        String cautionMessage = "The protein could not be merged with " + previousAc + " because of some conflicts with the protein sequence (features which cannot be shifted).";

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
        }

        if (!has_caution){
            Annotation demerge = new Annotation(caution, cautionMessage);
            annotationDao.persist(demerge);

            protein.addAnnotation(demerge);
        }
    }

    /**
     * Merge the duplicates, the interactions are moved (not the cross references as they will be deleted)
     * @param duplicates
     */
    protected Protein merge(List<Protein> duplicates, Map<String, Collection<Component>> proteinsNeedingPartialMerge, DuplicatesFoundEvent evt, boolean isSequenceChanged, DuplicateReport report) {
        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(duplicates);
        // set the protein kept from the merge
        evt.setReferenceProtein(originalProt);

        // the merge can be done without looking at the sequence of the duplicates
        if (!isSequenceChanged){
            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // move the interactions
                    ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext().getDaoFactory());

                    // add the intact secondary references
                    ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);

                    // update the protein transcripts if necessary
                    ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

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
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {
                // sequence of the duplicate
                String sequence = duplicate.getSequence();

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // we have feature conflicts for this protein which cannot be merged
                    if (proteinsNeedingPartialMerge.containsKey(duplicate.getAc())){
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                                "The duplicate " + duplicate.getAc() + " cannot be merged with " +
                                        originalProt.getAc() + " because we have " +
                                        proteinsNeedingPartialMerge.get(duplicate.getAc()).size() + " components with range conflicts.", UpdateError.impossible_merge, duplicate));
                        // add no-uniprot-update and caution
                        addAnnotationsForBadParticipant(duplicate, originalProt.getAc(), factory);
                        // components to let on the current protein
                        Collection<Component> componentToFix = proteinsNeedingPartialMerge.get(duplicate.getAc());
                        // components without conflicts to move on the original protein
                        Collection<Component> componentToMove = CollectionUtils.subtract(duplicate.getActiveInstances(), componentToFix);

                        // move components without conflicts
                        for (Component component : componentToMove) {

                            duplicate.removeActiveInstance(component);

                            if (duplicate.getActiveInstances().contains(component)){
                                factory.getComponentDao().delete(component);
                            }
                            else {
                                originalProt.addActiveInstance(component);
                                factory.getComponentDao().update(component);
                            }
                        }

                        // update protein
                        factory.getProteinDao().update((ProteinImpl) duplicate);

                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64()));

                    }
                    // we don't have feature conflicts, we can merge the proteins normally
                    else {
                        // move the interactions
                        ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext().getDaoFactory());

                        // the duplicate will be deleted, add intact secondary references
                        ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);

                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        if (ProteinTools.isSequenceChanged(sequence, evt.getUniprotSequence())){
                            processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64()));
                        }
                    }

                    // update isoforms and feature chains
                    ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

                    // update duplicate
                    factory.getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate if no active instances are still attached to it
                    if (duplicate.getActiveInstances().isEmpty()) {
                        deleteProtein(new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                    }
                }
                else {
                    // if the original protein contains range conflict, we need to demerge it to keep the bad ranges attached to a no-uniprot-update protein
                    if (proteinsNeedingPartialMerge.containsKey(originalProt.getAc())){
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                                "The duplicate " + duplicate.getAc() + " has been kept as original protein but a new protein has been created with " +
                                        proteinsNeedingPartialMerge.get(duplicate.getAc()).size() + " components with range conflicts.", UpdateError.feature_conflicts, duplicate));
                        Collection<Component> componentsToFix = new ArrayList<Component>();
                        componentsToFix.addAll(proteinsNeedingPartialMerge.get(originalProt.getAc()));

                        // create a deprecated protein and attach interactions with range conflicts to this protein
                        Protein protWithRangeConflicts = this.deprecatedParticipantFixer.createDeprecatedProtein(new OutOfDateParticipantFoundEvent(evt.getSource(), evt.getDataContext(), componentsToFix, originalProt, null, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, originalProt.getAc()), true).getProtein();

                        // update the report to attach the component with range conflicts to the deprecated protein and not the original protein
                        RangeUpdateReport rangeReport = report.getComponentsWithFeatureConflicts().get(originalProt);
                        report.getComponentsWithFeatureConflicts().remove(originalProt);

                        report.getComponentsWithFeatureConflicts().put(protWithRangeConflicts, rangeReport);
                    }

                    // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                    if (ProteinTools.isSequenceChanged(sequence, evt.getUniprotSequence())){
                        log.debug( "sequence of "+duplicate.getAc()+" requires update." );
                        duplicate.setSequence( evt.getUniprotSequence() );

                        // CRC64
                        String crc64 = evt.getUniprotCrc64();
                        if ( duplicate.getCrc64() == null || !duplicate.getCrc64().equals( crc64 ) ) {
                            log.debug( "CRC64 requires update." );
                            duplicate.setCrc64( crc64 );
                        }

                        factory.getProteinDao().update((ProteinImpl) duplicate);
                        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64()));
                    }
                }
            }
        }

        // update the original protein
        factory.getProteinDao().update((ProteinImpl) originalProt);

        return originalProt;
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
        proteinDeleter.delete(evt);
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

    private void processDuplicatesTranscript(UpdateCaseEvent caseEvent, Collection<ProteinTranscript> mergedTranscripts, DuplicatesFoundEvent duplEvt, Protein masterProtein) {
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

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                String validParentAc = entry.getKey().getAc();

                if (masterProtein != null){
                    validParentAc = masterProtein.getAc();
                }

                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);
                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                if (fixedProtein != null){

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
                                    mergedTranscripts.add(fixedProtein);
                                    caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                        processor.fireNonUniprotProteinFound(new ProteinEvent(processor, caseEvent.getDataContext(), entry.getKey()));
                    }
                }
            }
        }

        if (report.getOriginalProtein() != null){
            mergedTranscripts.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
        }
    }

    private DuplicateReport processDuplicatedProtein(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplicateEvent) {
        DuplicateReport report = fixProteinDuplicates(duplicateEvent);

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                String validParentAc = entry.getKey().getAc();

                if (report.getOriginalProtein() != null){
                    validParentAc = report.getOriginalProtein().getAc();
                }
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);
                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                if (fixedProtein != null){

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
                                    caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                        processor.fireNonUniprotProteinFound(new ProteinEvent(processor, caseEvent.getDataContext(), entry.getKey()));
                    }
                }
            }
        }
        return report;
    }

    private void processDuplicatedTranscript(UpdateCaseEvent caseEvent, Collection<ProteinTranscript> mergedTranscripts, DuplicatesFoundEvent duplEvt, Protein masterProtein) {
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

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                String validParentAc = entry.getKey().getAc();

                if (masterProtein != null){
                    validParentAc = masterProtein.getAc();
                }

                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);
                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

                if (fixedProtein != null){

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
                                    mergedTranscripts.add(fixedProtein);
                                    caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                            }
                        }
                    }

                    if (entry.getKey().getActiveInstances().isEmpty()){
                        ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                        processor.fireNonUniprotProteinFound(new ProteinEvent(processor, caseEvent.getDataContext(), entry.getKey()));
                    }
                }
            }
        }

        if (report.getOriginalProtein() != null){
            mergedTranscripts.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
        }
    }

    public DuplicatesFinder getDuplicatesFinder() {
        return duplicatesFinder;
    }

    public void setDuplicatesFinder(DuplicatesFinder duplicatesFinder) {
        this.duplicatesFinder = duplicatesFinder;
    }
}