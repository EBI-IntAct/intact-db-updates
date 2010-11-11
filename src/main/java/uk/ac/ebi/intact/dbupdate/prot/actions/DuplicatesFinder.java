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
package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.commons.util.Crc64;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;

import java.util.*;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFinder {

    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    public DuplicatesFoundEvent findProteinDuplicates(UpdateCaseEvent evt) throws ProcessorException {
        String uniprotId = evt.getUniprotServiceResult().getQuerySentToService();

        // we filter the possible duplicates and keep those with an unique distinct uniprot identity. The others cannot be processed
        List<Protein> possibleDuplicates = new ArrayList<Protein>();

        possibleDuplicates.addAll(evt.getPrimaryProteins());
        possibleDuplicates.addAll(evt.getSecondaryProteins());

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                // get the uniprot proteins in case of range update
                UniprotProtein uniprotProtein = evt.getProtein();

                String uniprotSequence = null;
                String uniprotCrc64 = null;

                uniprotSequence = uniprotProtein.getSequence();
                uniprotCrc64 = uniprotProtein.getCrc64();

                // in case of master protein, we merge the duplicates
                DuplicatesFoundEvent duplEvt = new DuplicatesFoundEvent( processor,evt.getDataContext(),possibleDuplicates, uniprotSequence, uniprotCrc64);
                return duplEvt;
            }
            else {
                throw new ProcessorException("It is impossible to use this listener without a ProteinProcessor of type ProteinUpdateProcessor. The current protein event " +
                        "contains a source of type " + evt.getSource().getClass().getName());
            }
        }
        else {
            if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + uniprotId );
        }
        return null;
    }

    public Collection<DuplicatesFoundEvent> findIsoformsDuplicates(UpdateCaseEvent evt) throws ProcessorException {
        String uniprotId = evt.getUniprotServiceResult().getQuerySentToService();

        Collection<DuplicatesFoundEvent> duplicateEvents = new ArrayList<DuplicatesFoundEvent>();

        // we filter the possible duplicates and keep those with an unique distinct uniprot identity. The others cannot be processed
        List<ProteinTranscript> possibleDuplicates = new ArrayList<ProteinTranscript>();

        possibleDuplicates.addAll(evt.getPrimaryIsoforms());
        possibleDuplicates.addAll(evt.getSecondaryIsoforms());

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                // the collection containing all the possible duplicates
                Collection<ProteinTranscript> totalProteins = new ArrayList<ProteinTranscript>();
                totalProteins.addAll(possibleDuplicates);

                // the collection which will contain the duplicates
                Collection<ProteinTranscript> duplicates = new ArrayList<ProteinTranscript>();

                // while the list of possible duplicates has not been fully treated, we need to check the duplicates
                while (totalProteins.size() > 0){
                    duplicates.clear();

                    // pick the first protein of the list and add it in the list of duplicates
                    Iterator<ProteinTranscript> iterator = totalProteins.iterator();

                    ProteinTranscript trans = iterator.next();
                    Protein protToCompare = trans.getProtein();
                    duplicates.add(trans);

                    // extract the isoform parents of this protein
                    Collection<InteractorXref> isoformParent = ProteinUtils.extractIsoformParentCrossReferencesFrom(protToCompare);

                    // we compare the isoform parents of this first protein against the isoform parents of the other proteins
                    while (iterator.hasNext()){
                        // we extract the isoform parents of the next protein to compare
                        ProteinTranscript trans2 = iterator.next();
                        Protein proteinCompared = trans2.getProtein();
                        Collection<InteractorXref> isoformParent2 = ProteinUtils.extractIsoformParentCrossReferencesFrom(proteinCompared);

                        // if the isoform parents are identical, we ad the protein to the list of duplicates
                        if (CollectionUtils.isEqualCollection(isoformParent, isoformParent2)){
                            duplicates.add(trans2);
                        }
                    }

                    // if we have more than two proteins in the duplicate list, we merge them
                    if (duplicates.size() > 1){
                        UniprotProteinTranscript transcript = trans.getUniprotVariant();

                        String uniprotSequence = null;
                        String uniprotCrc64 = null;
                        if (transcript != null){
                            uniprotSequence = transcript.getSequence();
                            uniprotCrc64 = Crc64.getCrc64(uniprotSequence);
                        }

                        Collection<Protein> duplicateToFix = new ArrayList<Protein>();

                        for (ProteinTranscript t : duplicates){
                            duplicateToFix.add(t.getProtein());
                        }
                        DuplicatesFoundEvent duplEvt = new DuplicatesFoundEvent( processor,evt.getDataContext(),duplicateToFix, uniprotSequence, uniprotCrc64);
                        duplicateEvents.add(duplEvt);
                    }
                    else {
                        if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + protToCompare.getAc() );
                    }

                    // we remove the processed proteins from the list of protein to process
                    totalProteins.removeAll(duplicates);
                }
            }
            else {
                throw new ProcessorException("It is impossible to use this listener without a ProteinProcessor of type ProteinUpdateProcessor. The current protein event " +
                        "contains a source of type " + evt.getSource().getClass().getName());
            }
        }
        else {
            if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + uniprotId );
        }
        return duplicateEvents;
    }


    public Collection<DuplicatesFoundEvent> findFeatureChainDuplicates(UpdateCaseEvent evt) throws ProcessorException {
        String uniprotId = evt.getUniprotServiceResult().getQuerySentToService();

        Collection<DuplicatesFoundEvent> duplicateEvents = new ArrayList<DuplicatesFoundEvent>();

        // we filter the possible duplicates and keep those with an unique distinct uniprot identity. The others cannot be processed
        List<ProteinTranscript> possibleDuplicates = new ArrayList<ProteinTranscript>();

        possibleDuplicates.addAll(evt.getPrimaryFeatureChains());

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {
            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                // the collection containing all the possible duplicates
                Collection<ProteinTranscript> totalProteins = new ArrayList<ProteinTranscript>();
                totalProteins.addAll(possibleDuplicates);
                // the collection which will contain the duplicates
                Collection<ProteinTranscript> duplicates = new ArrayList<ProteinTranscript>();

                // while the list of possible duplicates has not been fully treated, we need to check the duplicates
                while (totalProteins.size() > 0){
                    duplicates.clear();

                    // pick the first protein of the list and add it in the list of duplicates
                    Iterator<ProteinTranscript> iterator = totalProteins.iterator();
                    ProteinTranscript trans = iterator.next();
                    Protein protToCompare = trans.getProtein();
                    duplicates.add(trans);

                    // extract the chain parents of this protein
                    Collection<InteractorXref> chainParents = ProteinUtils.extractChainParentCrossReferencesFrom(protToCompare);

                    // we compare the chain parents of this first protein against the chain parents of the other proteins
                    while (iterator.hasNext()){
                        // we extract the chain parents of the next protein to compare
                        ProteinTranscript trans2 = iterator.next();
                        Protein proteinCompared = trans2.getProtein();
                        Collection<InteractorXref> chainParent2 = ProteinUtils.extractChainParentCrossReferencesFrom(proteinCompared);

                        // if the chain parents are identical, we ad the protein to the list of duplicates
                        if (CollectionUtils.isEqualCollection(chainParents, chainParent2)){
                            duplicates.add(trans2);
                        }
                    }

                    // if we have more than two proteins in the duplicate list, we merge them
                    if (duplicates.size() > 1){

                        String uniprotSequence = null;
                        String uniprotCrc64 = null;

                        UniprotProteinTranscript transcript = trans.getUniprotVariant();

                        if (transcript != null){
                            uniprotSequence = transcript.getSequence();
                            uniprotCrc64 = Crc64.getCrc64(uniprotSequence);
                        }
                        Collection<Protein> duplicateToFix = new ArrayList<Protein>();

                        for (ProteinTranscript t : duplicates){
                            duplicateToFix.add(t.getProtein());
                        }

                        DuplicatesFoundEvent duplEvt = new DuplicatesFoundEvent( processor,evt.getDataContext(),duplicateToFix, uniprotSequence, uniprotCrc64);
                        duplicateEvents.add(duplEvt);
                    }
                    else {
                        if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + protToCompare.getAc() );
                    }
                    // we remove the processed proteins from the list of protein to process
                    totalProteins.removeAll(duplicates);
                }
            }
            else {
                throw new ProcessorException("It is impossible to use this listener without a ProteinProcessor of type ProteinUpdateProcessor. The current protein event " +
                        "contains a source of type " + evt.getSource().getClass().getName());
            }
        }
        else {
            if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + uniprotId );
        }
        return duplicateEvents;
    }

    /**
     *
     * @param possibleDuplicates
     * @param evt
     * @deprecated use the DuplicateFuxer listener
     */
    @Deprecated
    private void checkAndFixDuplication(List<? extends Protein> possibleDuplicates, ProteinEvent evt, String uniprotSequenceToUseForRangeShifting, String crc64) {
        List<Protein> realDuplicates = new ArrayList<Protein>();

        // here there is a chance we keep proteins that have an other identity than the one of the original
        // protein we were processing. Say in the case where it is at index > 0 in the list.

        Protein firstProtein = possibleDuplicates.get(0);

        for (int i = 1; i < possibleDuplicates.size(); i++) {
            Protein possibleDuplicate =  possibleDuplicates.get(i);

            if (ProteinUtils.containTheSameIdentities(firstProtein, possibleDuplicate)) {
                if (realDuplicates.isEmpty()) realDuplicates.add(firstProtein);
                realDuplicates.add(possibleDuplicate);
            }
        }

        if (!realDuplicates.isEmpty()) {
            // fire a duplication event
            final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent(processor, evt.getDataContext(), realDuplicates, uniprotSequenceToUseForRangeShifting, crc64));
        }
    }
}