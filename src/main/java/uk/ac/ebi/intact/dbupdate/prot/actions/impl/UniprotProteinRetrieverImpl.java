package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.RemoteConnectFailureException;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.actions.DeadUniprotProteinFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinRetriever;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.Organism;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * This class retrieves uniprot entries using uniprot acs (secondary or primary)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Nov-2010</pre>
 */

public class UniprotProteinRetrieverImpl implements UniprotProteinRetriever{
    /**
     * UniProt Data Source.
     */
    private UniprotService uniprotService;

    /**
     * The fixer of proteins dead in uniprot
     */
    private DeadUniprotProteinFixer deadUniprotFixer;

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( UniprotProteinRetrieverImpl.class );

    /**
     * Max Number of attempts if the uniprot service fails to retrieve a protein
     */
    private final int MAX_RETRY_ATTEMPTS = 100;


    private int retryAttempt = 0;

    public UniprotProteinRetrieverImpl(UniprotService uniprotService) {
        this.uniprotService = uniprotService;
        this.deadUniprotFixer = new DeadUniprotProteinFixerImpl();
    }

    /**
     *
     * @param uniprotAc
     * @return the unique uniprot entry matching this uniprot ac if possible, null otherwise
     */
    public UniprotProtein retrieveUniprotEntry(String uniprotAc){
        Collection<UniprotProtein> uniprotProteins;
        try{
            uniprotProteins = uniprotService.retrieve( uniprotAc );
        } catch (RemoteConnectFailureException ce) {
            if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                throw new ProcessorException("Maximum number of retry attempts reached ("+MAX_RETRY_ATTEMPTS+") for: "+uniprotAc);
            }
            retryAttempt++;

            if (log.isErrorEnabled()) log.error("Couldn't connect to Uniprot. Will wait 60 seconds before retrying. (Retry: "+retryAttempt+")");
            try {
                Thread.sleep(60*1000);
                uniprotProteins = uniprotService.retrieve( uniprotAc );
            } catch (InterruptedException e) {
                throw new ProcessorException("Problem while waiting before retrying for "+uniprotAc, e);
            }

        }

        // no uniprot protein matches this uniprot ac
        if(uniprotProteins.size() == 0){
            return null;
        }
        else if ( uniprotProteins.size() > 1 ) {

            // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
            if (IdentifierChecker.isSpliceVariantId(uniprotAc)){
                String truncatedAc = uniprotAc.substring(0, Math.max(0, uniprotAc.indexOf("-")));
                List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<UniprotProtein>();

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
     *
     * @param evt
     * @return the unique uniprot entry matching this uniprot identity in the ProteinEvent if possible, null otherwise
     * @throws ProcessorException
     */
    public UniprotProtein retrieveUniprotEntry(ProteinEvent evt) throws ProcessorException {
        // the uniprot ac is the uniprot identity in the protein event
        String uniprotAc = evt.getUniprotIdentity();

        // if not null, query uniprot
        if (uniprotAc != null){
            // try to collect uniprot entries
            Collection<UniprotProtein> uniprotProteins;
            try{
                uniprotProteins = uniprotService.retrieve( uniprotAc );
            } catch (RemoteConnectFailureException ce) {
                if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                    throw new ProcessorException("Maximum number of retry attempts reached ("+MAX_RETRY_ATTEMPTS+") for: "+uniprotAc);
                }
                retryAttempt++;

                if (log.isErrorEnabled()) log.error("Couldn't connect to Uniprot. Will wait 60 seconds before retrying. (Retry: "+retryAttempt+")");
                try {
                    Thread.sleep(60*1000);
                    uniprotProteins = uniprotService.retrieve( uniprotAc );
                } catch (InterruptedException e) {
                    throw new ProcessorException("Problem while waiting before retrying for "+uniprotAc, e);
                }

            }

            // no uniprot protein matches this uniprot ac
            if(uniprotProteins.size() == 0){
                processProteinNotFoundInUniprot(evt);
            }
            else if ( uniprotProteins.size() > 1 ) {
                if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                    // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
                    if (IdentifierChecker.isSpliceVariantId(uniprotAc)){
                        String truncatedAc = uniprotAc.substring(0, Math.max(0, uniprotAc.indexOf("-")));
                        List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<UniprotProtein>();

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
                        updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + uniprotAc, UpdateError.several_uniprot_entries_same_organim, evt.getProtein(), evt.getUniprotIdentity()));
                    }

                    if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (several matching uniprot entries with the same organism)");
                    ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
                } else {

                    Protein prot = evt.getProtein();
                    String taxId = null;
                    if (prot.getBioSource() != null){
                        taxId = prot.getBioSource().getTaxId();
                    }

                    List<UniprotProtein> proteinsWithSameTaxId = new ArrayList<UniprotProtein>();

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
                            List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<UniprotProtein>();

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
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + uniprotAc, UpdateError.several_uniprot_entries_different_organisms, evt.getProtein(), evt.getUniprotIdentity()));
                        }

                        if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (several matching uniprot entries with different organisms)");
                        ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
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

        Collection<Integer> species = new ArrayList<Integer>( proteins.size() );
        for ( UniprotProtein protein : proteins ) {
            int taxid = protein.getOrganism().getTaxid();
            if(!species.contains(taxid)){
                species.add( taxid );
            }
        }

        return species.size();
    }

    /**
     * Filter all possible protein in the event which cannot match a single uniprot entry
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
        Collection<? extends Protein> secondaryProteins = evt.getSecondaryProteins();
        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        Collection<Protein> secondaryAcToRemove = new ArrayList<Protein>();

        for (Protein prot : secondaryProteins){
            InteractorXref primary = ProteinUtils.getUniprotXref(prot);

            String primaryAc = primary.getPrimaryId();

            // the protein is not the protein being updated at the moment so we need to query uniprot with this primary ac
            if (!serviceResult.getQuerySentToService().equals(primaryAc)){
                Collection<UniprotProtein> uniprotProteins = uniprotService.retrieve( primaryAc );

                // no uniprot protein matches this uniprot ac
                if(uniprotProteins.size() == 0){
                    processProteinNotFoundInUniprot(new ProteinEvent(evt.getSource(), evt.getDataContext(), prot));
                    secondaryAcToRemove.add(prot);
                }
                else if ( uniprotProteins.size() > 1 ) {
                    if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_same_organim, prot, primaryAc));
                        }
                        secondaryAcToRemove.add(prot);
                    } else {

                        String taxId = null;
                        if (prot.getBioSource() != null){
                            taxId = prot.getBioSource().getTaxId();
                        }

                        List<UniprotProtein> proteinsWithSameTaxId = new ArrayList<UniprotProtein>();

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
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_different_organisms, prot, primaryAc));
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

        Collection<ProteinTranscript> secondaryAcToRemove = new ArrayList<ProteinTranscript>();

        for (ProteinTranscript protTrans : transcripts){
            Protein prot = protTrans.getProtein();

            if (protTrans.getUniprotVariant() == null){
                if (ProteinUtils.isFromUniprot(prot)){
                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                        updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "No uniprot entry is matching the protein transcript  " + prot.getAc(), UpdateError.dead_uniprot_ac, prot, evt.getUniprotServiceResult().getQuerySentToService()));
                    }
                    processProteinNotFoundInUniprot(new ProteinEvent(evt.getSource(), evt.getDataContext(), prot));
                    secondaryAcToRemove.add(protTrans);
                }
            }
        }
        transcripts.removeAll(secondaryAcToRemove);
    }

    /**
     * process proteins not found in uniprot
     * @param evt
     */
    public void processProteinNotFoundInUniprot(ProteinEvent evt){
        // the intact protein matching this ac is not null
        if(evt.getProtein() != null){
            List<ProteinImpl> transcripts = evt.getDataContext().getDaoFactory().getProteinDao().getSpliceVariants(evt.getProtein());
            transcripts.addAll(evt.getDataContext().getDaoFactory().getProteinDao().getProteinChains(evt.getProtein()));

            final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

            // if we can update the dead proteins, we update them, otherwise we add an error in uniprotServiceResult
            if (config != null && !config.isProcessProteinNotFoundInUniprot()){
                if (evt.getSource() instanceof ProteinUpdateProcessor) {
                    final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                    updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "No uniprot entry is matching the ac " + evt.getUniprotIdentity(), UpdateError.dead_uniprot_ac, evt.getProtein(), evt.getUniprotIdentity()));
                }

            }
            else {
                deadUniprotFixer.fixDeadProtein(evt);
            }

            if (!transcripts.isEmpty()){
                for (Protein pt : transcripts){
                    InteractorXref uniprot = ProteinUtils.getUniprotXref(pt);

                    if (uniprot != null){
                        Collection<UniprotProtein> uniprotProteins;
                        try{
                            uniprotProteins = uniprotService.retrieve( uniprot.getPrimaryId() );

                        } catch (RemoteConnectFailureException ce) {
                            if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                                throw new ProcessorException("Maximum number of retry attempts reached ("+MAX_RETRY_ATTEMPTS+") for: "+uniprot.getPrimaryId() );
                            }
                            retryAttempt++;

                            if (log.isErrorEnabled()) log.error("Couldn't connect to Uniprot. Will wait 60 seconds before retrying. (Retry: "+retryAttempt+")");
                            try {
                                Thread.sleep(60*1000);
                                uniprotProteins = uniprotService.retrieve( uniprot.getPrimaryId() );
                            } catch (InterruptedException e) {
                                throw new ProcessorException("Problem while waiting before retrying for "+uniprot.getPrimaryId(), e);
                            }

                        }

                        if (uniprotProteins.size() >= 1){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor,
                                        evt.getDataContext(), "The protein transcript " + pt.getAc() + " is attached to the dead uniprot parent "
                                                + evt.getProtein().getAc() + " but we could find " + uniprotProteins.size() + " existing proteins in Uniprot matching the uniprot ac "
                                                + uniprot.getPrimaryId(), UpdateError.dead_protein_with_transcripts_not_dead, evt.getProtein(), evt.getUniprotIdentity()));
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

    public DeadUniprotProteinFixer getDeadUniprotFixer() {
        return deadUniprotFixer;
    }

    public void setDeadUniprotFixer(DeadUniprotProteinFixer deadUniprotFixer) {
        this.deadUniprotFixer = deadUniprotFixer;
    }
}
