package uk.ac.ebi.intact.dbupdate.prot.actions.retrievers;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.RemoteAccessException;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.DeadUniprotProteinFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.mappers.UniprotProteinMapper;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateError;
import uk.ac.ebi.intact.dbupdate.prot.errors.ProteinUpdateErrorFactory;
import uk.ac.ebi.intact.dbupdate.prot.errors.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.model.ProteinTranscript;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.service.CachedUniprotService;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.uniprot.service.SimpleUniprotRemoteService;
import uk.ac.ebi.intact.uniprot.service.UniprotService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * This class retrieves uniprot entries using uniprot acs (secondary or primary)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class UniprotProteinRetriever {
    /**
     * UniProt Data Source.
     */
    private UniprotService uniprotService;

    /**
     * The fixer of proteins dead in uniprot
     */
    private DeadUniprotProteinFixer deadUniprotFixer;

    /**
     * The mapper of proteins
     */
    private UniprotProteinMapper proteinMappingManager;

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( UniprotProteinRetriever.class );


    private int retryAttempt = 0;

    public UniprotProteinRetriever(UniprotService uniprotService, UniprotProteinMapper proteinMapper, DeadUniprotProteinFixer deadProteinfixer) {
        if (uniprotService != null){
            this.uniprotService = uniprotService;
        }
        else {
            this.uniprotService = new CachedUniprotService(new SimpleUniprotRemoteService());
        }

        if (deadProteinfixer != null){
            this.deadUniprotFixer = deadProteinfixer;
        }
        else {
            this.deadUniprotFixer = new DeadUniprotProteinFixer();
        }

        if (proteinMapper != null){
            this.proteinMappingManager = proteinMapper;
        }
        else {
            this.proteinMappingManager = new UniprotProteinMapper(uniprotService);
        }
    }

    /**
     * @param uniprotAc
     * @return the unique uniprot entry matching this ac, null otherwise
     */
    public UniprotProtein retrieveUniprotEntry(String uniprotAc){
        Collection<UniprotProtein> uniprotProteins = retrieveUniprotEntries(uniprotAc);

        // no uniprot protein matches this uniprot ac
        if(uniprotProteins.size() == 0){
            return null;
        }
        else if ( uniprotProteins.size() > 1 ) {

            // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
            if (IdentifierChecker.isSpliceVariantId(uniprotAc)){
                String truncatedAc = uniprotAc.substring(0, Math.max(0, uniprotAc.indexOf("-")));
                List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<>();

                for (UniprotProtein uniprot : uniprotProteins){
                    if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                        proteinsWithSameBaseUniprotAc.add(uniprot);
                    }
                }

                if (proteinsWithSameBaseUniprotAc.size() == 1){
                    return proteinsWithSameBaseUniprotAc.iterator().next();
                }
            }
            // If a uniprot ac we have in Intact as identity xref in IntAct, now corresponds to 2 or more proteins
            // in uniprot we should not update it automatically but send a message to the curators so that they
            // choose manually which of the new uniprot ac is relevant.
            return null;
        } else {
            return uniprotProteins.iterator().next();
        }
    }

    /**
     * @param evt
     * @return the unique uniprot entry matching the uniprot identity of the event, null otherwise
     * @throws ProcessorException
     */
    public UniprotProtein retrieveUniprotEntry(ProteinEvent evt) throws ProcessorException {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        // the uniprot ac is the uniprot identity in the protein event
        String uniprotAc = evt.getUniprotIdentity();

        // if not null, query uniprot
        if (uniprotAc != null){
            // try to collect uniprot entries
            Collection<UniprotProtein> uniprotProteins = retrieveUniprotEntries(uniprotAc);

            // no uniprot protein matches this uniprot ac
            if(uniprotProteins.size() == 0){
                evt.setMessage(uniprotAc + " doesn't match any uniprot entries.");
                processProteinNotFoundInUniprot(evt);
                if (!proteinMappingManager.processProteinRemappingFor(evt)){
                    log.info("The dead entry " + evt.getProtein().getAc() + " cannot be remapped to any uni-prot entries");
                }
                else {
                    InteractorXref identityAfterRemapping = ProteinUtils.getUniprotXref(evt.getProtein());

                    return retrieveUniprotEntry(identityAfterRemapping.getPrimaryId().trim());
                }
            }
            else if ( uniprotProteins.size() > 1 ) {
                if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                    // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
                    if (IdentifierChecker.isSpliceVariantId(uniprotAc)){
                        String truncatedAc = uniprotAc.substring(0, Math.max(0, uniprotAc.indexOf("-")));
                        List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<>();

                        for (UniprotProtein uniprot : uniprotProteins){
                            if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                proteinsWithSameBaseUniprotAc.add(uniprot);
                            }
                        }

                        if (proteinsWithSameBaseUniprotAc.size() == 1){
                            return proteinsWithSameBaseUniprotAc.iterator().next();
                        }
                    }
                    // If a uniprot ac we have in Intact as identity xref in IntAct, now corresponds to 2 or more proteins
                    // in uniprot we should not update it automatically but send a message to the curators so that they
                    // choose manually which of the new uniprot ac is relevant.
                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                        String taxId = evt.getProtein().getBioSource() != null ? evt.getProtein().getBioSource().getTaxId() : null;
                        ProteinUpdateError matchSeveralUniprot = errorFactory.createMatchSeveralUniprotEntriesError(evt.getProtein().getAc(), uniprotAc, taxId, UpdateError.several_uniprot_entries_same_organim, uniprotProteins, Collections.EMPTY_LIST);
                        updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), matchSeveralUniprot, evt.getProtein(), uniprotAc));
                    }

                } else {

                    Protein prot = evt.getProtein();
                    String taxId = null;
                    if (prot.getBioSource() != null){
                        taxId = prot.getBioSource().getTaxId();
                    }

                    List<UniprotProtein> proteinsWithSameTaxId = new ArrayList<>();

                    if (taxId != null){
                        for (UniprotProtein uniprot : uniprotProteins){
                            if (uniprot.getOrganism() != null){
                                Organism o = uniprot.getOrganism();
                                if (o.getTaxid() == Integer.parseInt(taxId)){
                                    proteinsWithSameTaxId.add(uniprot);
                                }
                            }
                        }
                    }

                    if (proteinsWithSameTaxId.size() == 1){
                        return proteinsWithSameTaxId.iterator().next();
                    }
                    else {
                        // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
                        if (IdentifierChecker.isSpliceVariantId(uniprotAc)){
                            String truncatedAc = uniprotAc.substring(0, Math.max(0, uniprotAc.indexOf("-")));
                            List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<>();

                            for (UniprotProtein uniprot : proteinsWithSameTaxId){
                                if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                    proteinsWithSameBaseUniprotAc.add(uniprot);
                                }
                            }

                            if (proteinsWithSameBaseUniprotAc.size() == 1){
                                return proteinsWithSameBaseUniprotAc.iterator().next();
                            }
                        }

                        // Send an error message because this should just not happen anymore in IntAct at all. In IntAct, all
                        // the demerged has taken care of the demerged proteins have been dealt with and replaced manually by
                        // the correct uniprot protein.
                        // Ex of demerged protein :P00001 was standing for the Cytochrome c of the human and the chimpanzee.
                        // It has now been demerged in one entry for the human P99998 and one for the chimpanzee P99999.
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                            ProteinUpdateError matchSeveralUniprot = errorFactory.createMatchSeveralUniprotEntriesError(prot.getAc(), uniprotAc, taxId, UpdateError.several_uniprot_entries_different_organisms, proteinsWithSameTaxId, CollectionUtils.subtract(uniprotProteins, proteinsWithSameTaxId));
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), matchSeveralUniprot, prot, uniprotAc));
                        }

                        if (log.isTraceEnabled()) {
                            log.debug("Request finalization, as this protein cannot be updated using UniProt (several matching uniprot entries with different organisms)");
                        }
                    }
                }
            } else {
                return uniprotProteins.iterator().next();
            }
        }
        return null;
    }

    /**
     * Count the number of differents species the proteins are spread on and return the count.
     * @param proteins a Collection of Uniprot Proteins
     * @return an int representing the number of different species found.
     */
    private int getSpeciesCount( Collection<UniprotProtein> proteins ) {

        if(proteins == null){
            throw new IllegalArgumentException("The proteins collection should not be null");
        }
        if(proteins.size() == 0){
            throw new IllegalArgumentException("The proteins collection should not be empty");
        }

        Collection<Integer> species = new ArrayList<>(proteins.size());
        for ( UniprotProtein protein : proteins ) {
            int taxid = protein.getOrganism().getTaxid();
            if(!species.contains(taxid)){
                species.add( taxid );
            }
        }

        return species.size();
    }

    /**
     * Filter proteins which could not be updated because not matching a single uniprot entry
     *
     * @param evt
     * @throws ProcessorException
     */
    public void filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException {
        // a secondary protein can be remapped to several uniprot entries so we only have to filter secondary proteins having several uniprot entries

        // filter primary isoforms
        if (evt.getPrimaryIsoforms().size() > 0){
            filterProteinTranscriptsPossibleToUpdate(evt.getPrimaryIsoforms(), evt);
        }

        // filter secondary isoforms
        if (evt.getSecondaryIsoforms().size() > 0){
            filterProteinTranscriptsPossibleToUpdate(evt.getSecondaryIsoforms(), evt);
        }

        // filter primary chains
        if (evt.getPrimaryFeatureChains().size() > 0){
            filterProteinTranscriptsPossibleToUpdate(evt.getPrimaryFeatureChains(), evt);
        }

        // filter secondary proteins only!
        if (evt.getSecondaryProteins().size() > 0){
            filterSecondaryProteinsPossibleToUpdate(evt);
        }
    }

    /**
     * Filter secondary proteins having several uniprot entries and remove them from the list of proteins to update
     * @param evt
     * @throws ProcessorException
     */
    private void filterSecondaryProteinsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

        Collection<? extends Protein> secondaryProteins = evt.getSecondaryProteins();
        UniprotProtein uniprotProtein = evt.getProtein();

        Collection<Protein> secondaryAcToRemove = new ArrayList<>();

        for (Protein prot : secondaryProteins){
            InteractorXref primary = ProteinUtils.getUniprotXref(prot);

            String primaryAc = primary.getPrimaryId();

            // the protein is not the protein being updated at the moment so we need to query uniprot with this primary ac
            if (!evt.getQuerySentToService().equals(primaryAc)){
                Collection<UniprotProtein> uniprotProteins = retrieveUniprotEntries( primaryAc );

                // no uniprot protein matches this uniprot ac
                if(uniprotProteins.size() == 0){
                    secondaryAcToRemove.add(prot);

                    // remove the protein from the proteins which can be updated. Will be updated later
                    evt.getProteins().remove(prot.getAc());
                }
                else if ( uniprotProteins.size() > 1 ) {
                    if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                            String taxId = prot.getBioSource() != null ? prot.getBioSource().getTaxId() : null;
                            ProteinUpdateError matchSeveralUniprot = errorFactory.createMatchSeveralUniprotEntriesError(prot.getAc(),
                                    primaryAc, taxId, UpdateError.several_uniprot_entries_same_organim,
                                    uniprotProteins, Collections.EMPTY_LIST);
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), matchSeveralUniprot, prot, primaryAc));
                        }
                        secondaryAcToRemove.add(prot);
                    } else {

                        String taxId = null;
                        if (prot.getBioSource() != null){
                            taxId = prot.getBioSource().getTaxId();
                        }

                        List<UniprotProtein> proteinsWithSameTaxId = new ArrayList<>();

                        if (taxId != null){
                            for (UniprotProtein uniprot : uniprotProteins){
                                if (uniprot.getOrganism() != null){
                                    Organism o = uniprot.getOrganism();
                                    if (o.getTaxid() == Integer.parseInt(taxId)){
                                        proteinsWithSameTaxId.add(uniprot);
                                    }
                                }
                            }
                        }

                        if (proteinsWithSameTaxId.size() != 1){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                                ProteinUpdateError matchSeveralUniprot = errorFactory.createMatchSeveralUniprotEntriesError(prot.getAc(), primaryAc, taxId, UpdateError.several_uniprot_entries_different_organisms, proteinsWithSameTaxId, CollectionUtils.subtract(uniprotProteins, proteinsWithSameTaxId));
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), matchSeveralUniprot, prot, primaryAc));
                            }
                            secondaryAcToRemove.add(prot);
                        }
                        else if (!uniprotProtein.equals(proteinsWithSameTaxId.iterator().next())){
                            secondaryAcToRemove.add(prot);
                        }
                    }
                }
                else if (!uniprotProtein.equals(uniprotProteins.iterator().next())){
                    secondaryAcToRemove.add(prot);
                }
            }
        }
        evt.getSecondaryProteins().removeAll(secondaryAcToRemove);
    }

    /**
     * Filter secondary isoforms having several uniprot entries and remove them from the list of proteins to update
     * @param evt
     * @throws ProcessorException
     */
    private void filterProteinTranscriptsPossibleToUpdate(Collection<ProteinTranscript> transcripts, UpdateCaseEvent evt)  throws ProcessorException {

        Collection<ProteinTranscript> secondaryAcToRemove = new ArrayList<>();

        for (ProteinTranscript protTrans : transcripts){

            Protein prot = protTrans.getProtein();

            InteractorXref primary = ProteinUtils.getUniprotXref(prot);
            String primaryAc = primary.getPrimaryId();

            if (protTrans.getUniprotProteinTranscript() == null){
                secondaryAcToRemove.add(protTrans);
                // remove the protein from the proteins whcih can be updated. Will be updated later
                evt.getProteins().remove(prot.getAc());
            }
            // the protein is not the protein being updated at the moment and is an isoform so we need to query uniprot with this primary ac
            // the feature chains are always unique per entry
            else if (!evt.getQuerySentToService().equals(primaryAc) && IdentifierChecker.isSpliceVariantId(primaryAc)){
                Collection<UniprotProtein> uniprotProteins = retrieveUniprotEntries(primaryAc);

                // no uniprot protein matches this uniprot ac
                if ( uniprotProteins.size() > 1 ) {
                    if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                        String truncatedAc = primaryAc.substring(0, Math.max(0, primaryAc.indexOf("-")));
                        List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<>();

                        for (UniprotProtein uniprot : uniprotProteins){
                            if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                proteinsWithSameBaseUniprotAc.add(uniprot);
                            }
                        }

                        if (proteinsWithSameBaseUniprotAc.size() != 1){
                            secondaryAcToRemove.add(protTrans);
                            // remove the protein from the proteins whcih can be updated. Will be updated later
                            evt.getProteins().remove(prot.getAc());
                        }

                    } else {

                        String taxId = null;
                        if (prot.getBioSource() != null){
                            taxId = prot.getBioSource().getTaxId();
                        }

                        List<UniprotProtein> proteinsWithSameTaxId = new ArrayList<>();

                        if (taxId != null){
                            for (UniprotProtein uniprot : uniprotProteins){
                                if (uniprot.getOrganism() != null){
                                    Organism o = uniprot.getOrganism();
                                    if (o.getTaxid() == Integer.parseInt(taxId)){
                                        proteinsWithSameTaxId.add(uniprot);
                                    }
                                }
                            }
                        }

                        if (proteinsWithSameTaxId.size() > 1){
                            String truncatedAc = primaryAc.substring(0, Math.max(0, primaryAc.indexOf("-")));
                            List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<>();

                            for (UniprotProtein uniprot : proteinsWithSameTaxId){
                                if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                    proteinsWithSameBaseUniprotAc.add(uniprot);
                                }
                            }

                            if (proteinsWithSameBaseUniprotAc.size() != 1){
                                secondaryAcToRemove.add(protTrans);
                                // remove the protein from the proteins whcih can be updated. Will be updated later
                                evt.getProteins().remove(prot.getAc());
                            }
                        }
                        else if (proteinsWithSameTaxId.isEmpty()) {
                            secondaryAcToRemove.add(protTrans);
                            // remove the protein from the proteins whcih can be updated. Will be updated later
                            evt.getProteins().remove(prot.getAc());
                        }
                    }
                }
            }
        }
        transcripts.removeAll(secondaryAcToRemove);
    }

    private Collection<UniprotProtein> retrieveUniprotEntries(String primaryAc) {
        // try to collect uniprot entries
        Collection<UniprotProtein> uniprotProteins;
        uniprotService.start();
        try{
            //Calls intact-uniprot:UniprotServices in intact-bridges
            uniprotProteins = uniprotService.retrieve( primaryAc );
        } catch (RemoteAccessException ce) {
            /**
             * Max Number of attempts if the uniprot service fails to retrieve a protein
             */
            int MAX_RETRY_ATTEMPTS = 100;
            if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                throw new ProcessorException("Maximum number of retry attempts reached ("+ MAX_RETRY_ATTEMPTS +") for: "+primaryAc);
            }
            retryAttempt++;

            if (log.isErrorEnabled()) log.error("Couldn't connect to Uniprot. Will wait 60 seconds before retrying. (Retry: "+retryAttempt+")");
            try {
                Thread.sleep(60*1000);
                uniprotProteins = uniprotService.retrieve( primaryAc );
            } catch (InterruptedException e) {
                uniprotService.close();
                throw new ProcessorException("Problem while waiting before retrying for "+primaryAc, e);
            }

        }
        //TODO: Noe uniprotService.close() close the cache for the cache implementation, how much impact has this in the life of the cache?
        uniprotService.close();
        return uniprotProteins;
    }

    /**
     * Process the protein not found in uniprot
     *
     * @param evt
     */
    public void processProteinNotFoundInUniprot(ProteinEvent evt){
        // the intact protein matching this ac is not null
        if(evt.getProtein() != null){
            List<ProteinImpl> transcripts = evt.getDataContext().getDaoFactory().getProteinDao().getSpliceVariants(evt.getProtein());
            transcripts.addAll(evt.getDataContext().getDaoFactory().getProteinDao().getProteinChains(evt.getProtein()));

            final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
            ProteinUpdateErrorFactory errorFactory = config.getErrorFactory();

            // if we can update the dead proteins, we update them, otherwise we add an error in uniprotServiceResult
            if (!config.isProcessProteinNotFoundInUniprot()){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                    ProteinUpdateError notFoundError = errorFactory.createDeadUniprotAcError(evt.getProtein().getAc(), evt.getUniprotIdentity());
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), notFoundError, evt.getProtein(), evt.getUniprotIdentity()));
                }
            }
            else {
                deadUniprotFixer.fixDeadProtein(evt);
            }

            if (!transcripts.isEmpty()){
                for (Protein pt : transcripts){
                    InteractorXref uniprot = ProteinUtils.getUniprotXref(pt);

                    if (uniprot != null){
                        Collection<UniprotProtein> uniprotProteins = retrieveUniprotEntries(uniprot.getPrimaryId());

                        if (uniprotProteins.size() >= 1){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

                                ProteinUpdateError existingTranscriptError = errorFactory.createNonExistingMasterProteinError(evt.getProtein().getAc(), evt.getUniprotIdentity(), uniprot.getPrimaryId(), pt.getAc());
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), existingTranscriptError, evt.getProtein(), evt.getUniprotIdentity()));
                            }

                        }
                    }
                }
            }
        }
    }

    public UniprotService getUniprotService() {
        return uniprotService;
    }

    public void setUniprotService(UniprotService uniprotService) {
        this.uniprotService = uniprotService;
    }


    public static UniprotProteinTranscript findUniprotSpliceVariant(String uniprotAc, UniprotProtein uniprot){

        if (uniprot == null || uniprotAc == null){
            return null;
        }

        for (UniprotProteinTranscript t : uniprot.getSpliceVariants()){
            if (t.getPrimaryAc().equalsIgnoreCase(uniprotAc) || t.getSecondaryAcs().contains(uniprotAc)){
                return t;
            }
        }

        return null;
    }

    public static UniprotProteinTranscript findUniprotFeatureChain(String uniprotAc, UniprotProtein uniprot){

        if (uniprot == null || uniprotAc == null){
            return null;
        }

        for (UniprotProteinTranscript t : uniprot.getFeatureChains()){
            if (t.getPrimaryAc().equalsIgnoreCase(uniprotAc)){
                return t;
            }
        }

        return null;
    }
}