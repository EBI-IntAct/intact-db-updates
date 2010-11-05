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
package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.*;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFixer extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    @Override
    public void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException {
        mergeDuplicates(evt.getProteins(), evt);
    }

    /**
     * Merge tha duplicates, the interactions are moved and the cross references as well
     * @param duplicates
     * @param evt
     */
    public void mergeDuplicates(Collection<Protein> duplicates, DuplicatesFoundEvent evt) {
        if (log.isDebugEnabled()) log.debug("Merging duplicates: "+ DebugUtil.acList(duplicates));

        // add the interactions from the duplicated proteins to the protein
        // that was created first in the database
        List<Protein> duplicatesAsList = new ArrayList<Protein>(duplicates);


        // the collection which will contain the duplicates
        List<Protein> duplicatesHavingSameSequence = new ArrayList<Protein>();

        List<Protein> duplicatesHavingDifferentSequence = new ArrayList<Protein>();

        // while the list of possible duplicates has not been fully treated, we need to check the duplicates
        while (duplicatesAsList.size() > 0){
            duplicatesHavingSameSequence.clear();

            // pick the first protein of the list and add it in the list of duplicates
            Iterator<Protein> iterator = duplicatesAsList.iterator();
            Protein protToCompare = iterator.next();
            duplicatesHavingSameSequence.add(protToCompare);

            String originalSequence = protToCompare.getSequence();

            // we compare the sequence of this first protein against the sequence of the other proteins
            while (iterator.hasNext()){
                // we extract the sequence of the next protein to compare
                Protein proteinCompared = iterator.next();
                String sequenceToCompare = proteinCompared.getSequence();

                // if the sequences are identical, we add the protein to the list of duplicates
                if (originalSequence != null && sequenceToCompare != null){
                    if (originalSequence.equalsIgnoreCase(sequenceToCompare)){
                        duplicatesHavingSameSequence.add(proteinCompared);
                    }
                    else {
                        if (log.isDebugEnabled()) log.debug( "The sequences of " + protToCompare.getAc() + " and " + proteinCompared.getAc() + " are different. Before merging them, we will shift the ranges of each protein separately using" +
                                " the uniprot protein sequence as sequence of reference.");
                    }
                }
                else {
                    if (log.isDebugEnabled()) log.debug( "The sequences of " + protToCompare.getAc() + " and " + proteinCompared.getAc() + " are different. Before merging them, we will shift the ranges of each protein separately using" +
                            " the uniprot protein sequence as sequence of reference.");
                }
            }

            // if we have more than two proteins in the duplicate list having the exact same sequence, we merge them
            if (duplicatesHavingSameSequence.size() > 1){
                duplicatesHavingDifferentSequence.add(merge(duplicatesHavingSameSequence, Collections.EMPTY_MAP, evt, false));
            }
            else{
                duplicatesHavingDifferentSequence.addAll(duplicatesHavingSameSequence);
            }

            // we remove the processed proteins from the list of protein to process
            duplicatesAsList.removeAll(duplicatesHavingSameSequence);
        }

        // we still have to merge duplicates having different sequences
        if (duplicatesHavingDifferentSequence.size() > 1){
            // the uniprot protein has been found previously
            if (evt.getUniprotSequence() != null){
                // in case of feature conflicts, we will only attach the interactions without feature conflicts to the original protein and keep the
                // duplicate as no-uniprot-update with the interactions having feature conflicts
                Map<String, Collection<Component>> proteinNeedingPartialMerge = new HashMap<String, Collection<Component>>();

                // we try to shift the ranges of each protein to merge and collect the components with feature conflicts
                for (Protein p : duplicatesHavingDifferentSequence){
                    Collection<Component> componentWithRangeConflicts = collectComponentsWithFeatureConflicts(p, evt.getUniprotSequence(), evt);

                    if (!componentWithRangeConflicts.isEmpty()){
                        log.info( "We found " + componentWithRangeConflicts.size() + " components with feature conflicts for the protein " + p.getAc() );
                        proteinNeedingPartialMerge.put(p.getAc(), componentWithRangeConflicts);
                    }
                }

                // we merge the proteins taking into account the possible feature conflicts
                Protein finalProt = merge(duplicatesHavingDifferentSequence, proteinNeedingPartialMerge, evt, true);
                log.info( "The protein " + finalProt.getAc() + "has been kept as original protein.");

            }
            // we cannot merge because we don't have a uniprot sequence as reference for range shifting
            else {
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "It is impossible to merge all the duplicates ("+duplicatesHavingDifferentSequence.size()+") because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.", UpdateError.impossible_merge));
                }
                log.error("It is impossible to merge all the duplicates ("+duplicatesHavingDifferentSequence.size()+") because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.");
            }
        }
    }

    /**
     *
     * @param sequenceDuplicate : sequence of the duplicate
     * @param sequenceUniprot : sequence of the protein in uniprot
     * @return true if the sequence in uniprot is different from the sequence of the duplicate.
     */
    private boolean isSequenceChanged(String sequenceDuplicate, String sequenceUniprot){
        if ( (sequenceDuplicate == null && sequenceUniprot != null)) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Sequence requires update." );
            }
            return true;
        }
        else if (sequenceDuplicate != null && sequenceUniprot != null){
            if (!sequenceUniprot.equals( sequenceDuplicate ) ){
                if ( log.isDebugEnabled() ) {
                    log.debug( "Sequence requires update." );
                }
                return true;
            }
        }

        return false;
    }

    /**
     * Shift the ranges attached to this protein whenever it is possible using the uniprot protein sequence as sequence reference
     * @param protein : the protein we want to merge and we want to shift the ranges
     * @param uniprotSequence : the uniprot protein sequence as a reference for range shifting
     * @param evt : the evt
     * @return the list of components with feature conflicts
     */
    protected Collection<Component> collectComponentsWithFeatureConflicts(Protein protein, String uniprotSequence, DuplicatesFoundEvent evt){

        RangeChecker checker = new RangeChecker();
        DataContext context = IntactContext.getCurrentInstance().getDataContext();

        String originalSequence = protein.getSequence();

        // if the duplicate has a different sequence from the one in uniprot, we need to shift the ranges
        if (isSequenceChanged(originalSequence, uniprotSequence)){
            // it is only possible if the proteinProcessor is of type ProteinUpdateProcessor
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                // the list of interaction accessions with feature conflicts
                Set<String> interactionAcsWithBadFeatures = new HashSet<String>();

                Collection<Component> components = protein.getActiveInstances();

                for (Component component : components){
                    Interaction interaction = component.getInteraction();

                    Collection<Feature> features = component.getBindingDomains();
                    for (Feature feature : features){
                        // collect the invalid ranges
                        Collection<InvalidRange> invalidRanges = checker.collectRangesImpossibleToShift(feature, originalSequence, uniprotSequence);

                        if (!invalidRanges.isEmpty()){
                            // the interaction contains range conflicts
                            interactionAcsWithBadFeatures.add(interaction.getAc());

                            for (InvalidRange invalid : invalidRanges){
                                // if the sequence is up to date, it seems to be an invalid range even before the update
                                if (originalSequence.equalsIgnoreCase(invalid.getSequence())){
                                    processor.fireOnInvalidRange(new InvalidRangeEvent(context, invalid));
                                }
                            }
                        }
                    }
                }

                // we have range conflicts
                if (!interactionAcsWithBadFeatures.isEmpty()){
                    Collection<Component> componentsToFix = new ArrayList<Component>();
                    for (Component c : components){
                        if (interactionAcsWithBadFeatures.contains(c.getInteractionAc())){
                            // this component contains range problems
                            componentsToFix.add(c);
                        }
                    }

                    return componentsToFix;
                }
            }
            else {
                throw new ProcessorException("The protein "+protein.getAc()+" need to be merged and it is impossible to shift the ranges because the ProteinProcessor " +
                        "must be of type ProteinUpdateProcessor and not " + evt.getSource().getClass().getName());
            }
        }

        return Collections.EMPTY_LIST;
    }

    /**
     * add a caution and 'no-uniprot-update' to the protein
     * @param protein : the duplicate with range conflicts
     * @param previousAc : the original protein to keep
     */
    private void addAnnotationsForBadParticipant(Protein protein, String previousAc, DaoFactory factory){

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            factory.getCvObjectDao(CvTopic.class).saveOrUpdate(no_uniprot_update);
        }
        CvTopic caution = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).saveOrUpdate(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution = false;
        String cautionMessage = "The protein could not be merged with " + previousAc + " because od some incompatibilities with the protein sequence (features which cannot be shifted).";

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
            Annotation demerge = new Annotation(caution, "The protein could not be merged with " + previousAc + " because od some incompatibilities with the protein sequence (features which cannot be shifted).");
            annotationDao.persist(demerge);

            protein.addAnnotation(demerge);
        }
    }

    /**
     * Merge tha duplicates, the interactions are moved (not the cross references as they will be deleted)
     * @param duplicates
     */
    protected Protein merge(List<Protein> duplicates, Map<String, Collection<Component>> proteinsNeedingPartialMerge, DuplicatesFoundEvent evt, boolean isSequenceChanged) {
        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(duplicates);

        evt.setReferenceProtein(originalProt);

        // the merge can be done without looking at the sequence of the duplicates
        if (!isSequenceChanged){
            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate);

                    // create an "intact-secondary" xref to the protein to be kept.
                    // This will allow the user to search using old ACs
                    Institution owner = duplicate.getOwner();

                    // the database is always intact because the framework is the intact framework and when we merge two proteins of this framework, it becomes 'intact-secondary'
                    CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

                    if (db == null){
                        db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.MINT_MI_REF, CvDatabase.INTACT);
                        factory.getCvObjectDao(CvDatabase.class).saveOrUpdate(db);
                    }

                    final String intactSecondaryLabel = "intact-secondary";
                    boolean hasIntactSecondary = false;

                    CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(intactSecondaryLabel);

                    if (intactSecondary == null) {
                        intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, intactSecondaryLabel);
                        factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(intactSecondary);
                    }

                    for (InteractorXref ref : originalProt.getXrefs()){
                        if (ref.getCvDatabase() != null){
                            if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                                if (ref.getCvXrefQualifier() != null){
                                    if (ref.getCvXrefQualifier().getShortLabel().equals(intactSecondaryLabel) && ref.getPrimaryId().equals(duplicate.getAc())){
                                        hasIntactSecondary = true;
                                    }
                                }
                            }
                        }
                    }

                    if (!hasIntactSecondary){
                        InteractorXref xref = new InteractorXref(owner, db, duplicate.getAc(), intactSecondary);
                        factory.getXrefDao(InteractorXref.class).persist(xref);

                        originalProt.addXref(xref);
                        log.debug( "Adding 'intact-secondary' Xref to protein '"+ originalProt.getShortLabel() +"' ("+originalProt.getAc()+"): " + duplicate.getAc() );
                    }

                    final List<ProteinImpl> isoforms = factory.getProteinDao().getSpliceVariants( duplicate );
                    for ( ProteinImpl isoform : isoforms ) {
                        // each isoform should now point to the original protein
                        final Collection<InteractorXref> isoformParents =
                                AnnotatedObjectUtils.searchXrefs( isoform,
                                        CvDatabase.INTACT_MI_REF,
                                        CvXrefQualifier.ISOFORM_PARENT_MI_REF );

                        remapTranscriptParent(originalProt, duplicate.getAc(), isoform, isoformParents, factory);
                    }

                    final List<ProteinImpl> proteinChains = factory.getProteinDao().getProteinChains( duplicate );
                    for ( ProteinImpl chain : proteinChains ) {
                        // each chain should now point to the original protein
                        final Collection<InteractorXref> chainParents =
                                AnnotatedObjectUtils.searchXrefs(chain,
                                        CvDatabase.INTACT_MI_REF,
                                        CvXrefQualifier.CHAIN_PARENT_MI_REF );

                        remapTranscriptParent(originalProt, duplicate.getAc(), chain, chainParents, factory);
                    }
                    IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate
                    if (duplicate.getActiveInstances().isEmpty()) {
                        deleteProtein(duplicate, new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                    }
                }
            }
        }
        // before merging, we need to check the feature conflicts because the sequence needs to be updated
        else {
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            // move the interactions from the rest of proteins to the original
            for (Protein duplicate : duplicates) {

                // don't process the original protein with itself
                if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                    // we have feature conflicts for this protein which cannot be merged
                    if (proteinsNeedingPartialMerge.containsKey(duplicate.getAc())){
                        addAnnotationsForBadParticipant(duplicate, originalProt.getAc(), evt.getDataContext().getDaoFactory());

                        // components to let on the current protein
                        Collection<Component> componentToFix = proteinsNeedingPartialMerge.get(duplicate.getAc());
                        // components without conflicts to move on the original protein
                        Collection<Component> componentToMove = CollectionUtils.subtract(duplicate.getActiveInstances(), componentToFix);

                        for (Component component : componentToMove) {

                            duplicate.removeActiveInstance(component);
                            originalProt.addActiveInstance(component);
                            factory.getComponentDao().update(component);
                        }

                        factory.getProteinDao().update((ProteinImpl) duplicate);
                    }
                    // we don't have feature conflicts, we can merge the proteins normally
                    else {
                        ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate);
                        // create an "intact-secondary" xref to the protein to be kept.
                        // This will allow the user to search using old ACs
                        Institution owner = duplicate.getOwner();

                        // the database is always intact because the framework is the intact framework and when we merge two proteins of this framework, it becomes 'intact-secondary'
                        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

                        if (db == null){
                            db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.MINT_MI_REF, CvDatabase.INTACT);
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
                        }

                        // as the protein will be completely merged, an intact secondary reference is necessary
                        final String intactSecondaryLabel = "intact-secondary";
                        boolean hasIntactSecondary = false;

                        CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(intactSecondaryLabel);

                        if (intactSecondary == null) {
                            intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, intactSecondaryLabel);
                            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(intactSecondary);
                        }

                        for (InteractorXref ref : originalProt.getXrefs()){
                            if (ref.getCvDatabase() != null){
                                if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                                    if (ref.getCvXrefQualifier() != null){
                                        if (ref.getCvXrefQualifier().getShortLabel().equals(intactSecondaryLabel) && ref.getPrimaryId().equals(duplicate.getAc())){
                                            hasIntactSecondary = true;
                                        }
                                    }
                                }
                            }
                        }

                        if (!hasIntactSecondary){
                            InteractorXref xref = new InteractorXref(owner, db, duplicate.getAc(), intactSecondary);
                            factory.getXrefDao(InteractorXref.class).persist(xref);

                            originalProt.addXref(xref);
                            log.debug( "Adding 'intact-secondary' Xref to protein '"+ originalProt.getShortLabel() +"' ("+originalProt.getAc()+"): " + duplicate.getAc() );
                        }
                    }

                    // update isoforms and feature chains
                    final List<ProteinImpl> isoforms = factory.getProteinDao().getSpliceVariants( duplicate );
                    for ( ProteinImpl isoform : isoforms ) {
                        // each isoform should now point to the original protein
                        final Collection<InteractorXref> isoformParents =
                                AnnotatedObjectUtils.searchXrefs( isoform,
                                        CvDatabase.INTACT_MI_REF,
                                        CvXrefQualifier.ISOFORM_PARENT_MI_REF );

                        remapTranscriptParent(originalProt, duplicate.getAc(), isoform, isoformParents, factory);
                    }

                    final List<ProteinImpl> proteinChains = factory.getProteinDao().getProteinChains( duplicate );
                    for ( ProteinImpl chain : proteinChains ) {
                        // each chain should now point to the original protein
                        final Collection<InteractorXref> chainParents =
                                AnnotatedObjectUtils.searchXrefs(chain,
                                        CvDatabase.INTACT_MI_REF,
                                        CvXrefQualifier.CHAIN_PARENT_MI_REF );

                        remapTranscriptParent(originalProt, duplicate.getAc(), chain, chainParents, factory);
                    }
                    IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) duplicate);

                    // and delete the duplicate
                    if (duplicate.getActiveInstances().isEmpty()) {
                        deleteProtein(duplicate, new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                    }
                }
                else {
                    // if the original protein contains range conflict, we need to demerge it to keep the bad ranges attached to a no-uniprot-update protein
                    if (proteinsNeedingPartialMerge.containsKey(originalProt.getAc())){

                        processor.fireOnOutOfDateParticipantFound(new OutOfDateParticipantFoundEvent(processor, IntactContext.getCurrentInstance().getDataContext(), originalProt, proteinsNeedingPartialMerge.get(originalProt.getAc())));
                    }
                }

                String sequence = duplicate.getSequence();

                // if the sequence in uniprot is different than the one of the duplicate, need to update the sequence and shift the ranges
                if (isSequenceChanged(sequence, evt.getUniprotSequence())){
                    if (evt.getSource() instanceof ProteinUpdateProcessor){
                        log.debug( "sequence of "+duplicate.getAc()+" requires update." );
                        duplicate.setSequence( evt.getUniprotSequence() );

                        // CRC64
                        String crc64 = evt.getUniprotCrc64();
                        if ( duplicate.getCrc64() == null || !duplicate.getCrc64().equals( crc64 ) ) {
                            log.debug( "CRC64 requires update." );
                            duplicate.setCrc64( crc64 );
                        }

                        factory.getProteinDao().update((ProteinImpl) duplicate);
                        processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, IntactContext.getCurrentInstance().getDataContext(), duplicate, sequence, evt.getUniprotSequence(), evt.getUniprotCrc64()));
                    }
                    else {
                        throw new ProcessorException("");
                    }
                }
            }
        }

        IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) originalProt);

        return originalProt;
    }



    /**
     * Remap the transcripts attached to this duplicate to the original protein
     * @param originalProt
     * @param transcript
     * @param transcriptParents
     */
    protected void remapTranscriptParent(Protein originalProt, String duplicateAc, ProteinImpl transcript, Collection<InteractorXref> transcriptParents, DaoFactory factory) {

        boolean hasRemappedParentAc = false;

        for (InteractorXref xref : transcriptParents){

            if (xref.getPrimaryId().equals(duplicateAc)){
                xref.setPrimaryId( originalProt.getAc() );
                factory.getXrefDao(InteractorXref.class).update(xref);
//                        daoFactory.getXrefDao( InteractorXref.class ).update( xref );
                log.debug( "Fixing transcript-parent Xref for transcript '"+ transcript.getShortLabel()+
                        "' ("+ transcript.getAc()+") so that it points to the merges master protein '"+
                        originalProt.getShortLabel() + "' (" + originalProt.getAc() + ")" );
                break;
            }
        }

        if (!hasRemappedParentAc){
            log.error( "No isoform parent cross reference refers to the duplicate : " + duplicateAc );
        }
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

    protected static Protein calculateOriginalProteinBasedOnSequence(List<? extends Protein> duplicates) {
        Protein originalProt = null;

        for (int i = 0; i < duplicates.size(); i++) {
            Protein duplicate =  duplicates.get(i);

            if (originalProt == null){
                if (duplicate.getSequence() != null){
                    originalProt = duplicate;
                }
            }
            else if (duplicate.getCreated().before(originalProt.getCreated()) && duplicate.getSequence() != null) {
                originalProt = duplicate;
            }
        }

        return originalProt;
    }

    /**
     * Fire a delete event for this protein
     * @param protein
     * @param evt
     */
    private void deleteProtein(Protein protein, ProteinEvent evt) {
        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        processor.fireOnDelete(new ProteinEvent(evt.getSource(), evt.getDataContext(), protein, evt.getMessage()));
    }
}