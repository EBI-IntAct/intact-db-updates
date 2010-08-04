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
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.Collection;
import java.util.List;

/**
 * Will check if the protein participate in any interaction and if not it will be (configuration allowing) deleted.
 *
 * If a protein has splice variants or chains, and any of them have interactions, none
 * of the proteins will be deleted unless <code>deleteProteinTranscriptsWithoutInteractions</code> is true, which would remove
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

    private boolean deleteProteinTranscriptsWithoutInteractions;

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

        final boolean isProteinTranscript = isProteinTranscript(protein);

        // TODO migrate later
//        final boolean isFeatureChain = ProteinUtils.isFeatureChain(protein);

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        final Integer interactionCount = proteinDao.countInteractionsForInteractorWithAc( protein.getAc() );

        // it is not a splice variant, not a feature chain but a protein
        if( !isProteinTranscript ) {

            // check the number of interactions in which this protein is involved. If none,
            // check if the protein has splice variants/chains as they cannot be removed

            // Checking is any splice variant or feature chain is involved in interactions
            boolean hasProteinTranscriptAttached = false;
            // splice variants
            List<ProteinImpl> trasncripts = proteinDao.getSpliceVariants(protein);
            // feature chains
            trasncripts.addAll(proteinDao.getProteinChains(protein));

            for (Protein transcript : trasncripts) {
                if (proteinDao.countInteractionsForInteractorWithAc(transcript.getAc()) > 0) {
                    hasProteinTranscriptAttached = true;
                } else if (isDeleteProteinTranscriptsWithoutInteractions()) {
                    if (log.isDebugEnabled()) log.debug("Protein transcripts for protein '"+protein.getShortLabel()+"' will be deleted: "+protInfo( transcript ));
                    evt.setMessage("Protein transcript without interactions");
                    deleteProtein(transcript, evt);
                } else {
                    hasProteinTranscriptAttached = true;
                }
            }

            // if no splice variant/chain attached to that master either and it is not involved in interactions, then delete it.
            if ( interactionCount == 0 && ! hasProteinTranscriptAttached) {
                if (log.isDebugEnabled()) log.debug("Protein '"+protInfo(protein)+"' will be deleted as it doesn't have interaction and has no isoform/chain attached." );
                evt.setMessage("Protein without interactions");
                deleteProtein(protein, evt);
            }

        } else if ( isProteinTranscript && interactionCount == 0 && isDeleteProteinTranscriptsWithoutInteractions() ) {

            if (log.isDebugEnabled()) log.debug("Protein transcript will be deleted: "+protInfo(protein));
            evt.setMessage("Protein transcript without interactions - however master does");
            deleteProtein(protein, evt);

        }
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
                if ( CvXrefQualifier.CHAIN_PARENT_MI_REF.equals(qualifierIdentity)) {
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

    public boolean isDeleteProteinTranscriptsWithoutInteractions() {
        return deleteProteinTranscriptsWithoutInteractions;
    }

    public void setDeleteProteinTranscriptsWithoutInteractions(boolean deleteProteinTranscriptsWithoutInteractions) {
        this.deleteProteinTranscriptsWithoutInteractions = deleteProteinTranscriptsWithoutInteractions;
    }

    /**
     * A protein transcript is either a splice variant, or a feature chain
     * @param protein
     * @return true if the protein is a protein transcript
     */
    private boolean isProteinTranscript(Protein protein){
        final boolean isSpliceVariant = ProteinUtils.isSpliceVariant(protein);
        final boolean isFeatureChain = isFeatureChain(protein);

        if (isSpliceVariant || isFeatureChain){
             return true;
        }

        return false;
    }
}
