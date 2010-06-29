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
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Will check if the protein participate in any interaction and if not it will be (configuration allowing) deleted.
 *
 * If a protein has splice variants or chains, and any of them have interactions, none
 * of the proteins will be deleted unless <code>deleteSpliceVariantsWithoutInteractions</code> is true, which would remove
 * the splice vars (without interactions) as well.
 *
 * @see uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * 
 * @version $Id$
 */
public class ProtWithoutInteractionDeleter extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( ProtWithoutInteractionDeleter.class );

    private boolean deleteSpliceVariantsWithoutInteractions;
    private boolean deleteChainsWithoutInteractions;

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        final Protein protein = evt.getProtein();

        if (log.isDebugEnabled()) {
            log.debug("Checking if the protein has interactions: "+ protInfo(protein) );
        }

        if (protein.getAc() == null) {
            log.debug("Protein without AC, cannot be deleted");
            return;
        }

        final boolean isSpliceVariant = ProteinUtils.isSpliceVariant(protein);
        final boolean isFeatureChain = isFeatureChain(protein);
        // TODO migrate later
//        final boolean isFeatureChain = ProteinUtils.isFeatureChain(protein);
        final boolean isProtein = ! isSpliceVariant && ! isFeatureChain;

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        final Integer interactionCount = proteinDao.countInteractionsForInteractorWithAc( protein.getAc() );

        if( isProtein ) {

            // check the number of interactions in which this protein is involved. If none,
            // check if the protein has splice variants/chains as they cannot be removed

            // Checking is any splice variant is involved in interactions
            boolean hasIsoformAttached = false;
            List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants(protein);

            for (Protein isoform : spliceVariants) {
                if (proteinDao.countInteractionsForInteractorWithAc(isoform.getAc()) > 0) {
                    hasIsoformAttached = true;
                } else if (isDeleteSpliceVariantsWithoutInteractions()) {
                    if (log.isDebugEnabled()) log.debug("Splice variant for protein '"+protein.getShortLabel()+"' will be deleted: "+protInfo( isoform ));
                    evt.setMessage("Splice variant without interactions");
                    deleteProtein(isoform, evt);
                } else {
                    hasIsoformAttached = true;
                }
            }

            // Checking if any chain is involved in interactions
            boolean hasChainsAttached = false;
            // TODO migrate later
            List<ProteinImpl> chains = getFeatureChains(protein, evt);
//            List<ProteinImpl> chains = proteinDao.getFeatureChains(protein);

            for (Protein chain : chains) {
                if (proteinDao.countInteractionsForInteractorWithAc(chain.getAc()) > 0) {
                    hasChainsAttached = true;
                } else if (isDeleteChainsWithoutInteractions()) {
                    if ( log.isDebugEnabled() )
                        log.debug("Feature chain for protein '"+protein.getShortLabel()+"' will be deleted: "+ protInfo( chain ) );
                    evt.setMessage("Feature chain without interactions");
                    deleteProtein(chain, evt);
                } else {
                    hasChainsAttached = true;
                }
            }

            // if no splice variant/chain attached to that master either and it is not involved in interactions, then delete it.
            if ( interactionCount == 0 && ! hasIsoformAttached && ! hasChainsAttached ) {
                if (log.isDebugEnabled()) log.debug("Protein '"+protInfo(protein)+"' will be deleted as it doesn't have interaction and has no isoform/chain attached." );
                evt.setMessage("Protein without interactions");
                deleteProtein(protein, evt);
            }

        } else if ( isSpliceVariant && interactionCount == 0 && isDeleteSpliceVariantsWithoutInteractions() ) {

            if (log.isDebugEnabled()) log.debug("Splice variant will be deleted: "+protInfo(protein));
            evt.setMessage("Splice variant without interactions - however master does");
            deleteProtein(protein, evt);

        } else if ( isFeatureChain && interactionCount == 0 && isDeleteChainsWithoutInteractions()) {

            if (log.isDebugEnabled()) log.debug("Splice variant will be deleted: "+protInfo(protein));
            evt.setMessage("Splice variant without interactions - however master does");
            deleteProtein(protein, evt);
        }
    }

    private List<ProteinImpl> getFeatureChains( Protein protein, ProteinEvent evt ) {
//        List<ProteinImpl> chains = new ArrayList<ProteinImpl>( );
        final ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        return proteinDao.getByXrefLike( CvDatabase.INTACT_MI_REF, "MI:0951", protein.getAc() );
//        final Collection<InteractorXref> xrefs = AnnotatedObjectUtils.searchXrefs( protein, CvDatabase.INTACT_MI_REF, "MI:0951" );
//        for ( InteractorXref xref : xrefs ) {
//            chains.add( proteinDao.getByAc( xref.getPrimaryId() ) );
//        }
//        return chains;
    }

    /**
     * Checks if the given protein is a feature chain.
     *
     * @param protein the protein to check
     * @return true if the protein is a feature chain
     */
    public boolean isFeatureChain(Protein protein) {
        Collection<InteractorXref> xrefs = protein.getXrefs();
        for (InteractorXref xref : xrefs) {
            if (xref.getCvXrefQualifier() != null) {
                String qualifierIdentity = xref.getCvXrefQualifier().getIdentifier();
                if ( "MI:0951".equals(qualifierIdentity)) {
                    return true;
                }
            }
        }
        return false;
    }

    private void deleteProtein(Protein protein, ProteinEvent evt) {
        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        processor.fireOnDelete(new ProteinEvent(processor, evt.getDataContext(), protein, evt.getMessage()));
    }

    public boolean isDeleteSpliceVariantsWithoutInteractions() {
        return deleteSpliceVariantsWithoutInteractions;
    }

    public void setDeleteSpliceVariantsWithoutInteractions(boolean deleteSpliceVariantsWithoutInteractions) {
        this.deleteSpliceVariantsWithoutInteractions = deleteSpliceVariantsWithoutInteractions;
    }

    public boolean isDeleteChainsWithoutInteractions() {
        return deleteChainsWithoutInteractions;
    }

    public void setDeleteChainsWithoutInteractions(boolean deleteChainsWithoutInteractions) {
        this.deleteChainsWithoutInteractions = deleteChainsWithoutInteractions;
    }
}
