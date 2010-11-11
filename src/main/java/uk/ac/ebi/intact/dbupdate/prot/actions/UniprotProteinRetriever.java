package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.remoting.RemoteConnectFailureException;
import uk.ac.ebi.intact.dbupdate.prot.*;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
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
 * TODO comment this
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

    private static final Log log = LogFactory.getLog( UniprotProteinRetriever.class );

    private final int MAX_RETRY_ATTEMPTS = 100;
    private int retryAttempt = 0;

    public UniprotProteinRetriever(UniprotService uniprotService) {
        this.uniprotService = uniprotService;
    }

    public UniprotProtein retrieveUniprotEntry(ProteinEvent evt) throws ProcessorException {
        String uniprotAc = evt.getUniprotIdentity();

        if (uniprotAc != null){
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
                // the intact protein matching this ac is not null
                if(evt.getProtein() != null){

                    final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

                    // if we can update the dead proteins, we update them, otherwise we add an error in uniprotServiceResult
                    if (config != null && !config.isProcessProteinNotFoundInUniprot()){
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "No uniprot entry is matching the ac " + uniprotAc, UpdateError.dead_uniprot_ac));
                        }

                        if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (no-uniprot-update)");
                        ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireOnUniprotDeadEntry(new ProteinEvent(updateProcessor, evt.getDataContext(), evt.getProtein()));
                        }
                    }

                }
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
                        updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + uniprotAc, UpdateError.several_uniprot_entries_same_organim));
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
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + uniprotAc, UpdateError.several_uniprot_entries_different_organisms));
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

    public void filterAllSecondaryProteinsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException {
        if (evt.getSecondaryProteins().size() > 0){
            filterSecondaryProteinsPossibleToUpdate(evt);
        }

        if (evt.getSecondaryIsoforms().size() > 0){
            filterSecondaryIsoformsPossibleToUpdate(evt);
        }
    }

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
                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                        updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "No uniprot entry is matching the ac " + primaryAc, UpdateError.dead_uniprot_ac));
                    }
                    secondaryAcToRemove.add(prot);
                }
                else if ( uniprotProteins.size() > 1 ) {
                    if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_same_organim));
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
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_different_organisms));
                            }
                            secondaryAcToRemove.add(prot);
                        }
                        else if (proteinsWithSameTaxId.size() == 1){
                            secondaryAcToRemove.add(prot);
                        }
                    }
                }
            }
        }
        evt.getSecondaryProteins().removeAll(secondaryAcToRemove);
    }

    private void filterSecondaryIsoformsPossibleToUpdate(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<ProteinTranscript> secondaryProteins = evt.getSecondaryIsoforms();
        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        Collection<Protein> secondaryAcToRemove = new ArrayList<Protein>();

        for (ProteinTranscript protTrans : secondaryProteins){
            Protein prot = protTrans.getProtein();
            
            InteractorXref primary = ProteinUtils.getUniprotXref(prot);

            String primaryAc = primary.getPrimaryId();

            // the protein is not the protein being updated at the moment so we need to query uniprot with this primary ac
            if (!serviceResult.getQuerySentToService().equals(primaryAc)){
                Collection<UniprotProtein> uniprotProteins = uniprotService.retrieve( primaryAc );

                // no uniprot protein matches this uniprot ac
                if(uniprotProteins.size() == 0){
                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                        updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + "No uniprot entry is matching the ac " + primaryAc, UpdateError.dead_uniprot_ac));
                    }
                    secondaryAcToRemove.add(prot);
                }
                else if ( uniprotProteins.size() > 1 ) {
                    if ( 1 == getSpeciesCount( uniprotProteins ) ) {
                        // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
                        String truncatedAc = primaryAc.substring(0, Math.max(0, primaryAc.indexOf("-")));
                        List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<UniprotProtein>();

                        for (UniprotProtein uniprot : uniprotProteins){
                            if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                proteinsWithSameBaseUniprotAc.add(uniprot);
                            }
                        }

                        if (proteinsWithSameBaseUniprotAc.size() != 1){
                            // If a uniprot ac we have in Intact as identity xref in IntAct, now corresponds to 2 or more proteins
                            // in uniprot we should not update it automatically but send a message to the curators so that they
                            // choose manually which of the new uniprot ac is relevant.
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_same_organim));
                            }
                            secondaryAcToRemove.add(prot);
                        }

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

                            // several splice variants can be attached to several master proteins and it is not an error. If we are working with such protein transcripts, we need to update them
                            String truncatedAc = primaryAc.substring(0, Math.max(0, primaryAc.indexOf("-")));
                            List<UniprotProtein> proteinsWithSameBaseUniprotAc = new ArrayList<UniprotProtein>();

                            for (UniprotProtein uniprot : proteinsWithSameTaxId){
                                if (uniprot.getPrimaryAc().equalsIgnoreCase(truncatedAc)){
                                    proteinsWithSameBaseUniprotAc.add(uniprot);
                                }
                            }

                            if (proteinsWithSameBaseUniprotAc.size() != 1){
                                secondaryAcToRemove.add(prot);
                            }
                            else if (proteinsWithSameBaseUniprotAc.size() == 1){
                                if (!uniprotProtein.equals(proteinsWithSameBaseUniprotAc.iterator().next())){
                                    if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                        final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                        updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), uniprotProteins.size() + " uniprot entries are matching the ac " + primaryAc, UpdateError.several_uniprot_entries_different_organisms));
                                    }
                                    secondaryAcToRemove.add(prot);
                                }
                            }
                        }
                    }
                }
            }
        }
        evt.getSecondaryProteins().removeAll(secondaryAcToRemove);
    }
}
