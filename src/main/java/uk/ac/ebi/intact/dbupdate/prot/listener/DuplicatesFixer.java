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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.util.DebugUtil;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;

import java.util.ArrayList;
import java.util.Collection;
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

    protected void mergeDuplicates(Collection<Protein> duplicates, DuplicatesFoundEvent evt) {
        if (log.isDebugEnabled()) log.debug("Merging duplicates: "+ DebugUtil.acList(duplicates));

        // add the interactions from the duplicated proteins to the protein
        // that was created first in the database

        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(new ArrayList<Protein>(duplicates));
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
                InstitutionXref ownerXref = XrefUtils.getPsiMiIdentityXref(owner);
                DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
                // TODO set an owner Xref in the test !!!Aurelie Hoareau
                if (ownerXref != null) {
                    CvDatabase ownerDb = daoFactory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( ownerXref.getPrimaryId() );

                    CvXrefQualifier intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, "intact-secondary");
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(intactSecondary);

                    InteractorXref xref = new InteractorXref(owner, ownerDb, duplicate.getAc(), intactSecondary);
                    daoFactory.getXrefDao(InteractorXref.class).persist(xref);
                    
                    originalProt.addXref(xref);
                    log.debug( "Adding 'intact-secondary' Xref to protein '"+ originalProt.getShortLabel() +"' ("+originalProt.getAc()+"): " + duplicate.getAc() );
                }

                final List<ProteinImpl> isoforms = daoFactory.getProteinDao().getSpliceVariants( duplicate );
                for ( ProteinImpl isoform : isoforms ) {
                    // each isoform should now point to the original protein
                    final Collection<InteractorXref> isoformParents =
                            AnnotatedObjectUtils.searchXrefs( isoform,
                                                              CvDatabase.INTACT_MI_REF,
                                                              CvXrefQualifier.ISOFORM_PARENT_MI_REF );

                    remapTranscriptParent(originalProt, isoform, isoformParents);
                }

                final List<ProteinImpl> proteinChains = daoFactory.getProteinDao().getProteinChains( duplicate );
                for ( ProteinImpl chain : proteinChains ) {
                    // each chain should now point to the original protein
                    final Collection<InteractorXref> chainParents =
                            AnnotatedObjectUtils.searchXrefs(chain,
                                                              CvDatabase.INTACT_MI_REF,
                                                              CvXrefQualifier.CHAIN_PARENT_MI_REF );

                    remapTranscriptParent(originalProt, chain, chainParents);
                }

                // and delete the duplicate
                if (duplicate.getActiveInstances().isEmpty()) {
                    deleteProtein(duplicate, new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                } else {
                    throw new IllegalStateException("Attempt to delete a duplicate that still contains interactions: "+protInfo(duplicate));
                }
            }
        }

    }

    protected void remapTranscriptParent(Protein originalProt, ProteinImpl transcript, Collection<InteractorXref> transcriptParents) {
        if( transcriptParents.size() != 1 ) {
            log.warn( "More than one transcript-parent Xref found on protein transcript: " + transcript.getAc() );
        } else {
            final InteractorXref xref = transcriptParents.iterator().next();
            xref.setPrimaryId( originalProt.getAc() );
            IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(xref);
//                        daoFactory.getXrefDao( InteractorXref.class ).update( xref );
            log.debug( "Fixing transcript-parent Xref for transcript '"+ transcript.getShortLabel()+
                       "' ("+ transcript.getAc()+") so that it points to the merges master protein '"+
                       originalProt.getShortLabel() + "' (" + originalProt.getAc() + ")" );
        }
    }


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

    private void deleteProtein(Protein protein, ProteinEvent evt) {
        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        processor.fireOnDelete(new ProteinEvent(evt.getSource(), evt.getDataContext(), protein, evt.getMessage()));
    }
}