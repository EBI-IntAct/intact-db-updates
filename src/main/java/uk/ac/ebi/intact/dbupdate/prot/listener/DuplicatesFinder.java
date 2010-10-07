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
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.*;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFinder extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        final DataContext dataContext = evt.getDataContext();
        final Protein protein = evt.getProtein();

        if (protein.getAc() == null) {
            throw new IllegalArgumentException("We need an AC to check for duplicates: "+protInfo(protein));
        }

        if (log.isDebugEnabled()) log.debug("Searching for duplicates in the database for: "+protInfo(protein));

        // ignore proteins that cannot be updated from uniprot
        if (!ProteinUtils.isFromUniprot(protein)) {
            if (log.isDebugEnabled()) log.debug("Duplicate search ignored for proteins that do not exist in UniprotKb: "+protInfo(protein));
            return;
        }

//        // ignore splice variants for now, as they follow additional criteria to distinguish between them
//        if (ProteinUtils.isSpliceVariant(protein)) {
//            // TODO do handle splice variant duplicate, as well as feature chains
//            if (log.isDebugEnabled()) log.debug("--- [NEW] --- Duplicated splice variant: "+protInfo(protein));
////            if (log.isDebugEnabled()) log.debug("Duplicate search ignored for splice variant: "+protInfo(protein));
////            return;
//        }

        final List<InteractorXref> identities = ProteinUtils.getIdentityXrefs( protein );
        if( identities.size() != 1 ) {
            if ( log.isDebugEnabled() )
                log.debug( "Protein " + protInfo(protein)+ " has " + identities.size() + " Xref(identity), expected only 1. Skip.");
            return;
        }

        final InteractorXref identity = identities.iterator().next();
        if( ! CvDatabase.UNIPROT_MI_REF.equals( identity.getCvDatabase().getIdentifier() ) ) {
            if ( log.isDebugEnabled() )
                log.debug( "Protein " + protInfo(protein)+ " has an Xref("+ identity.getCvDatabase().getShortLabel() +
                        ", identity) while uniprotkb expected. Skip.");
            return;
        }

        ProteinDao proteinDao = dataContext.getDaoFactory().getProteinDao();
        final List<ProteinImpl> possibleDuplicates =
                proteinDao.getByXrefLike( identity.getCvDatabase(),
                        identity.getCvXrefQualifier(),
                        identity.getPrimaryId() );

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {
            final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            // in case of splice variant, we check that the list of isoform parents is the same before merging duplicates
            if (ProteinUtils.isSpliceVariant(protein)){

                // the collection containing all the possible duplicates
                Collection<Protein> totalProteins = new ArrayList<Protein>();
                totalProteins.addAll(possibleDuplicates);

                // the collection which will contain the duplicates
                Collection<Protein> duplicates = new ArrayList<Protein>();

                // while the list of possible duplicates has not been fully treated, we need to check the duplicates
                while (totalProteins.size() > 0){
                    duplicates.clear();

                    // pick the first protein of the list and add it in the list of duplicates
                    Iterator<Protein> iterator = totalProteins.iterator();
                    Protein protToCompare = iterator.next();
                    duplicates.add(protToCompare);

                    // extract the isoform parents of this protein
                    Collection<InteractorXref> isoformParent = ProteinUtils.extractIsoformParentCrossReferencesFrom(protToCompare);

                    // we compare the isoform parents of this first protein against the isoform parents of the other proteins
                    while (iterator.hasNext()){
                        // we extract the isoform parents of the next protein to compare
                        Protein proteinCompared = iterator.next();
                        Collection<InteractorXref> isoformParent2 = ProteinUtils.extractIsoformParentCrossReferencesFrom(proteinCompared);

                        // if the isoform parents are identical, we ad the protein to the list of duplicates
                        if (CollectionUtils.isEqualCollection(isoformParent, isoformParent2)){
                            duplicates.add(proteinCompared);
                        }
                    }

                    // if we have more than two proteins in the duplicate list, we merge them
                    if (duplicates.size() > 1){
                        processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent( processor,
                                evt.getDataContext(),
                                duplicates));
                    }

                    // we remove the processed proteins from the list of protein to process
                    totalProteins.removeAll(duplicates);
                }
            }
            // in case of feature chain, we check that the list of chain parents is the same before merging duplicates            
            else if (ProteinUtils.isFeatureChain(protein)){
                // the collection containing all the possible duplicates
                Collection<ProteinImpl> totalProteins = new ArrayList<ProteinImpl>();
                totalProteins.addAll(possibleDuplicates);
                 // the collection which will contain the duplicates
                Collection<Protein> duplicates = new ArrayList<Protein>();

                // while the list of possible duplicates has not been fully treated, we need to check the duplicates
                while (totalProteins.size() > 0){
                    duplicates.clear();

                    // pick the first protein of the list and add it in the list of duplicates
                    Iterator<ProteinImpl> iterator = totalProteins.iterator();
                    ProteinImpl protToCompare = iterator.next();
                    duplicates.add(protToCompare);

                    // extract the chain parents of this protein                    
                    Collection<InteractorXref> chainParents = ProteinUtils.extractChainParentCrossReferencesFrom(protToCompare);

                    // we compare the chain parents of this first protein against the chain parents of the other proteins
                    while (iterator.hasNext()){
                        // we extract the chain parents of the next protein to compare
                        ProteinImpl proteinCompared = iterator.next();
                        Collection<InteractorXref> chainParent2 = ProteinUtils.extractChainParentCrossReferencesFrom(proteinCompared);

                        // if the chain parents are identical, we ad the protein to the list of duplicates
                        if (CollectionUtils.isEqualCollection(chainParents, chainParent2)){
                            duplicates.add(proteinCompared);
                        }
                    }

                    // if we have more than two proteins in the duplicate list, we merge them
                    if (duplicates.size() > 1){
                        processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent( processor,
                                evt.getDataContext(),
                                duplicates));
                    }
                    // we remove the processed proteins from the list of protein to process                    
                    totalProteins.removeAll(duplicates);
                }
            }
            // in case of master protein, we merge the duplicates
            else {
                processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent( processor,
                                evt.getDataContext(),
                                new ArrayList<Protein> (possibleDuplicates)));
            }

//            checkAndFixDuplication(protein, possibleDuplicates, evt);
        } else {
            if (log.isDebugEnabled()) log.debug( "No duplicates found for: " + protInfo(protein) );
        }
    }

    /**
     *
     * @param possibleDuplicates
     * @param evt
     * @deprecated use the DuplicateFuxer listener
     */
    @Deprecated
    private void checkAndFixDuplication(List<? extends Protein> possibleDuplicates, ProteinEvent evt) {
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
            processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent(processor, evt.getDataContext(), realDuplicates));
        }
    }
}