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
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.util.ComponentTools;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
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

    public DuplicatesFixerImpl(ProteinDeleter proteinDeleter, OutOfDateParticipantFixer participantFixer, DuplicatesFinder duplicateFinder){
        if (proteinDeleter != null) {
            this.proteinDeleter = proteinDeleter;
        }
        else {
            this.proteinDeleter = new ProteinDeleterImpl();
        }

        if (deprecatedParticipantFixer != null) {
            this.deprecatedParticipantFixer = participantFixer;

            if (participantFixer.getRangeFixer() == null){
                this.rangeFixer = new RangeFixerImpl();
                participantFixer.setRangeFixer(this.rangeFixer);
            }
            else{
                this.rangeFixer = participantFixer.getRangeFixer();
            }
        }
        else {
            if (this.rangeFixer == null){
                this.rangeFixer = new RangeFixerImpl();
            }

            this.deprecatedParticipantFixer = new OutOfDateParticipantFixerImpl(this.rangeFixer);
        }

        if (duplicateFinder != null) {
            this.duplicatesFinder = duplicateFinder;
        }
        else {
            this.duplicatesFinder = new DuplicatesFinderImpl();
        }
    }

    /**
     * set the protein kept from the merge and the list of components having feature conflicts
     * and which couldn't be merged in the duplicateFoundEvent
     * @param evt : contains the list of duplicated proteins
     * @throws ProcessorException
     */
    public void fixProteinDuplicates(DuplicatesFoundEvent evt) throws ProcessorException {
        // merge protein duplicates and shift ranges when necessary
        mergeDuplicates(evt.getProteins(), evt);
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
            processDuplicatedProtein(evt, duplicateEvent);

            // the master protein is the result of the merge
            if (duplicateEvent.getReferenceProtein() != null){
                masterProtein = duplicateEvent.getReferenceProtein();

                // all the duplicated proteins have been fixed, clear the list of primary proteins and secondary proteins to update and put only the master protein
                evt.getPrimaryProteins().clear();
                evt.getSecondaryProteins().clear();
                evt.getPrimaryProteins().add(masterProtein);

                // updated the original protein
                evt.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) masterProtein );
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

        UniprotProtein masterUniprot = evt.getProtein();

        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
        Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>(evt.getPrimaryIsoforms().size() + evt.getSecondaryIsoforms().size());

        for (DuplicatesFoundEvent duplEvt : duplicateEvents){
            processDuplicatedTranscript(evt, duplEvt, masterProtein, true, mergedIsoforms);

            if (duplEvt.getReferenceProtein() != null){

                UniprotProteinTranscript uniprotTranscript = UniprotProteinRetrieverImpl.findUniprotSpliceVariant(duplEvt.getPrimaryUniprotAc(), masterUniprot);

                mergedIsoforms.add(new ProteinTranscript(duplEvt.getReferenceProtein(), uniprotTranscript));
                // updated the original protein
                evt.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) duplEvt.getReferenceProtein() );
            }

            // log in 'duplicates.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                processor.fireOnProteinDuplicationFound(duplEvt);
            }
        }

        if (!mergedIsoforms.isEmpty()){
            evt.getPrimaryIsoforms().clear();
            evt.getSecondaryIsoforms().clear();
            evt.getPrimaryIsoforms().addAll(mergedIsoforms);
        }

        Collection<DuplicatesFoundEvent> duplicateEvents2 = duplicatesFinder.findFeatureChainDuplicates(evt);

        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
        Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>(evt.getPrimaryFeatureChains().size());

        for (DuplicatesFoundEvent duplEvt : duplicateEvents2){
            processDuplicatedTranscript(evt, duplEvt, masterProtein, false, mergedChains);
            if (duplEvt.getReferenceProtein() != null){
                UniprotProteinTranscript uniprotTranscript = UniprotProteinRetrieverImpl.findUniprotFeatureChain(duplEvt.getPrimaryUniprotAc(), masterUniprot);

                mergedChains.add(new ProteinTranscript(duplEvt.getReferenceProtein(), uniprotTranscript));

                // updated the original protein
                evt.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) duplEvt.getReferenceProtein() );
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
     *
     * @param duplicates
     * @returna map of duplicated proteins sorted per taxId
     */
    private Map<String, List<Protein>> sortDuplicatesPerOrganism(Collection<Protein> duplicates){
        Map<String, List<Protein>> duplicatesSortedByOrganism = new HashMap<String, List<Protein>>(duplicates.size());

        for (Protein prot : duplicates){
            BioSource biosource = prot.getBioSource();
            String taxId = "";

            if (biosource != null){
                taxId = biosource.getTaxId() != null ? biosource.getTaxId() : "";
            }

            if (duplicatesSortedByOrganism.containsKey(taxId)){
                duplicatesSortedByOrganism.get(taxId).add(prot);
            }
            else {
                List<Protein> duplicatesAsList = new ArrayList<Protein>();
                duplicatesAsList.add(prot);

                duplicatesSortedByOrganism.put(taxId, duplicatesAsList);
            }
        }

        return duplicatesSortedByOrganism;
    }

    /**
     * Merge the duplicates, the interactions are moved from the duplicate to the original protein.
     * If there are feature range conflucts, the duplicate is not merged and the interactions having range conflicts are still attached to the duplicate
     * @param duplicates : the list of duplicates to merge
     * @param evt : the event
     */
    private void mergeDuplicates(Collection<Protein> duplicates, DuplicatesFoundEvent evt) {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        String uniprotOrganism = evt.getUniprotTaxId() != null ? evt.getUniprotTaxId() : "";

        if (log.isDebugEnabled()) log.debug("Merging duplicates: "+ DebugUtil.acList(duplicates));

        // the collection which will contain the duplicates having the same sequence
        List<Protein> duplicatesHavingSameSequence = new ArrayList<Protein>(duplicates.size());

        // the collection which will contain the duplicates having different sequences
        List<Protein> duplicatesHavingDifferentSequence = new ArrayList<Protein>(duplicates.size());

        // sort the duplicates per organism
        Map<String, List<Protein>> duplicatesSortedByOrganism = sortDuplicatesPerOrganism(duplicates);

        if (duplicatesSortedByOrganism.size() != 1){
            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                StringBuffer reason = new StringBuffer();
                reason.append("Impossible to merge proteins having different taxIds : ");

                int index = 0;
                for (String taxId : duplicatesSortedByOrganism.keySet()){
                    reason.append(taxId);

                    if (index < duplicatesSortedByOrganism.size() - 1){
                        reason.append(", ");
                    }
                    index ++;
                }

                ProteinUpdateError impossibleMerge = errorFactory.createImpossibleMergeError(null, null, evt.getPrimaryUniprotAc(), reason.toString());
                processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(), impossibleMerge, evt.getPrimaryUniprotAc()));
            }
        }

        // for each duplicated proteins sorted per organism, merge. If two proteins have same uniprot but different taxids, they are not merged together
        for (Map.Entry<String, List<Protein>> entry : duplicatesSortedByOrganism.entrySet()){

            duplicatesHavingDifferentSequence.clear();
            duplicatesHavingSameSequence.clear();

            List<Protein> duplicatesAsList = entry.getValue();

            boolean isUpdatable = entry.getKey().equals(uniprotOrganism);

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
                    duplicatesHavingDifferentSequence.add(merge(duplicatesHavingSameSequence, Collections.EMPTY_MAP, evt, false));
                }
                // the duplicates didn't have the same sequence, we add it to the list of duplicates having different sequences only if the proteins can be updated with the uniprot entry. No organism conflicts
                else if (isUpdatable){
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

                        if (!rangeReport.getShiftedRanges().isEmpty() || (rangeReport.getInvalidComponents().isEmpty() && !rangeReport.getUpdatedFeatureAnnotations().isEmpty())){
                            evt.getUpdatedRanges().put(p.getAc(), rangeReport);
                        }
                        // get the list of components with feature range conflicts
                        Collection<Component> componentWithRangeConflicts = rangeReport.getInvalidComponents().keySet();

                        // if we have feature range conflicts before the merge
                        if (!componentWithRangeConflicts.isEmpty()){

                            log.info( "We found " + componentWithRangeConflicts.size() + " components with feature conflicts for the protein " + p.getAc() );
                            // add this duplicate with the list of components with conflicts to the map of protein which couldn't be merged
                            proteinNeedingPartialMerge.put(p.getAc(), componentWithRangeConflicts);
                            // add the components with feature conflicts in the report
                            evt.getComponentsWithFeatureConflicts().put(p, rangeReport);
                        }
                    }

                    // we merge the proteins taking into account the possible feature conflicts
                    Protein finalProt = merge(duplicatesHavingDifferentSequence, proteinNeedingPartialMerge, evt, true);
                    log.info( "The protein " + finalProt.getAc() + "has been kept as original protein.");
                    // we set the original protein in the report
                    evt.setReferenceProtein(finalProt);
                }
                // we cannot merge because we don't have a uniprot sequence as reference for range shifting. Log in 'process_errors.csv'
                else {
                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                        for (Protein prot : duplicatesHavingDifferentSequence){
                            ProteinUpdateError impossibleMerge = errorFactory.createImpossibleMergeError(prot.getAc(), null, evt.getPrimaryUniprotAc(), " The uniprot sequence is null and we need a valid uniprot sequence to merge proteins having different sequences");
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), impossibleMerge, prot, evt.getPrimaryUniprotAc()));
                        }
                    }
                    log.error("It is impossible to merge all the duplicates ("+duplicatesHavingDifferentSequence.size()+") because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.");
                }
            }
            // all the duplicates had the same sequence and have been merged. Set the original protein in the report
            else if (duplicatesHavingDifferentSequence.size() == 1){
                evt.setReferenceProtein(duplicatesHavingDifferentSequence.iterator().next());
            }
        }
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
    protected Protein merge(List<Protein> duplicates, Map<String, Collection<Component>> proteinsNeedingPartialMerge, DuplicatesFoundEvent evt, boolean isSequenceChanged) {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorfactory = config.getErrorFactory();

        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // calculate the original protein (the oldest is kept as original)
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
                    Set<String> movedInteractions = ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource(), evt.getPrimaryUniprotAc());

                    // report the interactions to move
                    reportMovedInteraction(duplicate, movedInteractions, evt);

                    // add the intact secondary references
                    Collection<InteractorXref> addedXRef = ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);

                    // update the protein transcripts if necessary
                    Collection<String> updatedTranscripts = ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

                    evt.getMovedXrefs().put(duplicate.getAc(), addedXRef);
                    evt.getUpdatedTranscripts().put(duplicate.getAc(), updatedTranscripts);

                    // the duplicate will be deleted
                    //factory.getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate if no active instances are attached to it
                    if (duplicate.getActiveInstances().isEmpty()) {
                        ProteinEvent protEvt = new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc());
                        protEvt.setUniprotIdentity(evt.getPrimaryUniprotAc());
                        deleteProtein(protEvt);
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
            evt.setHasShiftedRanges(true);
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {
                // sequence of the duplicate
                String sequence = duplicate.getSequence();

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // we have feature conflicts for this protein which cannot be merged and becomes deprecated
                    if (proteinsNeedingPartialMerge.containsKey(duplicate.getAc())){
                        ProteinUpdateError impossibleMerge = errorfactory.createImpossibleMergeError(duplicate.getAc(), originalProt.getAc(), evt.getPrimaryUniprotAc(), "the duplicated protein has " +
                                proteinsNeedingPartialMerge.get(duplicate.getAc()).size() + " components with range conflicts. The protein is now deprecated.");
                        processor.fireOnProcessErrorFound(new UpdateErrorEvent(processor, evt.getDataContext(),
                                impossibleMerge, duplicate, evt.getPrimaryUniprotAc()));

                        // add no-uniprot-update and caution
                        Collection<Annotation> addedAnnotations = addAnnotationsForBadParticipant(duplicate, originalProt.getAc(), factory);
                        // components to let on the current protein
                        Collection<Component> componentToFix = proteinsNeedingPartialMerge.get(duplicate.getAc());
                        // components without conflicts to move on the original protein
                        Collection<Component> componentToMove = CollectionUtils.subtract(duplicate.getActiveInstances(), componentToFix);

                        Set<String> movedInteractions = Collections.EMPTY_SET;
                        // move components without conflicts
                        if (!componentToMove.isEmpty()){
                            movedInteractions = ComponentTools.moveComponents(originalProt, duplicate, evt.getDataContext(), processor, componentToMove, evt.getPrimaryUniprotAc());
                        }

                        // report the interactions to move before moving them
                        reportMovedInteraction(duplicate, movedInteractions, evt);

                        evt.getAddedAnnotations().put(duplicate.getAc(), addedAnnotations);

                        // the sequence is not updated because of range conflicts
                        //double relativeConservation = computesRequenceConservation(sequence, evt.getUniprotSequence());
                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        //processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getPrimaryUniprotAc(), evt.getUniprotSequence(), evt.getUniprotCrc64(), relativeConservation));

                        // update duplicate which will be kept because of range conflicts
                        factory.getProteinDao().update((ProteinImpl) duplicate);
                    }
                    // we don't have feature conflicts, we can merge the proteins normally
                    else {

                        // move the interactions
                        Set<String> movedInteractions = ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate, evt.getDataContext(), processor, evt.getPrimaryUniprotAc());

                        // report the interactions to move before moving them
                        reportMovedInteraction(duplicate, movedInteractions, evt);

                        // the duplicate will be deleted, add intact secondary references
                        Collection<InteractorXref> addedXRef = ProteinTools.addIntactSecondaryReferences(originalProt, duplicate, factory);
                        evt.getMovedXrefs().put(duplicate.getAc(), addedXRef);

                        // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                        if (ProteinTools.isSequenceChanged(sequence, evt.getUniprotSequence())){
                            double relativeConservation = computesRequenceConservation(sequence, evt.getUniprotSequence());
                            processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), duplicate, sequence, evt.getPrimaryUniprotAc(), evt.getUniprotSequence(), evt.getUniprotCrc64(), relativeConservation));
                        }
                    }

                    // update isoforms and feature chains
                    Collection<String> updatedTranscripts = ProteinTools.updateProteinTranscripts(factory, originalProt, duplicate);

                    evt.getUpdatedTranscripts().put(duplicate.getAc(), updatedTranscripts);

                    // and delete the duplicate if no active instances are still attached to it
                    if (duplicate.getActiveInstances().isEmpty()) {
                        ProteinEvent protEvt = new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc());
                        protEvt.setUniprotIdentity(evt.getPrimaryUniprotAc());
                        deleteProtein(protEvt);
                    }
                    else {
                        log.trace("The duplicate " + duplicate.getAc() + " still have " + duplicate.getActiveInstances().size() + " active instances and cannot be deleted.");
                    }
                }
            }
        }

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
            ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
            ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            ProteinUpdateError impossibleToDelete = errorFactory.createImpossibleToDeleteError(evt.getProtein().getShortLabel(), "The protein " + evt.getProtein().getShortLabel() + " cannot be deleted because doesn't have any intact ac.");
            processor.fireOnProcessErrorFound(new UpdateErrorEvent(evt.getSource(), evt.getDataContext(), impossibleToDelete, evt.getProtein(), evt.getUniprotIdentity()));
        }
    }

    public ProteinDeleter getProteinDeleter() {
        return proteinDeleter;
    }

    @Override
    public void setProteinDeleter(ProteinDeleter deleter) {
        this.proteinDeleter = deleter;
    }

    public RangeFixer getRangeFixer() {
        return rangeFixer;
    }

    @Override
    public void setRangeFixer(RangeFixer rangeFixer) {
        this.rangeFixer = rangeFixer;
    }

    public OutOfDateParticipantFixer getDeprecatedParticipantFixer() {
        return deprecatedParticipantFixer;
    }

    @Override
    public void setDeprecatedParticipantFixer(OutOfDateParticipantFixer participantFixer) {
        this.deprecatedParticipantFixer = participantFixer;
    }

    private void processDuplicatedTranscript(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplEvt, Protein masterProtein, boolean isIsoform, Collection<ProteinTranscript> mergedTranscripts) {
        fixProteinDuplicates(duplEvt);

        // we had range conflicts during merge, we need to process them
        if (!duplEvt.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : duplEvt.getComponentsWithFeatureConflicts().entrySet()){
                // the parent ac is the original protein ac if no master protein is given
                String validParentAc = duplEvt.getReferenceProtein() != null ? duplEvt.getReferenceProtein().getAc() : entry.getKey().getAc();

                if (masterProtein != null){
                    validParentAc = masterProtein.getAc();
                }

                // we want to create a deprecated protein if the protein with range confilcts is a protein transcripts because unisave does not keep information about isoforms sequence versions
                boolean enableCreationDeprecatedProtein = false;

                if (duplEvt.getReferenceProtein() != null){
                    if (duplEvt.getReferenceProtein().getAc().equalsIgnoreCase(entry.getKey().getAc())){
                        enableCreationDeprecatedProtein = true;
                    }
                }

                // try to fix the protein with range conflicts
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), caseEvent.getProtein(), entry.getValue(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);

                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, enableCreationDeprecatedProtein, false);

                // protein has been fixed or a deprecated protein has been created
                if (fixedProtein != null){

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
                        ProteinEvent protEvt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate");
                        protEvt.setUniprotIdentity(duplEvt.getPrimaryUniprotAc());

                        proteinDeleter.delete(protEvt);
                    }
                }
            }
        }

        // we have a valid original protein
        if (duplEvt.getReferenceProtein() != null){
            if (duplEvt.hasShiftedRanges() && ProteinTools.isSequenceChanged(duplEvt.getReferenceProtein().getSequence(), duplEvt.getUniprotSequence())){
                String oldSequence = duplEvt.getReferenceProtein().getSequence();

                log.debug( "sequence of "+duplEvt.getReferenceProtein().getAc()+" requires update." );
                duplEvt.getReferenceProtein().setSequence( duplEvt.getUniprotSequence() );

                // CRC64
                String crc64 = duplEvt.getUniprotCrc64();
                if ( duplEvt.getReferenceProtein().getCrc64() == null || !duplEvt.getReferenceProtein().getCrc64().equals( crc64 ) ) {
                    log.debug( "CRC64 requires update." );
                    duplEvt.getReferenceProtein().setCrc64( crc64 );
                }

                caseEvent.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) duplEvt.getReferenceProtein());

                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                    double relativeConservation = computesRequenceConservation(oldSequence, duplEvt.getUniprotSequence());

                    processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, caseEvent.getDataContext(), duplEvt.getReferenceProtein(), duplEvt.getPrimaryUniprotAc(), oldSequence, duplEvt.getUniprotSequence(), duplEvt.getUniprotCrc64(), relativeConservation));
                }
            }
        }
    }

    private void processDuplicatedProtein(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplicateEvent) {
        fixProteinDuplicates(duplicateEvent);

        if (!duplicateEvent.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : duplicateEvent.getComponentsWithFeatureConflicts().entrySet()){
                // the valid parent ac is the ac of the protein having range conflicts
                String validParentAc = entry.getKey().getAc();

                // if merge not successful, we create a deprecated protein for original protein
                boolean enableCreationDeprecatedProtein = false;

                if (duplicateEvent.getReferenceProtein() != null){
                    validParentAc = duplicateEvent.getReferenceProtein().getAc();
                    if (duplicateEvent.getReferenceProtein().getAc().equalsIgnoreCase(entry.getKey().getAc())){
                        enableCreationDeprecatedProtein = true;
                    }
                }

                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), caseEvent.getProtein(), entry.getValue(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains(), validParentAc);

                ProteinTranscript fixedProtein = deprecatedParticipantFixer.fixParticipantWithRangeConflicts(participantEvt, enableCreationDeprecatedProtein, false);

                if (fixedProtein != null){

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
                        ProteinEvent protEvt = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate");
                        protEvt.setUniprotIdentity(duplicateEvent.getPrimaryUniprotAc());

                        proteinDeleter.delete(protEvt);
                    }
                }
            }
        }

        if (duplicateEvent.getReferenceProtein() != null){
            if (duplicateEvent.hasShiftedRanges() && ProteinTools.isSequenceChanged(duplicateEvent.getReferenceProtein().getSequence(), duplicateEvent.getUniprotSequence())){
                String oldSequence = duplicateEvent.getReferenceProtein().getSequence();

                log.debug( "sequence of "+duplicateEvent.getReferenceProtein().getAc()+" requires update." );
                duplicateEvent.getReferenceProtein().setSequence( duplicateEvent.getUniprotSequence() );

                // CRC64
                String crc64 = duplicateEvent.getUniprotCrc64();
                if ( duplicateEvent.getReferenceProtein().getCrc64() == null || !duplicateEvent.getReferenceProtein().getCrc64().equals( crc64 ) ) {
                    log.debug( "CRC64 requires update." );
                    duplicateEvent.getReferenceProtein().setCrc64( crc64 );
                }

                caseEvent.getDataContext().getDaoFactory().getProteinDao().update((ProteinImpl) duplicateEvent.getReferenceProtein());

                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                    double relativeConservation = computesRequenceConservation(oldSequence, duplicateEvent.getUniprotSequence());

                    processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, caseEvent.getDataContext(), duplicateEvent.getReferenceProtein(), duplicateEvent.getPrimaryUniprotAc(), oldSequence, duplicateEvent.getUniprotSequence(), duplicateEvent.getUniprotCrc64(), relativeConservation));
                }
            }
        }
    }

    public DuplicatesFinder getDuplicatesFinder() {
        return duplicatesFinder;
    }

    public void setDuplicatesFinder(DuplicatesFinder duplicatesFinder) {
        this.duplicatesFinder = duplicatesFinder;
    }

    private void reportMovedInteraction(Protein sourceProtein, Set<String> movedInteractionAcs, DuplicatesFoundEvent evt) {
        Map<String, Set<String>> movedInteractions = evt.getMovedInteractions();

        if (!movedInteractions.containsKey(sourceProtein.getAc())){
            movedInteractions.put(sourceProtein.getAc(), new HashSet(movedInteractionAcs));
        }
        else {
            movedInteractions.get(sourceProtein.getAc()).addAll(movedInteractionAcs);
        }
    }
}