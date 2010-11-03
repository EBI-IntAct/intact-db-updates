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
                }
            }

            // if we have more than two proteins in the duplicate list, we merge them
            if (duplicatesHavingSameSequence.size() > 1){
                duplicatesHavingDifferentSequence.add(merge(duplicatesHavingSameSequence, Collections.EMPTY_MAP, evt));
            }
            else{
                duplicatesHavingDifferentSequence.addAll(duplicatesHavingSameSequence);
            }

            // we remove the processed proteins from the list of protein to process
            duplicatesAsList.removeAll(duplicatesHavingSameSequence);
        }

        if (duplicatesHavingDifferentSequence.size() > 1){
            if (evt.getUniprotSequence() != null){
                Map<String, Collection<Component>> proteinNeedingPartialMerge = new HashMap<String, Collection<Component>>();

                for (Protein p : duplicatesHavingDifferentSequence){
                    Collection<Component> componentWithRangeConflicts = shiftRangesToUniprotSequence(p, evt.getUniprotSequence(), evt);

                    if (!componentWithRangeConflicts.isEmpty()){
                        proteinNeedingPartialMerge.put(p.getAc(), componentWithRangeConflicts);
                    }
                }

                Protein finalProt = merge(duplicatesHavingDifferentSequence, proteinNeedingPartialMerge, evt);
                if (!finalProt.getSequence().equals(evt.getUniprotSequence())){
                    finalProt.setSequence( evt.getUniprotSequence() );

                    // CRC64
                    String crc64 = evt.getUniprotCrc64();
                    if ( finalProt.getCrc64() == null || !finalProt.getCrc64().equals( crc64 ) ) {
                        log.debug( "CRC64 requires update." );
                        finalProt.setCrc64( crc64 );
                    }

                    IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) finalProt);

                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, IntactContext.getCurrentInstance().getDataContext(), finalProt, finalProt.getSequence(), evt.getUniprotSequence(), evt.getUniprotCrc64()));
                }
            }
            else {
                log.error("It is impossible to merge all the duplicates because the duplicates have different sequence and no uniprot sequence has been given to be able to shift the ranges before the merge.");
            }
        }
    }

    protected Collection<Component> shiftRangesToUniprotSequence(Protein protein, String uniprotSequence, DuplicatesFoundEvent evt){
        boolean sequenceToBeUpdated = false;

        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        RangeChecker checker = new RangeChecker();
        DataContext context = IntactContext.getCurrentInstance().getDataContext();

        String originalSequence = protein.getSequence();

        if ( (originalSequence == null && uniprotSequence != null)) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Sequence requires update." );
            }
            sequenceToBeUpdated = true;
        }
        else if (originalSequence != null && uniprotSequence != null){
            if (!uniprotSequence.equals( originalSequence ) ){
                if ( log.isDebugEnabled() ) {
                    log.debug( "Sequence requires update." );
                }
                sequenceToBeUpdated = true;
            }
        }

        if (sequenceToBeUpdated){
            Set<String> interactionAcsWithBadFeatures = new HashSet<String>();

            Collection<Component> components = protein.getActiveInstances();

            for (Component component : components){
                Interaction interaction = component.getInteraction();

                Collection<Feature> features = component.getBindingDomains();
                for (Feature feature : features){
                    Collection<InvalidRange> invalidRanges = checker.collectRangesImpossibleToShift(feature, originalSequence, uniprotSequence);

                    if (!invalidRanges.isEmpty()){
                        interactionAcsWithBadFeatures.add(interaction.getAc());

                        for (InvalidRange invalid : invalidRanges){
                            if (originalSequence.equalsIgnoreCase(invalid.getSequence())){
                                processor.fireOnInvalidRange(new InvalidRangeEvent(context, invalid));
                            }
                        }
                    }
                }
            }

            if (!interactionAcsWithBadFeatures.isEmpty()){
                Collection<Component> componentsToFix = new ArrayList<Component>();
                for (Component c : components){
                    if (interactionAcsWithBadFeatures.contains(c.getInteractionAc())){
                        componentsToFix.add(c);
                    }
                }
                return componentsToFix;
            }
        }

        return Collections.EMPTY_LIST;
    }

    private void addAnnotationsForBadParticipant(Protein protein, String previousAc){

        CvTopic no_uniprot_update = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(no_uniprot_update);
        }
        CvTopic caution = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
        }

        AnnotationDao annotationDao = IntactContext.getCurrentInstance().getDaoFactory().getAnnotationDao();

        Annotation no_uniprot = new Annotation(no_uniprot_update, null);
        annotationDao.persist(no_uniprot);

        protein.addAnnotation(no_uniprot);

        Annotation demerge = new Annotation(caution, "The protein could not be merged with " + previousAc + " because od some incompatibilities with the protein sequence (features which cannot be shifted).");
        annotationDao.persist(demerge);

        protein.addAnnotation(demerge);
    }

    /**
     * Merge tha duplicates, the interactions are moved and the cross references as well
     * @param duplicates
     */
    protected Protein merge(List<Protein> duplicates, Map<String, Collection<Component>> proteinsNeedingPartialMerge, DuplicatesFoundEvent evt) {
        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(duplicates);

        evt.setReferenceProtein(originalProt);

        // move the interactions from the rest of proteins to the original
        for (Protein duplicate : duplicates) {

            // don't process the original protein with itself
            if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                if (proteinsNeedingPartialMerge.containsKey(duplicate.getAc())){
                    addAnnotationsForBadParticipant(duplicate, originalProt.getAc());

                    Collection<Component> componentToFix = proteinsNeedingPartialMerge.get(duplicate.getAc());
                    Collection<Component> componentToMove = CollectionUtils.subtract(duplicate.getActiveInstances(), componentToFix);

                    for (Component component : componentToMove) {

                        duplicate.removeActiveInstance(component);
                        originalProt.addActiveInstance(component);
                        IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().update(component);
                    }

                    IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) duplicate);
                }
                else {
                    ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate);
                }

                List<InteractorXref> copiedXrefs = ProteinTools.copyNonIdentityXrefs(originalProt, duplicate);

                for (InteractorXref copiedXref : copiedXrefs) {
                    DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
                    daoFactory.getXrefDao(InteractorXref.class).persist(copiedXref);
                }

                // create an "intact-secondary" xref to the protein to be kept.
                // This will allow the user to search using old ACs
                Institution owner = duplicate.getOwner();
                DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();

                // the database is always intact because the framework is the intact framework and when we merge two proteins of this framework, it becomes 'intact-secondary'
                CvDatabase db = daoFactory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

                if (db == null){
                    db = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvDatabase.class, CvDatabase.MINT_MI_REF, CvDatabase.INTACT);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(db);
                }

                final String intactSecondaryLabel = "intact-secondary";

                CvXrefQualifier intactSecondary = daoFactory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(intactSecondaryLabel);

                if (intactSecondary == null) {
                    intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, intactSecondaryLabel);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(intactSecondary);
                }

                InteractorXref xref = new InteractorXref(owner, db, duplicate.getAc(), intactSecondary);
                daoFactory.getXrefDao(InteractorXref.class).persist(xref);

                originalProt.addXref(xref);
                log.debug( "Adding 'intact-secondary' Xref to protein '"+ originalProt.getShortLabel() +"' ("+originalProt.getAc()+"): " + duplicate.getAc() );

                final List<ProteinImpl> isoforms = daoFactory.getProteinDao().getSpliceVariants( duplicate );
                for ( ProteinImpl isoform : isoforms ) {
                    // each isoform should now point to the original protein
                    final Collection<InteractorXref> isoformParents =
                            AnnotatedObjectUtils.searchXrefs( isoform,
                                    CvDatabase.INTACT_MI_REF,
                                    CvXrefQualifier.ISOFORM_PARENT_MI_REF );

                    remapTranscriptParent(originalProt, duplicate.getAc(), isoform, isoformParents);
                }

                final List<ProteinImpl> proteinChains = daoFactory.getProteinDao().getProteinChains( duplicate );
                for ( ProteinImpl chain : proteinChains ) {
                    // each chain should now point to the original protein
                    final Collection<InteractorXref> chainParents =
                            AnnotatedObjectUtils.searchXrefs(chain,
                                    CvDatabase.INTACT_MI_REF,
                                    CvXrefQualifier.CHAIN_PARENT_MI_REF );

                    remapTranscriptParent(originalProt, duplicate.getAc(), chain, chainParents);
                }
                IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) duplicate);

                // and delete the duplicate
                if (duplicate.getActiveInstances().isEmpty()) {
                    deleteProtein(duplicate, new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                }
            }
            else {
                if (proteinsNeedingPartialMerge.containsKey(originalProt.getAc())){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                    processor.fireOnOutOfDateParticipantFound(new OutOfDateParticipantFoundEvent(processor, IntactContext.getCurrentInstance().getDataContext(), originalProt, proteinsNeedingPartialMerge.get(originalProt.getAc())));
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
    protected void remapTranscriptParent(Protein originalProt, String duplicateAc, ProteinImpl transcript, Collection<InteractorXref> transcriptParents) {

        boolean hasRemappedParentAc = false;

        for (InteractorXref xref : transcriptParents){

            if (xref.getPrimaryId().equals(duplicateAc)){
                xref.setPrimaryId( originalProt.getAc() );
                IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(xref);
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