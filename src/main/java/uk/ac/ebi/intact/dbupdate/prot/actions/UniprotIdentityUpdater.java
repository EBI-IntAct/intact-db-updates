package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinRetriever;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
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

public class UniprotIdentityUpdater {

    private static final Log log = LogFactory.getLog( UniprotProteinRetriever.class );

    public UpdateCaseEvent collectPrimaryAndSecondaryProteins(ProteinEvent evt) throws ProcessorException {

        UniprotProtein uniprotProtein = evt.getUniprotProtein();
        String uniprotId = evt.getUniprotIdentity();

        UpdateCaseEvent caseEvt = new UpdateCaseEvent(evt.getSource(), evt.getDataContext(), evt.getUniprotProtein(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        UniprotServiceResult serviceResult = new UniprotServiceResult(uniprotId);
        caseEvt.setUniprotServiceResult(serviceResult);

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        final String uniprotAc = uniprotProtein.getPrimaryAc();

        if (log.isDebugEnabled()) log.debug("Searching IntAct for Uniprot protein: "+ uniprotAc + ", "
                + uniprotProtein.getOrganism().getName() +" ("+uniprotProtein.getOrganism().getTaxid()+")");

        // we will assign the proteins to two collections - primary / secondary
        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.addAll(proteinDao.getByUniprotId(uniprotAc));
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();

        for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
            secondaryProteins.addAll(proteinDao.getByUniprotId(secondaryAc));
        }

        int countPrimary = primaryProteins.size();
        int countSecondary = secondaryProteins.size();

        caseEvt.setPrimaryProteins(primaryProteins);
        caseEvt.setSecondaryProteins(secondaryProteins);
        caseEvt.getUniprotServiceResult().addAllToProteins(primaryProteins);
        caseEvt.getUniprotServiceResult().addAllToProteins(secondaryProteins);

        if (log.isTraceEnabled()) log.trace("Found "+countPrimary+" primary and "+countSecondary+" secondary for "+uniprotAc);

        // will update all splice variants as well
        collectSpliceVariants(caseEvt);

        // will update the feature chains as well
        collectFeatureChains(caseEvt);

        return caseEvt;
    }

    private void collectSpliceVariants(UpdateCaseEvent evt){
        Collection<UniprotSpliceVariant> variants = evt.getProtein().getSpliceVariants();
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        evt.setPrimaryIsoforms(primaryIsoforms);
        evt.setSecondaryIsoforms(secondaryIsoforms);

        collectSpliceVariantsFrom(evt, evt.getPrimaryProteins(), variants, proteinDao);
        collectSpliceVariantsFrom(evt, evt.getSecondaryProteins(), variants, proteinDao);
    }

    private void collectSpliceVariantsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotSpliceVariant> variants, ProteinDao proteinDao) {
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();

        for (Protein primary : proteins){
            Collection<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants( primary );

            if (!spliceVariants.isEmpty()){
                for (Protein variant : spliceVariants){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (uniprotId != null){
                        boolean hasFoundAc = false;

                        for (UniprotSpliceVariant sv : variants){
                            Collection<String> variantAcs = sv.getSecondaryAcs();

                            if (sv.getPrimaryAc().equalsIgnoreCase(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                primaryIsoforms.add(new ProteinTranscript(variant, sv));
                                evt.getUniprotServiceResult().getProteins().add(variant);
                            }
                            else if (variantAcs.contains(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                secondaryIsoforms.add(new ProteinTranscript(variant, sv));
                                evt.getUniprotServiceResult().getProteins().add(variant);
                            }
                        }

                        if (!hasFoundAc){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't match any splice variants of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                            }
                        }
                    }
                    else if (!ProteinUtils.isFromUniprot(variant)){
                        primaryIsoforms.add(new ProteinTranscript(variant, null));
                        evt.getUniprotServiceResult().getProteins().add(variant);
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't have a uniprot identifier and doesn't match any splice variants of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                        }
                    }
                }
            }
        }
    }

    private void collectFeatureChains(UpdateCaseEvent evt){
        Collection<UniprotFeatureChain> chains = evt.getProtein().getFeatureChains();
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        evt.setPrimaryFeatureChains(primaryChains);

        collectFeatureChainsFrom(evt, evt.getPrimaryProteins(), chains, proteinDao);
        collectFeatureChainsFrom(evt, evt.getSecondaryProteins(), chains, proteinDao);
    }

    private void collectFeatureChainsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotFeatureChain> variants, ProteinDao proteinDao) {
        Collection<ProteinTranscript> primaryChains = evt.getPrimaryFeatureChains();

        for (Protein primary : proteins){
            Collection<ProteinImpl> featureChains = proteinDao.getProteinChains( primary );

            if (!featureChains.isEmpty()){
                for (Protein variant : featureChains){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (uniprotId != null){
                        boolean hasFoundAc = false;

                        for (UniprotFeatureChain fc : variants){

                            if (fc.getPrimaryAc().equalsIgnoreCase(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                primaryChains.add(new ProteinTranscript(variant, fc));
                                evt.getUniprotServiceResult().getProteins().add(variant);
                            }
                        }

                        if (!hasFoundAc){
                            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't match any feature chains of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                            }
                        }
                    }
                    else if (!ProteinUtils.isFromUniprot(variant)){
                        primaryChains.add(new ProteinTranscript(variant, null));
                        evt.getUniprotServiceResult().getProteins().add(variant);
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The feature chain " + variant.getAc() + " doesn't have a uniprot identifier and doesn't match any feature chains of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                        }
                    }
                }
            }
        }
    }

    public void updateSecondaryAcsForProteins(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();

        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        if (evt.getSource() instanceof ProteinUpdateProcessor) {
            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

            updateProcessor.fireOnSecondaryAcsFound(evt);
        }
        else {
            throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
        }

        for (Protein prot : secondaryProteins){

            Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateUniprotXrefs(prot, uniprotProtein);
            serviceResult.getXrefUpdaterReports().addAll(xrefReports);
            primaryProteins.add(prot);
        }

        evt.getSecondaryProteins().clear();
    }

    public void updateSecondaryAcsForIsoforms(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<ProteinTranscript> secondaryProteins = evt.getSecondaryIsoforms();
        Collection<ProteinTranscript> primaryProteins = evt.getPrimaryIsoforms();

        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        if (evt.getSource() instanceof ProteinUpdateProcessor) {
            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();

            updateProcessor.fireOnSecondaryAcsFound(evt);
        }
        else {
            throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
        }

        for (ProteinTranscript prot : secondaryProteins){
            UniprotProteinTranscript spliceVariant = prot.getUniprotVariant();

            if (spliceVariant != null){
                Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs(prot.getProtein(), spliceVariant, uniprotProtein);
                serviceResult.getXrefUpdaterReports().addAll(xrefReports);
                primaryProteins.add(prot);
            }
        }

        evt.getSecondaryIsoforms().clear();
    }
}
