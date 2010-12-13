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
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DbInfoDao;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.*;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.meta.DbInfo;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

import java.text.SimpleDateFormat;
import java.util.*;

/**
 * Updates the database proteins using the latest information from UniProt.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessor extends ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );

    public ProteinUpdateProcessor(){
        super();
    }

    public ProteinUpdateProcessor(ProteinUpdateProcessorConfig configUpdate){
        this();
        ProteinUpdateContext.getInstance().setConfig( configUpdate );
    }

    @Override
    public void updateAll() throws ProcessorException {

        boolean global = ProteinUpdateContext.getInstance().getConfig().isGlobalProteinUpdate();
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( true );

        super.updateAll();

        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( global );

        // update db info accordingly
        String lastProtUpdate = new SimpleDateFormat("dd-MMM-yy").format(new Date());

        saveOrUpdateDbInfo("last_protein_update", lastProtUpdate);
        saveOrUpdateDbInfo("uniprotkb.version", UniProtJAPI.factory.getVersion());
    }

    private void saveOrUpdateDbInfo(String key, String value) {
        DataContext dataContext = IntactContext.getCurrentInstance().getDataContext();
        final TransactionStatus transactionStatus = dataContext.beginTransaction();

        DbInfoDao dbInfoDao = dataContext.getDaoFactory().getDbInfoDao();
        DbInfo dbInfo = dbInfoDao.get(key);

        if (dbInfo == null) {
            dbInfo = new DbInfo(key, value);
            dbInfoDao.persist(dbInfo);
        } else {
            dbInfo.setValue(value);
            dataContext.getDaoFactory().getEntityManager().merge(dbInfo);
        }

        dataContext.commitTransaction(transactionStatus);
    }

    protected void registerListeners() {
        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        addListener(new LoggingProcessorListener());

        if (config.getReportHandler() != null) {
            addListener(new ReportWriterListener(config.getReportHandler()));
        }

        addListener(new SequenceChangedListener());
    }

    public void fireOnDelete(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDelete(evt);
        }
    }

    public void fireOnProteinDuplicationFound(DuplicatesFoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinDuplicationFound(evt);
        }
    }

    public void fireOnProteinSequenceChanged(ProteinSequenceChangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinSequenceChanged(evt);
        }
    }

    public void fireOnProteinCreated(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinCreated(evt);
        }
    }

    public void fireNonUniprotProteinFound(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onNonUniprotProteinFound(evt);
        }
    }

    public void fireOnProcessErrorFound(UpdateErrorEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProcessErrorFound(evt);
        }
    }

    public void fireOnUpdateCase(UpdateCaseEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onUpdateCase(evt);
        }
    }

    public void fireOnRangeChange(RangeChangedEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onRangeChanged(evt);
        }
    }

    public void fireOnInvalidRange(InvalidRangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onInvalidRange(evt);
        }
    }

    public void fireOnOutOfDateRange(InvalidRangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onOutOfDateRange(evt);
        }
    }

    public void fireOnUniprotDeadEntry(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDeadProteinFound(evt);
        }
    }

    public void fireOnOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onOutOfDateParticipantFound(evt);
        }
    }

    public void fireOnSecondaryAcsFound(UpdateCaseEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onSecondaryAcsFound(evt);
        }
    }

    public void fireOnProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinTranscriptWithSameSequence(evt);
        }
    }

    public void fireOnInvalidIntactParentFound(InvalidIntactParentFoundEvent evt){
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onInvalidIntactParent(evt);
        }
    }

    @Override
    public List<Protein> retrieveAndUpdateProteinFromUniprot(String uniprotAc) throws ProcessorException{
        List<Protein> intactProteins = super.retrieveAndUpdateProteinFromUniprot(uniprotAc);

        // register the listeners
        if (getListeners(SequenceChangedListener.class) == null){
            addListener(new SequenceChangedListener());
        }

        // the current config
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        if (uniprotAc == null){
            throw new ProcessorException("The uniprot ac should not be null");
        }

        // get the uniprot protein
        UniprotProtein uniprotProtein = uniprotRetriever.retrieveUniprotEntry(uniprotAc);

        // if the uniprot protein exists, start to update
        if (uniprotProtein != null){
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                if (log.isTraceEnabled()) log.trace("Retrieving all intact proteins matcing the uniprot entry : "+uniprotAc);

                ProteinEvent processEvent = new ProteinEvent(this, context, null);
                processEvent.setUniprotIdentity(uniprotAc);
                processEvent.setUniprotProtein(uniprotProtein);

                // get all the proteins in intact matching primary and secondary acs of this uniprot protein. Get also all the splice variants and feature chains attached to this protein
                UpdateCaseEvent caseEvent = uniprotIdentityUpdater.collectPrimaryAndSecondaryProteins(processEvent);

                // if we can delete proteins without interactions, delete all of the proteins attached to this uniprot entry without interactions
                if (config.isDeleteProtsWithoutInteractions()){
                    if (log.isTraceEnabled()) log.trace("Checking for all protein interactions");

                    Set<Protein> protToDelete = protWithoutInteractionDeleter.collectAndRemoveProteinsWithoutInteractions(caseEvent);

                    for (Protein p : protToDelete){
                        ProteinEvent protEvent = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), p, uniprotProtein, "Protein without interactions");
                        proteinDeleter.delete(protEvent);
                    }
                }

                List<Protein> transcriptsWithoutParents = parentUpdater.checkConsistencyOfAllTranscripts(caseEvent);

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
                    for (Protein prot : caseEvent.getPrimaryProteins()){
                        if (prot.getSequence() != null){
                            UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), caseEvent.getProtein());

                            if (transcriptsWithSameSequence != null){
                                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                                    processor.fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(processor, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                }
                            }
                        }
                    }

                    if (config.isFixDuplicates()){
                        if (log.isTraceEnabled()) log.trace("Check for possible duplicates." );

                        DuplicatesFoundEvent duplicateEvent = duplicateFinder.findProteinDuplicates(caseEvent);

                        // we found real duplicates, we merge them
                        if (duplicateEvent != null){
                            if (log.isTraceEnabled()) log.trace("Fix the duplicates." );

                            DuplicateReport report = processDuplicatesProtein(caseEvent, duplicateEvent);

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

                if (!transcriptsWithoutParents.isEmpty()){
                    if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                        ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) caseEvent.getSource();

                        if (masterProtein != null){
                            parentUpdater.createParentXRefs(transcriptsWithoutParents, masterProtein, context, updateProcessor);
                        }
                        else if (!caseEvent.getPrimaryProteins().isEmpty()){
                            parentUpdater.createParentXRefs(transcriptsWithoutParents, caseEvent.getPrimaryProteins().iterator().next(), context, updateProcessor);
                        }
                    }
                }

                // update isoforms
                //isoform duplicates to merge
                if (caseEvent.getPrimaryIsoforms().size() > 1 ){
                    for (ProteinTranscript trans : caseEvent.getPrimaryIsoforms()){
                        Protein prot = trans.getProtein();

                        if (prot.getSequence() != null){
                            UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), trans.getUniprotVariant(), caseEvent.getProtein());

                            if (transcriptsWithSameSequence != null){
                                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                                    processor.fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(processor, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                }
                            }
                        }
                    }

                    if (config.isFixDuplicates()){
                        if (log.isTraceEnabled()) log.trace("Check for possible isoform duplicates." );

                        Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findIsoformsDuplicates(caseEvent);

                        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                        Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>();

                        for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                            processDuplicatesTranscript(caseEvent, mergedIsoforms, duplEvt);
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
                    for (ProteinTranscript trans : caseEvent.getPrimaryFeatureChains()){
                        Protein prot = trans.getProtein();

                        if (prot.getSequence() != null){
                            UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), trans.getUniprotVariant(), caseEvent.getProtein());

                            if (transcriptsWithSameSequence != null){
                                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                                    processor.fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(processor, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                }
                            }
                        }
                    }

                    if (config.isFixDuplicates()){
                        if (log.isTraceEnabled()) log.trace("Check for possible feature chains duplicates." );

                        Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findFeatureChainDuplicates(caseEvent);

                        if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                        Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>();

                        for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                            processDuplicatesTranscript(caseEvent, mergedChains, duplEvt);
                        }

                        if (!mergedChains.isEmpty()){
                            caseEvent.getPrimaryFeatureChains().clear();
                            caseEvent.getPrimaryFeatureChains().addAll(mergedChains);
                        }
                    }

                }

                boolean canUpdateProteinTranscript = false;

                if (masterProtein == null && caseEvent.getPrimaryProteins().size() == 1){
                    masterProtein = caseEvent.getPrimaryProteins().iterator().next();
                    canUpdateProteinTranscript = true;
                }
                else if (masterProtein == null && caseEvent.getPrimaryProteins().size() != 1){
                    caseEvent.getUniprotServiceResult().addException( new ProcessorException("The splice variants of " + uniprotProtein.getPrimaryAc() + " cannot be updated because we found " + caseEvent.getPrimaryProteins().size() + " possible master proteins in IntAct"));
                }
                else {
                    canUpdateProteinTranscript = true;
                }

                if ((!caseEvent.getPrimaryIsoforms().isEmpty() || (caseEvent.getPrimaryIsoforms().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) && canUpdateProteinTranscript){
                    try {
                        updater.createOrUpdateIsoform(caseEvent, masterProtein);
                    } catch (ProteinServiceException e) {
                        caseEvent.getUniprotServiceResult().addException(e);
                    }
                }

                if ((!caseEvent.getPrimaryFeatureChains().isEmpty() || (caseEvent.getPrimaryFeatureChains().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) && canUpdateProteinTranscript){
                    try {
                        updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                    } catch (ProteinServiceException e) {
                        caseEvent.getUniprotServiceResult().addException(e);
                    }
                }

                if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                    processor.fireOnUpdateCase(caseEvent);
                }
                intactProteins.addAll(caseEvent.getPrimaryProteins());
                intactProteins.addAll(caseEvent.getSecondaryProteins());

                for (ProteinTranscript pt : caseEvent.getPrimaryIsoforms()){
                    intactProteins.add(pt.getProtein());
                }
                for (ProteinTranscript pt : caseEvent.getSecondaryIsoforms()){
                    intactProteins.add(pt.getProtein());
                }
                for (ProteinTranscript pt : caseEvent.getPrimaryFeatureChains()){
                    intactProteins.add(pt.getProtein());
                }

                context.commitTransaction(status);
            }  catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                if (!status.isCompleted()){
                    context.rollbackTransaction(status);
                }
            }
        }
        else if (config.isProcessProteinNotFoundInUniprot()){
            DataContext context = IntactContext.getCurrentInstance().getDataContext();
            TransactionStatus status = context.beginTransaction();
            try {
                List<ProteinImpl> deadProteins = IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByUniprotId(uniprotAc);

                if (!deadProteins.isEmpty()){
                    for (ProteinImpl prot : deadProteins){
                        ProteinEvent evt = new ProteinEvent(this, context, prot);

                        if (evt.getSource() instanceof ProteinUpdateProcessor){
                            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                            processor.fireOnUniprotDeadEntry(evt);
                        }
                    }
                }

                context.commitTransaction(status);
            } catch (Exception e) {
                log.fatal("We failed to update the protein " + uniprotAc);
                if (!status.isCompleted()){
                    context.rollbackTransaction(status);
                }
            }
        }

        return intactProteins;
    }

    @Override
    public Set<String> update(Protein protToUpdate, DataContext dataContext) throws ProcessorException {
        // the proteins processed during this update
        Set<String> processedProteins = super.update(protToUpdate, dataContext);

        // register the listeners
        registerListenersIfNotDoneYet();
        // the current config
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        // the protein to update
        this.currentProtein = protToUpdate;

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

            boolean canBeUpdated = true;

            if (ProteinTools.isFeatureChain(protToUpdate) || ProteinUtils.isSpliceVariant(protToUpdate)){
                canBeUpdated = parentUpdater.checkConsistencyProteinTranscript(processEvent);
            }

            if (canBeUpdated){
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

                            Set<Protein> protToDelete = protWithoutInteractionDeleter.collectAndRemoveProteinsWithoutInteractions(caseEvent);

                            for (Protein p : protToDelete){
                                ProteinEvent protEvent = new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), p, uniprotProtein, "Protein without interactions");
                                proteinDeleter.delete(protEvent);
                            }
                        }

                        List<Protein> transcriptsWithoutParents = parentUpdater.checkConsistencyOfAllTranscripts(caseEvent);

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
                            for (Protein prot : caseEvent.getPrimaryProteins()){
                                if (prot.getSequence() != null){
                                    UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), caseEvent.getProtein());

                                    if (transcriptsWithSameSequence != null){
                                            fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(this, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                    }
                                }
                            }

                            if (config.isFixDuplicates()){
                                if (log.isTraceEnabled()) log.trace("Check for possible duplicates." );

                                DuplicatesFoundEvent duplicateEvent = duplicateFinder.findProteinDuplicates(caseEvent);

                                // we found real duplicates, we merge them
                                if (duplicateEvent != null){
                                    if (log.isTraceEnabled()) log.trace("Fix the duplicates." );

                                    DuplicateReport report = processDuplicatesProtein(caseEvent, duplicateEvent);

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

                        if (!transcriptsWithoutParents.isEmpty()){

                            if (masterProtein != null){
                                parentUpdater.createParentXRefs(transcriptsWithoutParents, masterProtein, dataContext, this);
                            }
                            else if (!caseEvent.getPrimaryProteins().isEmpty()){
                                parentUpdater.createParentXRefs(transcriptsWithoutParents, caseEvent.getPrimaryProteins().iterator().next(), dataContext, this);
                            }
                        }

                        // update isoforms
                        //isoform duplicates to merge
                        if (caseEvent.getPrimaryIsoforms().size() > 1 ){

                            for (ProteinTranscript trans : caseEvent.getPrimaryIsoforms()){
                                Protein prot = trans.getProtein();

                                if (prot.getSequence() != null){
                                    UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), trans.getUniprotVariant(), caseEvent.getProtein());

                                    if (transcriptsWithSameSequence != null){
                                        fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(this, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                    }
                                }
                            }

                            if (config.isFixDuplicates()){
                                if (log.isTraceEnabled()) log.trace("Check for possible isoform duplicates." );

                                Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findIsoformsDuplicates(caseEvent);

                                if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                                Collection<ProteinTranscript> mergedIsoforms = new ArrayList<ProteinTranscript>();

                                for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                                    processDuplicatesTranscript(caseEvent, mergedIsoforms, duplEvt);
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

                            for (ProteinTranscript trans : caseEvent.getPrimaryFeatureChains()){
                                Protein prot = trans.getProtein();

                                if (prot.getSequence() != null){
                                    UniprotProteinTranscript transcriptsWithSameSequence = participantFixer.findTranscriptsWithIdenticalSequence(prot.getSequence(), trans.getUniprotVariant(), caseEvent.getProtein());

                                    if (transcriptsWithSameSequence != null){
                                        fireOnProteinTranscriptWithSameSequence(new ProteinTranscriptWithSameSequenceEvent(this, caseEvent.getDataContext(), prot, uniprotProtein, transcriptsWithSameSequence.getPrimaryAc()));
                                    }
                                }
                            }

                            if (config.isFixDuplicates()){
                                if (log.isTraceEnabled()) log.trace("Check for possible feature chains duplicates." );

                                Collection<DuplicatesFoundEvent> duplicateEvents = duplicateFinder.findFeatureChainDuplicates(caseEvent);

                                if (log.isTraceEnabled()) log.trace("Fix the duplicates." );
                                Collection<ProteinTranscript> mergedChains = new ArrayList<ProteinTranscript>();

                                for (DuplicatesFoundEvent duplEvt : duplicateEvents){
                                    processDuplicatesTranscript(caseEvent, mergedChains, duplEvt);
                                }

                                if (!mergedChains.isEmpty()){
                                    caseEvent.getPrimaryFeatureChains().clear();
                                    caseEvent.getPrimaryFeatureChains().addAll(mergedChains);
                                }
                            }

                        }

                        boolean canUpdateProteinTranscript = false;

                        if (masterProtein == null && caseEvent.getPrimaryProteins().size() == 1){
                            masterProtein = caseEvent.getPrimaryProteins().iterator().next();
                            canUpdateProteinTranscript = true;
                        }
                        else if (masterProtein == null && caseEvent.getPrimaryProteins().size() != 1){
                            caseEvent.getUniprotServiceResult().addException( new ProcessorException("The splice variants of " + uniprotProtein.getPrimaryAc() + " cannot be updated because we found " + caseEvent.getPrimaryProteins().size() + " possible master proteins in IntAct"));
                        }
                        else {
                            canUpdateProteinTranscript = true;
                        }

                        if ((!caseEvent.getPrimaryIsoforms().isEmpty() || (caseEvent.getPrimaryIsoforms().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) && canUpdateProteinTranscript){
                            try {
                                updater.createOrUpdateIsoform(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }

                        if ((!caseEvent.getPrimaryFeatureChains().isEmpty() || (caseEvent.getPrimaryFeatureChains().size() == 0 && !config.isGlobalProteinUpdate() && !config.isDeleteProteinTranscriptWithoutInteractions())) && canUpdateProteinTranscript){
                            try {
                                updater.createOrUpdateFeatureChain(caseEvent, masterProtein);
                            } catch (ProteinServiceException e) {
                                caseEvent.getUniprotServiceResult().addException(e);
                            }
                        }

                        for (Protein p : caseEvent.getUniprotServiceResult().getProteins()){
                            processedProteins.add(p.getAc());
                        }

                        if (caseEvent.getSource() instanceof ProteinUpdateProcessor){
                            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) caseEvent.getSource();
                            processor.fireOnUpdateCase(caseEvent);
                        }
                    }
                }
            }
        }

        return processedProteins;
    }

    private void processDuplicatesTranscript(UpdateCaseEvent caseEvent, Collection<ProteinTranscript> mergedTranscripts, DuplicatesFoundEvent duplEvt) {
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

            if (report.getTranscript() == null){
                for (ProteinTranscript p : caseEvent.getPrimaryFeatureChains()){
                    if (originalAc.equals(p.getProtein().getAc())){
                        report.setTranscript(p.getUniprotVariant());
                    }
                }
            }
        }

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

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
                                    mergedTranscripts.add(fixedProtein);
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
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    fireNonUniprotProteinFound(new ProteinEvent(this, caseEvent.getDataContext(), entry.getKey()));
                }
            }
        }

        if (report.getOriginalProtein() != null){
            mergedTranscripts.add(new ProteinTranscript(report.getOriginalProtein(), report.getTranscript()));
        }
    }

    private DuplicateReport processDuplicatesProtein(UpdateCaseEvent caseEvent, DuplicatesFoundEvent duplicateEvent) {
        DuplicateReport report = duplicateFixer.fixProteinDuplicates(duplicateEvent);

        if (!report.getComponentsWithFeatureConflicts().isEmpty()){
            for (Map.Entry<Protein, RangeUpdateReport> entry : report.getComponentsWithFeatureConflicts().entrySet()){
                OutOfDateParticipantFoundEvent participantEvt = new OutOfDateParticipantFoundEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getValue().getInvalidComponents().keySet(), entry.getKey(), caseEvent.getProtein(), caseEvent.getPrimaryIsoforms(), caseEvent.getSecondaryIsoforms(), caseEvent.getPrimaryFeatureChains());
                ProteinTranscript fixedProtein = participantFixer.fixParticipantWithRangeConflicts(participantEvt, false);

                rangeFixer.processInvalidRanges(entry.getKey(), caseEvent, caseEvent.getUniprotServiceResult().getQuerySentToService(), entry.getKey().getSequence(), entry.getValue(), fixedProtein, (ProteinUpdateProcessor)caseEvent.getSource(), false);

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
                        proteinDeleter.delete(new ProteinEvent(caseEvent.getSource(), caseEvent.getDataContext(), entry.getKey(), "Protein duplicate"));
                    }
                }
                else {
                    fireNonUniprotProteinFound(new ProteinEvent(this, caseEvent.getDataContext(), entry.getKey()));
                }
            }
        }
        return report;
    }

}