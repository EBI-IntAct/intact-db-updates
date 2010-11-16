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
package uk.ac.ebi.intact.dbupdate.prot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.actions.*;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;

import javax.swing.event.EventListenerList;
import java.util.*;

/**
 * Iterates through the proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public abstract class ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    // to allow listener
    protected EventListenerList listenerList = new EventListenerList();

    private List<String> previousBatchACs;

    private boolean finalizationRequested;

    private Protein currentProtein;

    /**
     * The filter of no-uniprot-update and multi uniprot identities
     */
    private ProteinUpdateFilter updateFilter;
    /**
     * The updater for the uniprot identity cross references
     */
    private UniprotIdentityUpdater uniprotIdentityUpdater;
    /**
     * The uniprot protein retriever
     */
    private UniprotProteinRetriever uniprotRetriever;
    /**
     * The duplicate finder
     */
    private DuplicatesFinder duplicateFinder;
    /**
     * The duplicate fixer
     */
    private DuplicatesFixer duplicateFixer;
    /**
     * The protein deleter
     */
    private ProteinDeleter proteinDeleter;
    /**
     * The deleter of proteins without interactions
     */
    private ProtWithoutInteractionDeleter protWithoutInteractionDeleter;

    private OutOfDateParticipantFixer participantFixer;
    UniprotProteinUpdater updater;

    public ProteinProcessor() {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        previousBatchACs = new ArrayList<String>();
        updateFilter = new ProteinUpdateFilter();
        this.uniprotIdentityUpdater = new UniprotIdentityUpdater();
        this.uniprotRetriever = new UniprotProteinRetriever(config.getUniprotService());
        this.duplicateFinder = new DuplicatesFinder();
        this.duplicateFixer = new DuplicatesFixer();
        this.proteinDeleter = new ProteinDeleter();
        this.protWithoutInteractionDeleter = new ProtWithoutInteractionDeleter();
        protWithoutInteractionDeleter.setDeleteProteinTranscriptsWithoutInteractions(config.isDeleteProteinTranscriptWithoutInteractions());
        this.updater = new UniprotProteinUpdater(config.getTaxonomyService());
        this.participantFixer = new OutOfDateParticipantFixer();
    }

    /**
     *
     * @param batchSize
     * @param stepSize
     * @deprecated please use the default constructor instead.
     */
    @Deprecated
    public ProteinProcessor(int batchSize, int stepSize){
        this();
    }

    protected abstract void registerListeners();

    /**
     * Updates all the proteins in the database
     * @throws ProcessorException
     */
    public void updateAll() throws ProcessorException {

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        TransactionStatus transactionStatus = dataContext.beginTransaction();
        List<String> acs = dataContext.getDaoFactory().getEntityManager()
                .createQuery("select p.ac from ProteinImpl p order by p.created").getResultList();
        commitTransaction(transactionStatus);

        updateByACs(acs);
    }

    public void updateByACs(List<String> protACsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();

        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        Set<String> processedIntactProteins = new HashSet<String>();

        for (String protAc : protACsToUpdate) {
            TransactionStatus transactionStatus = dataContext.beginTransaction();
            ProteinImpl prot = dataContext.getDaoFactory().getProteinDao().getByAc(protAc);

            if (prot == null) {
                if (log.isWarnEnabled()) log.warn("Protein was not found in the database. Probably it was deleted already? "+protAc);
                continue;
            }

            // load annotations (to avoid lazyinitializationexceptions later)
            prot.getXrefs().size();
            prot.getAnnotations().size();

            if (!processedIntactProteins.contains(prot.getAc())){
                processedIntactProteins.addAll(update(prot));
            }

            try {
                dataContext.commitTransaction(transactionStatus);
            } catch (IntactTransactionException e) {
                throw new ProcessorException(e);
            }
        }
    }

    public void update(List<? extends Protein> protsToUpdate) throws ProcessorException {
        registerListenersIfNotDoneYet();
        Set<String> processedIntactProteins = new HashSet<String>();

        if (log.isTraceEnabled()) log.trace("Going to process "+protsToUpdate.size()+" proteins");

        for (Protein protToUpdate : protsToUpdate) {
            if (!processedIntactProteins.contains(protToUpdate.getAc())){
                processedIntactProteins.addAll(update(protToUpdate));
            }
            update(protToUpdate);
        }
    }

    public Set<String> update(Protein protToUpdate) throws ProcessorException {
        // the proteins processed during this update
        Set<String> processedProteins = new HashSet<String>();

        // register the listeners
        registerListenersIfNotDoneYet();
        // the current config
        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        // the protein to update
        this.currentProtein = protToUpdate;

        // the data context
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();

        // add the protein to the list of processed proteins
        processedProteins.add(protToUpdate.getAc());

        // create the event for this protein
        ProteinEvent processEvent = new ProteinEvent(this, dataContext, protToUpdate);

        // to know if the protein should be deleted
        boolean toDelete = false;

        // if we delete proteins without interactions
        if (config.isDeleteProtsWithoutInteractions()){
            if (log.isTraceEnabled()) log.trace("Checking for protein interactions : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+")");

            // true if the protein is not involved in any interactions
            toDelete = protWithoutInteractionDeleter.hasToBeDeleted(processEvent);
        }

        // if the protein must be deleted, delete it
        if (toDelete){
            proteinDeleter.delete(processEvent);
        }
        // the protein must not be deleted, update it
        else {
            if (log.isTraceEnabled()) log.trace("Filtering protein : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+") for uniprot update");

            // get the uniprot identity of this protein
            String uniprotIdentity = updateFilter.filterOnUniprotIdentity(processEvent);

            // if the protein has a uniprot identity
            if (uniprotIdentity != null){
                if (log.isTraceEnabled()) log.trace("Retrieving uniprot entry matching the protein : "+protToUpdate.getShortLabel()+" ("+protToUpdate.getAc()+"), "+uniprotIdentity+"");
                processEvent.setUniprotIdentity(uniprotIdentity);

                // get the uniprot protein
                UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(processEvent);

                // if the uniprot protein exists, start to update
                if (uniprotProtein != null){
                    processEvent.setUniprotProtein(uniprotProtein);

                    if (log.isTraceEnabled()) log.trace("Retrieving all intact proteins matcing the uniprot entry : "+uniprotIdentity);

                    // get all the proteins in intact matching primary and secondary acs of this uniprot protein. Get also all the splice variants and feature chains attached to this protein
                    UpdateCaseEvent caseEvent = uniprotIdentityUpdater.collectPrimaryAndSecondaryProteins(processEvent);

                    // add each protein to the list of processed proteins
                    for (Protein p : caseEvent.getUniprotServiceResult().getProteins()){
                        processedProteins.add(p.getAc());
                    }

                    // if we can delete proteins without interactions, delete all of the proteins attached to this uniprot entry without interactions
                    if (config.isDeleteProtsWithoutInteractions()){
                        if (log.isTraceEnabled()) log.trace("Checking for all protein interactions");

                        Set<Protein> protToDelete = protWithoutInteractionDeleter.collectProteinsWithoutInteractions(caseEvent);

                        for (Protein p : protToDelete){
                            ProteinEvent protEvent = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), p, uniprotProtein, "Protein without interactions");
                            proteinDeleter.delete(protEvent);
                        }
                    }

                    if (log.isTraceEnabled()) log.trace("Filtering " + caseEvent.getPrimaryProteins().size() + " primary proteins and " + caseEvent.getSecondaryProteins().size() + "secondary proteins for uniprot update." );

                    // filter on 'no-uniprot-update' and multi identities
                    updateFilter.filterNonUniprotAndMultipleUniprot(caseEvent);

                    if (log.isTraceEnabled()) log.trace("Checking that it is possible to update existing secondary proteins for " + uniprotProtein.getPrimaryAc() );

                    // filter on the proteins matching a single uniprot protein
                    uniprotRetriever.filterAllSecondaryProteinsPossibleToUpdate(caseEvent);

                    // secondary acs
                    if (!caseEvent.getSecondaryProteins().isEmpty() || !caseEvent.getSecondaryIsoforms().isEmpty()){
                        uniprotIdentityUpdater.updateAllSecondaryProteins(caseEvent);
                    }

                    // the master protein in IntAct
                    Protein masterProtein = null;

                    // if there are some duplicates and we can fix them, merge them
                    if (caseEvent.getPrimaryProteins().size() > 1){
                        if (config.isFixDuplicates()){
                            if (log.isTraceEnabled()) log.trace("Check for possible duplicates." );

                            DuplicatesFoundEvent duplicateEvent = duplicateFinder.findProteinDuplicates(caseEvent);

                            // we found real duplicates, we merge them
                            if (duplicateEvent != null){
                                if (log.isTraceEnabled()) log.trace("Fix the duplicates." );

                                DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplicateEvent);

                                if (!report.getComponentsWithFeatureConflicts().isEmpty()){
                                    for (Map.Entry<Protein, Collection<Component>> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                                        OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                                        ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                                        if (fixedProtein != null){

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
                                                            ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                            caseEvent.getPrimaryIsoforms().add(fixedProtein);
                                                            caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                                        ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                        caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                                        caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                                                    }
                                                }
                                            }

                                            if (entry.getKey().getActiveInstances().isEmpty()){
                                                ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                                                proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey()));
                                            }
                                        }
                                    }
                                }

                                // the master protein is the result of the merge
                                if (report.getOriginalProtein() != null){
                                    masterProtein = report.getOriginalProtein();
                                    caseEvent.getPrimaryProteins().clear();
                                    caseEvent.getPrimaryProteins().add(masterProtein);
                                }
                            }
                        }
                    }

                    try {
                        // update master protein first
                        // update the protein
                        updater.createOrUpdateProtein(caseEvent);
                    } catch (ProteinServiceException e) {
                        caseEvent.getUniprotServiceResult().addException(e);
                    }

                    // update isoforms
                    //isoform duplicates to merge
                    if (caseEvent.getPrimaryIsoforms().size() > 1){
                        if (config.isFixDuplicates()){
                            if (log.isTraceEnabled()) log.trace("Check for possible isoform duplicates." );

                            Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findIsoformsDuplicates(caseEvent);

                            if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                            Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>();

                            for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                                DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplEvt);

                                String originalAc = report.getOriginalProtein().getAc();

                                for (ProteinTranscript p : caseEvent.getPrimaryIsoforms()){
                                    if (originalAc.equals(p.getProtein().getAc())){
                                        report.setTranscript(p.getUniprotVariant());
                                    }
                                }

                                if (report.getTranscript() == null){
                                    for (ProteinTranscript p : caseEvent.getSecondaryIsoforms()){
                                        if (originalAc.equals(p.getProtein().getAc())){
                                            report.setTranscript(p.getUniprotVariant());
                                        }
                                    }
                                }

                                if (!report.getComponentsWithFeatureConflicts().isEmpty()){
                                    for (Map.Entry<Protein, Collection<Component>> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                                        OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                                        ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                                        if (fixedProtein != null){
                                            ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
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
                                                            ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                            mergedIsoforms.add(fixedProtein);
                                                            caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                                        ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                        caseEvent.getPrimaryFeatureChains().add(fixedProtein);
                                                        caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                                                    }
                                                }
                                            }

                                            if (entry.getKey().getActiveInstances().isEmpty()){
                                                ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                                                proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey()));
                                            }
                                        }
                                    }
                                }

                                if (report.getOriginalProtein() != null){
                                    mergedIsoforms.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
                                }
                            }

                            if (!mergedIsoforms.isEmpty()){
                                caseEvent.getPrimaryIsoforms().clear();
                                caseEvent.getPrimaryIsoforms().addAll(mergedIsoforms);
                            }
                        }
                    }

                    // update feature chains

                    //chain duplicates to merge
                    if (caseEvent.getPrimaryFeatureChains().size() > 1){
                        if (config.isFixDuplicates()){
                            if (log.isTraceEnabled()) log.trace("Check for possible feature chains duplicates." );

                            Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findFeatureChainDuplicates(caseEvent);

                            if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                            Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>();

                            for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                                DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplEvt);

                                String originalAc = report.getOriginalProtein().getAc();

                                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                                    if (originalAc.equals(p.getProtein().getAc())){
                                        report.setTranscript(p.getUniprotVariant());
                                    }
                                }

                                if (!report.getComponentsWithFeatureConflicts().isEmpty()){
                                    for (Map.Entry<Protein, Collection<Component>> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                                        OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                                        ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                                        if (fixedProtein != null){

                                            ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
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
                                                            ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                            caseEvent.getPrimaryIsoforms().add(fixedProtein);
                                                            caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
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
                                                        ProteinTools.updateProteinTranscripts(caseEvent.getDataContext().getDaoFactory(), report.getOriginalProtein(), entry.getKey());
                                                        mergedChains.add(fixedProtein);
                                                        caseEvent.getUniprotServiceResult().getProteins().add(fixedProtein.getProtein());
                                                    }
                                                }
                                            }

                                            if (entry.getKey().getActiveInstances().isEmpty()){
                                                ProteinTools.addIntactSecondaryReferences(fixedProtein.getProtein(), entry.getKey(), caseEvent.getDataContext().getDaoFactory());
                                                proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey()));
                                            }
                                        }
                                    }
                                }

                                if (report.getOriginalProtein() != null){
                                    mergedChains.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
                                }
                            }

                            if (!mergedChains.isEmpty()){
                                caseEvent.getPrimaryFeatureChains().clear();
                                caseEvent.getPrimaryFeatureChains().addAll(mergedChains);
                            }
                        }
                    }

                    if (!caseEvent.getPrimaryIsoforms().isEmpty()){
                        if (masterProtein == null && !caseEvent.getPrimaryProteins().isEmpty()){
                            masterProtein = caseEvent.getPrimaryProteins().iterator().next();

                            try {
                                updater.createOrUpdateIsoform(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }
                        else if (masterProtein == null && caseEvent.getPrimaryProteins().isEmpty()){
                            caseEvent.getUniprotServiceResult().addException( new ProcessorException("The splice variants of " + uniprotProtein.getPrimaryAc() + " don't have a valid master protein."));
                        }
                        else {
                            try {
                                updater.createOrUpdateIsoform(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }
                    }

                    if (!caseEvent.getPrimaryFeatureChains().isEmpty()){
                        if (masterProtein == null && !caseEvent.getPrimaryProteins().isEmpty()){
                            masterProtein = caseEvent.getPrimaryProteins().iterator().next();

                            try {
                                updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }
                        else if (masterProtein == null && caseEvent.getPrimaryProteins().isEmpty()){
                            caseEvent.getUniprotServiceResult().addException( new ProcessorException("The feature chains of " + uniprotProtein.getPrimaryAc() + " don't have a valid master protein."));
                        }
                        else {
                            try {
                                updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }
                    }
                }
            }
        }

        return processedProteins;
    }

    public void finalizeAfterCurrentPhase() {
        finalizationRequested = true;
    }

    public boolean isFinalizationRequested() {
        return finalizationRequested;
    }

    // listener methods
    public void addListener(ProteinUpdateProcessorListener listener) {
        listenerList.add(ProteinUpdateProcessorListener.class, listener);
    }

    public void removeListener(ProteinUpdateProcessorListener listener) {
        listenerList.remove(ProteinUpdateProcessorListener.class, listener);
    }

    protected <T> List<T> getListeners(Class<T> listenerClass) {
        List list = new ArrayList();

        Object[] listeners = listenerList.getListenerList();

        for (int i = 0; i < listeners.length; i += 2) {
            if (listeners[i] == ProteinUpdateProcessorListener.class) {
                if (listenerClass.isAssignableFrom(listeners[i+1].getClass())) {
                    list.add(listeners[i+1]);
                }
            }
        }
        return list;
    }

    public void fireOnPreProcess(ProteinEvent evt) {
        for (ProteinProcessorListener listener : getListeners(ProteinProcessorListener.class)) {
            if (isFinalizationRequested()) {
                log.debug("Processor finalization already requested. Skipping: " + listener.getClass());
                return;
            }
            listener.onPreProcess(evt);
        }
    }

    public void fireOnProcess(ProteinEvent evt) {
        for (ProteinProcessorListener listener : getListeners(ProteinProcessorListener.class)) {
            if (isFinalizationRequested()) {
                log.debug("Processor finalization already requested. Skipping: " + listener.getClass());
                return;
            }
            listener.onProcess(evt);
        }
    }

    private void registerListenersIfNotDoneYet() {
        if (listenerList.getListenerCount() == 0) {
            registerListeners();
        }

        if (listenerList.getListenerCount() == 0) {
            throw new IllegalStateException("No listener registered for ProteinProcessor");
        }
    }

    // other private methods

    private void commitTransaction(TransactionStatus transactionStatus) throws ProcessorException {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        try {
            dataContext.commitTransaction(transactionStatus);
        } catch (IntactTransactionException e) {
            throw new ProcessorException("Problem committing", e);
        }
    }

    public Protein getCurrentProtein() {
        return currentProtein;
    }
}