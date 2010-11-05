package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.IdentifierChecker;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04-Nov-2010</pre>
 */

public class UniprotPrimaryAcUpdater extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( UniprotProteinRetrieverListener.class );

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {

        UniprotProtein uniprotProtein = evt.getUniprotProtein();
        String uniprotId = evt.getUniprotIdentity();

        // the protein has a single uniprot id that we can rely on
        if (uniprotId != null){
            // No single uniprot entry has been found. It can be a dead protein. We don't touch it
            if (uniprotProtein != null){
                // if a splice variant, check the splice variants
                if (IdentifierChecker.isSpliceVariantId(uniprotId)){

                    UniprotSpliceVariant spliceVariant = null;

                    Collection<UniprotSpliceVariant> variants = uniprotProtein.getSpliceVariants();

                    for (UniprotSpliceVariant variant : variants){
                        Collection<String> variantAcs = variant.getSecondaryAcs();

                        if (variant.getPrimaryAc().equalsIgnoreCase(uniprotId) || variantAcs.contains(uniprotId)){
                            spliceVariant = variant;
                            break;
                        }
                    }

                    if (spliceVariant != null){
                        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

                        final String uniprotAc = spliceVariant.getPrimaryAc();

                        if (log.isDebugEnabled()) log.debug("Searching IntAct for Uniprot protein: "+ uniprotAc + ", "
                                + spliceVariant.getOrganism().getName() +" ("+spliceVariant.getOrganism().getTaxid()+")");

                        // we will assign the proteins to two collections - primary / secondary
                        Collection<ProteinImpl> primaryProteins = proteinDao.getByUniprotId(uniprotAc);
                        Collection<ProteinImpl> secondaryProteins = new ArrayList<ProteinImpl>();

                        for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
                            secondaryProteins.addAll(proteinDao.getByUniprotId(secondaryAc));
                        }

                        // filter and remove non-uniprot prots from the list, and assign to the primary or secondary collections
                        ProteinTools.filterNonUniprotAndMultipleUniprot(primaryProteins);
                        ProteinTools.filterNonUniprotAndMultipleUniprot(secondaryProteins);

                        int countPrimary = primaryProteins.size();
                        int countSecondary = secondaryProteins.size();

                        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotAc);

                        if (countSecondary > 0){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                UpdateCaseEvent updateEvent = new UpdateCaseEvent(updateProcessor, evt.getDataContext(), uniprotProtein, Collections.EMPTY_LIST, secondaryProteins);
                                updateEvent.setUniprotServiceResult(new UniprotServiceResult(uniprotId));

                                updateProcessor.fireOnSecondaryAcsFound(updateEvent);
                            }
                            else {
                                throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
                            }
                        }
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(),  "The uniprot id " + uniprotId + " is not matching any of the primary and secondary acs of the uniprot splice variant " + spliceVariant.getPrimaryAc(), UpdateError.not_matching_uniprot_id));
                        }

                        if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (several matching uniprot entries with different organisms)");
                        ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
                    }
                }
                else if (IdentifierChecker.isFeatureChainId(uniprotId)){
                    Collection<UniprotFeatureChain> chains = uniprotProtein.getFeatureChains();
                    UniprotFeatureChain featureChain = null;

                    for (UniprotFeatureChain chain : chains){

                        if (chain.getPrimaryAc().equalsIgnoreCase(uniprotId)){
                            featureChain = chain;
                            break;
                        }
                    }

                    if (featureChain == null){
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The uniprot id " + uniprotId + " is not matching the primary ac of the uniprot feature chain " + featureChain.getPrimaryAc(), UpdateError.not_matching_uniprot_id));
                        }

                        if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt (several matching uniprot entries with different organisms)");
                        ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
                    }
                }
                else {
                    ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

                    final String uniprotAc = uniprotProtein.getPrimaryAc();

                    if (log.isDebugEnabled()) log.debug("Searching IntAct for Uniprot protein: "+ uniprotAc + ", "
                            + uniprotProtein.getOrganism().getName() +" ("+uniprotProtein.getOrganism().getTaxid()+")");

                    // we will assign the proteins to two collections - primary / secondary
                    Collection<ProteinImpl> primaryProteins = proteinDao.getByUniprotId(uniprotAc);
                    Collection<ProteinImpl> secondaryProteins = new ArrayList<ProteinImpl>();

                    for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
                        secondaryProteins.addAll(proteinDao.getByUniprotId(secondaryAc));
                    }

                    // filter and remove non-uniprot prots from the list, and assign to the primary or secondary collections
                    ProteinTools.filterNonUniprotAndMultipleUniprot(primaryProteins);
                    ProteinTools.filterNonUniprotAndMultipleUniprot(secondaryProteins);

                    int countPrimary = primaryProteins.size();
                    int countSecondary = secondaryProteins.size();

                    if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotAc);

                    if (countSecondary > 0){
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            UpdateCaseEvent updateEvent = new UpdateCaseEvent(updateProcessor, evt.getDataContext(), uniprotProtein, Collections.EMPTY_LIST, secondaryProteins);
                            updateEvent.setUniprotServiceResult(new UniprotServiceResult(uniprotId));

                            updateProcessor.fireOnSecondaryAcsFound(updateEvent);
                        }
                        else {
                            throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
                        }
                    }
                }
            }
        }
    }

    @Override
    public void onSecondaryAcsFound(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<? extends Protein> secondaryProteins = evt.getSecondaryProteins();
        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        if (serviceResult != null){
            String uniprotId = serviceResult.getQuerySentToService();

            if (secondaryProteins != null){
                if (uniprotProtein != null){
                    boolean isSpliceVariant = IdentifierChecker.isSpliceVariantId(uniprotId);
                    UniprotSpliceVariant spliceVariant = null;

                    if (isSpliceVariant){

                        Collection<UniprotSpliceVariant> variants = evt.getProtein().getSpliceVariants();

                        for (UniprotSpliceVariant variant : variants){
                            Collection<String> variantAcs = variant.getSecondaryAcs();

                            if (variant.getPrimaryAc().equalsIgnoreCase(uniprotId) || variantAcs.contains(uniprotId)){
                                spliceVariant = variant;
                                break;
                            }
                        }
                    }


                    for (Protein prot : secondaryProteins){
                        // if a splice variant,
                        if (spliceVariant != null){
                            Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs(prot, spliceVariant, uniprotProtein);
                            serviceResult.getXrefUpdaterReports().addAll(xrefReports);
                        }
                        else {
                            Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateUniprotXrefs(prot, uniprotProtein);
                            serviceResult.getXrefUpdaterReports().addAll(xrefReports);
                        }
                    }
                }
                else {
                    throw new ProcessorException("The uniprot protein should never be null to update secondary proteins");
                }
            }
            else {
                throw new ProcessorException("The list of secondaryAcs should never be null.");
            }
        }
        else {
            throw new ProcessorException("The uniprot service result should never be null to update secondary proteins as it contains the uniprot primary ac of the protein which is updated.");
        }
    }
}
