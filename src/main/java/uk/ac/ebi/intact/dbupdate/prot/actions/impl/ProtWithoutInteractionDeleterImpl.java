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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.ProtWithoutInteractionDeleter;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.*;

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
public class ProtWithoutInteractionDeleterImpl implements ProtWithoutInteractionDeleter {

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( ProtWithoutInteractionDeleterImpl.class );

    /**
     *
     * @param evt evt containing the protein to look at
     * @return true if the protein doesn't have any interactions nor splice variants/feature chains with interactions and can be deleted,
     * false otherwise
     * @throws ProcessorException
     */
    public boolean hasToBeDeleted(ProteinEvent evt) throws ProcessorException {
        final Protein protein = evt.getProtein();

        if (log.isDebugEnabled()) {
            log.debug("Checking if the protein has interactions: "+ protein.getAc() );
        }

        if (protein.getAc() == null) {
            return true;
        }

        final boolean isProteinTranscript = isProteinTranscript(protein);

//        final boolean isFeatureChain = ProteinUtils.isFeatureChain(protein);

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        // collect number of active instances
        final Integer interactionCount = protein.getActiveInstances().size();

        // it is not a splice variant, not a feature chain but a protein
        if( !isProteinTranscript ) {

            // check the number of interactions in which this protein is involved. If none,
            // check if the protein has splice variants/chains as they cannot be removed

            // Checking is any splice variant or feature chain is involved in interactions
            boolean hasTranscriptInvolvedInInteractionsAttached = false;
            // splice variants
            List<ProteinImpl> transcripts = proteinDao.getSpliceVariants(protein);
            // feature chains
            transcripts.addAll(proteinDao.getProteinChains(protein));

            for (Protein transcript : transcripts) {

                if (transcript.getActiveInstances().size() > 0) {
                    hasTranscriptInvolvedInInteractionsAttached = true;
                } else if (isDeleteProteinTranscriptsWithoutInteractions()) {
                    if (log.isDebugEnabled()) log.debug("Protein transcripts for protein '"+protein.getShortLabel()+"' will be deleted: "+transcript.getAc());
                    evt.setMessage("Protein transcript without any interactions");
                } else {
                    hasTranscriptInvolvedInInteractionsAttached = true;
                }
            }

            // if no splice variant/chain attached to that master either and it is not involved in interactions, then delete it.
            if ( interactionCount == 0 && ! hasTranscriptInvolvedInInteractionsAttached) {
                if (log.isDebugEnabled()) log.debug("Protein '"+protein.getAc()+"' will be deleted as it doesn't have interaction and has no isoform/chain attached." );
                evt.setMessage("Protein without any interactions");
                return true;
            }

        } else if ( isProteinTranscript && interactionCount == 0) {

            if (log.isDebugEnabled()) log.debug("Protein transcript will be deleted: "+protein.getAc());
            evt.setMessage("Protein transcript without any interactions");
            return true;

        }

        return false;
    }

    /**
     *
     * @param evt : contains all the proteins attached to a single uniprot entry
     * @return remove the proteins without interactions and without protein transcript with interactions from the list of
     * proteins to update and return them
     */
    public Set<Protein> collectAndRemoveProteinsWithoutInteractions(UpdateCaseEvent evt){
        Set<Protein> protToDelete = new HashSet<Protein>();

        if (!evt.getPrimaryIsoforms().isEmpty()){
            collectProteinsTranscriptsWithoutInteractionsFrom(evt.getPrimaryIsoforms(), protToDelete, evt);
        }
        if (!evt.getSecondaryIsoforms().isEmpty()){
            collectProteinsTranscriptsWithoutInteractionsFrom(evt.getSecondaryIsoforms(), protToDelete, evt);
        }
        if (!evt.getPrimaryFeatureChains().isEmpty()){
            collectProteinsTranscriptsWithoutInteractionsFrom(evt.getPrimaryFeatureChains(), protToDelete, evt);
        }
        if (!evt.getPrimaryProteins().isEmpty()){
            collectProteinsWithoutInteractionsFrom(evt.getPrimaryProteins(), protToDelete, evt);
        }
        if (!evt.getSecondaryProteins().isEmpty()){
            collectProteinsWithoutInteractionsFrom(evt.getSecondaryProteins(), protToDelete, evt);
        }

        return protToDelete;
    }

    /**
     * For each protein to review, check if it has interactions attached to it. If not and no transcripts with interactions
     * are attached to the protein, add the protein to the list of proteins to delete
     * @param protToInspect : list of proteins to review
     * @param protToDelete : list of proteins to delete
     * @param evt
     */
    private void collectProteinsWithoutInteractionsFrom(Collection<Protein> protToInspect, Set<Protein> protToDelete, UpdateCaseEvent evt){

        for (Protein p : protToInspect){
            if (p.getAc() == null) {
                log.debug("Protein without AC, cannot be deleted");

                protToDelete.add(p);
            }
            else {

                ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

                final Integer interactionCount = p.getActiveInstances().size();

                // check the number of interactions in which this protein is involved. If none,
                // check if the protein has splice variants/chains as they cannot be removed

                // Checking is any splice variant or feature chain is involved in interactions
                boolean hasProteinTranscriptAttached = false;
                // splice variants
                List<ProteinImpl> transcripts = proteinDao.getSpliceVariants(p);
                // feature chains
                transcripts.addAll(proteinDao.getProteinChains(p));

                for (Protein transcript : transcripts) {

                    if (transcript.getActiveInstances().size() > 0) {
                        hasProteinTranscriptAttached = true;
                    } else if (isDeleteProteinTranscriptsWithoutInteractions()) {
                        if (log.isDebugEnabled()) log.debug("Protein transcripts for protein '"+p.getShortLabel()+"' will be deleted: "+transcript.getAc());
                        protToDelete.add(transcript);
                        if (evt.getPrimaryIsoforms().contains(transcript)){
                            evt.getPrimaryIsoforms().remove(transcript);
                        }
                        else if (evt.getSecondaryIsoforms().contains(transcript)){
                            evt.getSecondaryIsoforms().remove(transcript);
                        }
                        else if (evt.getPrimaryFeatureChains().contains(transcript)){
                            evt.getPrimaryFeatureChains().remove(transcript);
                        }
                    } else {
                        hasProteinTranscriptAttached = true;
                    }
                }

                // if no splice variant/chain attached to that master either and it is not involved in interactions, then delete it.
                if ( interactionCount == 0 && ! hasProteinTranscriptAttached) {
                    if (log.isDebugEnabled()) log.debug("Protein '"+p.getAc()+"' will be deleted as it doesn't have interaction and has no isoform/chain attached." );
                    protToDelete.add(p);
                }
            }
        }

        protToInspect.removeAll(protToDelete);
    }

    /**
     * For each protein transcript to review, check if it has interactions attached to it. If no
     * add the protein transcript to the list of proteins to delete
     * @param protToInspect : list of protein transcripts to review
     * @param protToDelete : list of protein transcripts to delete
     * @param evt
     */
    private void collectProteinsTranscriptsWithoutInteractionsFrom(Collection<ProteinTranscript> protToInspect, Set<Protein> protToDelete, UpdateCaseEvent evt){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        Collection<ProteinTranscript> transcriptToDelete = new ArrayList<ProteinTranscript>();

        for (ProteinTranscript p : protToInspect){
            if (p.getProtein().getAc() == null) {
                log.debug("Protein without AC, cannot be deleted");
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                    ProteinUpdateError impossibleToDelete = errorFactory.createImpossibleToDeleteError(p.getProtein().getShortLabel(), "The protein " + p.getProtein().getShortLabel() + " cannot be deleted because doesn't have any intact ac.");
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), impossibleToDelete, p.getProtein(), evt.getQuerySentToService()));
                }
            }
            else {

                final Integer interactionCount = p.getProtein().getActiveInstances().size();

                if ( interactionCount == 0) {

                    if (log.isDebugEnabled()) log.debug("Protein transcript will be deleted: "+p.getProtein().getAc());
                    protToDelete.add(p.getProtein());
                    transcriptToDelete.add(p);
                }
            }
        }

        protToInspect.removeAll(transcriptToDelete);
    }

    public boolean isDeleteProteinTranscriptsWithoutInteractions() {
        return ProteinUpdateContext.getInstance().getConfig().isDeleteProteinTranscriptWithoutInteractions();
    }

    /**
     * A protein transcript is either a splice variant, or a feature chain
     * @param protein
     * @return true if the protein is a protein transcript
     */
    private boolean isProteinTranscript(Protein protein){
        final boolean isSpliceVariant = ProteinUtils.isSpliceVariant(protein);
        final boolean isFeatureChain = ProteinUtils.isFeatureChain(protein);

        if (isSpliceVariant || isFeatureChain){
            return true;
        }

        return false;
    }
}
