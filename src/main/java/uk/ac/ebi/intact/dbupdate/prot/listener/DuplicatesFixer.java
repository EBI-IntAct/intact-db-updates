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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Iterator;
import java.util.List;

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
                duplicatesHavingDifferentSequence.add(merge(duplicatesHavingSameSequence, evt));
            }
            else{
                duplicatesHavingDifferentSequence.addAll(duplicatesHavingSameSequence);
            }

            // we remove the processed proteins from the list of protein to process
            duplicatesAsList.removeAll(duplicatesHavingSameSequence);
        }

        if (duplicatesHavingDifferentSequence.size() > 1){
            List<Protein> proteinsWithShiftedRanges = new ArrayList<Protein>();

            for (Protein p : duplicatesHavingDifferentSequence){
                if (shiftRangesToUniprotSequence(p, evt.getUniprotSequence(), evt)){
                    proteinsWithShiftedRanges.add(p);
                }
            }

            if (proteinsWithShiftedRanges.size() > 1){
                merge(proteinsWithShiftedRanges, evt);
            }

            Collection<Protein> proteinsWhichCannotBeMerged = CollectionUtils.subtract(duplicatesHavingDifferentSequence, proteinsWithShiftedRanges);
            CvObjectDao<CvTopic> cvDao = IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvTopic.class);

            for (Protein p : proteinsWhichCannotBeMerged){
                CvTopic caution = cvDao.getByPsiMiRef(CvTopic.CAUTION_MI_REF);

                if (caution == null){
                    caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
                    cvDao.persist(caution);
                }

                Annotation cautionAnn = new Annotation(caution, "This protein ("+p.getAc()+") need to be merged with other proteins but the features attached to this protein cannot be shifted properly.");
                IntactContext.getCurrentInstance().getDaoFactory().getAnnotationDao().persist(cautionAnn);

                p.addAnnotation(cautionAnn);
                IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) p);
            }
        }
    }

    protected boolean shiftRangesToUniprotSequence(Protein protein, String uniprotSequence, DuplicatesFoundEvent evt){
        boolean canMerge = true;
        boolean isSequenceDifferent = true;

        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        RangeChecker checker = new RangeChecker();

        String originalSequence = protein.getSequence();

        if (uniprotSequence != null){
            if (originalSequence != null){
                if (!originalSequence.equalsIgnoreCase(uniprotSequence)){
                    Collection<Component> components = protein.getActiveInstances();

                    for (Component c : components){
                        Collection<Feature> features = c.getBindingDomains();

                        for (Feature f : features){

                            if (!checker.canShiftRange(f, protein.getSequence(), uniprotSequence)){
                                canMerge = false;
                            }
                        }
                    }
                }
                else {
                    isSequenceDifferent = false;
                }
            }
            else {
                isSequenceDifferent = true;
            }
        }

        if (canMerge){
            if (isSequenceDifferent){
                if ( log.isDebugEnabled() ) {
                    log.debug( "Sequence requires update before merging " + protein.getAc() + " with other duplicated proteins." );
                }
                protein.setSequence(uniprotSequence);
                IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().update((ProteinImpl) protein);
                processor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(processor, evt.getDataContext(), protein, originalSequence));
            }
        }

        return canMerge;
    }

    /**
     * Merge tha duplicates, the interactions are moved and the cross references as well
     * @param duplicates
     */
    protected Protein merge(List<Protein> duplicates, DuplicatesFoundEvent evt) {
        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(duplicates);

        evt.setReferenceProtein(originalProt);

        // move the interactions from the rest of proteins to the original
        for (Protein duplicate : duplicates) {

            // don't process the original protein with itself
            if ( ! duplicate.getAc().equals( originalProt.getAc() ) ) {

                ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate);
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
                } else {
                    throw new IllegalStateException("Attempt to delete a duplicate that still contains interactions: "+protInfo(duplicate));
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