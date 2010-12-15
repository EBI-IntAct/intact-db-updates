package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotIdentityUpdater;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterReport;
import uk.ac.ebi.intact.util.protein.utils.XrefUpdaterUtils;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * Updates the uniprot identity cross references if the protein becomes secondary protein and collect all proteins in intact attached to
 * a single entry
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>04-Nov-2010</pre>
 */

public class UniprotIdentityUpdaterImpl implements UniprotIdentityUpdater{

    /**
     * Logger for this class
     */
    private static final Log log = LogFactory.getLog( UniprotProteinRetrieverImpl.class );

    /**
     *
     * @param evt
     * @return an UpdateCaseEvent containing all the intact proteins attached to the single uniprot entry in the ProteinEvent
     * @throws ProcessorException
     */
    public UpdateCaseEvent collectPrimaryAndSecondaryProteins(ProteinEvent evt) throws ProcessorException {

        // the uniprot entry
        UniprotProtein uniprotProtein = evt.getUniprotProtein();
        // the uniprot id = query
        String uniprotId = evt.getUniprotIdentity();

        // new UpdateCaseEvent with empty collections
        UpdateCaseEvent caseEvt = new UpdateCaseEvent(evt.getSource(), evt.getDataContext(), evt.getUniprotProtein(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        // create a UniprotServiceResult with the query = uniprot id of the protein event
        UniprotServiceResult serviceResult = new UniprotServiceResult(uniprotId);
        caseEvt.setUniprotServiceResult(serviceResult);

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        // uniprot primary ac
        final String uniprotAc = uniprotProtein.getPrimaryAc();

        if (log.isDebugEnabled()) log.debug("Searching IntAct for Uniprot protein: "+ uniprotAc + ", "
                + uniprotProtein.getOrganism().getName() +" ("+uniprotProtein.getOrganism().getTaxid()+")");

        // we will assign the proteins to two collections - primary / secondary
        Collection<Protein> primaryProteins = new ArrayList<Protein>();

        // all intact proteins having the same uniprot primary ac as uniprot identity
        List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotAc);

        // load collections to avoid lazy initialization later
        ProteinTools.loadCollections(proteinsInIntact);

        // the proteins are added to the list of primary proteins to update
        primaryProteins.addAll(proteinsInIntact);

        // the collection containing secondary proteins
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();

        // try to collect each intact protein having one of the uniprot secondary ac as uniprot identity
        for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
            proteinsInIntact.clear();
            proteinsInIntact = proteinDao.getByUniprotId(secondaryAc);
            // load collections to avoid lazy initialization later
            ProteinTools.loadCollections(proteinsInIntact);

            // the proteins are added to the list of secondary proteins to update
            secondaryProteins.addAll(proteinsInIntact);
        }
        // number of primary proteins
        int countPrimary = primaryProteins.size();
        // number of secondary proteins
        int countSecondary = secondaryProteins.size();

        // set the list of proteins in the event
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

    /**
     * collect all intact proteins having a uniprot identity related to a specific uniprot transcript
     * but without proper parent and add them to the list of isoform primary and secondary
     * @param primaryIsoforms : list of primary isoforms
     * @param secondaryIsoforms : list of secondary isoforms
     * @param uniprotTranscript : the uniprot transcript
     * @param proteinDao
     */
    private void addIsoformsWithoutParents(Collection<ProteinTranscript> primaryIsoforms, Collection<ProteinTranscript> secondaryIsoforms, UniprotProteinTranscript uniprotTranscript, ProteinDao proteinDao){
        // get all proteins having same primary ac as the protein transcript
        List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotTranscript.getPrimaryAc());

        // load 
        ProteinTools.loadCollections(proteinsInIntact);

        for (ProteinImpl p : proteinsInIntact){
            if (!ProteinUtils.isSpliceVariant(p)){
                primaryIsoforms.add(new ProteinTranscript(p, uniprotTranscript));
            }
        }

        for (String secondaryAc : uniprotTranscript.getSecondaryAcs()) {
            proteinsInIntact.clear();
            proteinsInIntact = proteinDao.getByUniprotId(secondaryAc);
            ProteinTools.loadCollections(proteinsInIntact);

            for (ProteinImpl p : proteinsInIntact){
                if (!ProteinUtils.isSpliceVariant(p)){
                    secondaryIsoforms.add(new ProteinTranscript(p, uniprotTranscript));
                }
            }
        }
    }

    private void addFeatureChainsWithoutParents(Collection<ProteinTranscript> primaryFeatureChains, UniprotProteinTranscript uniprotTranscript, ProteinDao proteinDao){
        List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotTranscript.getPrimaryAc());

        ProteinTools.loadCollections(proteinsInIntact);

        for (ProteinImpl p : proteinsInIntact){
            if (!ProteinTools.isFeatureChain(p)){
                primaryFeatureChains.add(new ProteinTranscript(p, uniprotTranscript));
            }
        }
    }

    private void collectSpliceVariants(UpdateCaseEvent evt){
        Collection<UniprotSpliceVariant> variants = evt.getProtein().getSpliceVariants();
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        evt.setPrimaryIsoforms(primaryIsoforms);
        evt.setSecondaryIsoforms(secondaryIsoforms);

        collectSpliceVariantsFrom(evt, evt.getPrimaryProteins(), variants, proteinDao, true);
        collectSpliceVariantsFrom(evt, evt.getSecondaryProteins(), variants, proteinDao, false);
    }

    private void collectSpliceVariantsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotSpliceVariant> variants, ProteinDao proteinDao, boolean collectTranscriptWithoutParents) {
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();

        for (Protein primary : proteins){
            List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants( primary );

            ProteinTools.loadCollections(spliceVariants);

            if (!spliceVariants.isEmpty()){
                for (Protein variant : spliceVariants){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (!ProteinUtils.isFromUniprot(variant)){
                        primaryIsoforms.add(new ProteinTranscript(variant, null));
                        evt.getUniprotServiceResult().getProteins().add(variant);
                    }
                    else if (uniprotId != null){
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
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't match any splice variants of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript, variant));
                            }
                        }
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't have a uniprot identifier and doesn't match any splice variants of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript, variant));
                        }
                    }
                }
            }
        }

        if (collectTranscriptWithoutParents){
            for (UniprotSpliceVariant variant : variants){
                addIsoformsWithoutParents(evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), variant, proteinDao);
            }
        }
    }

    private void collectFeatureChains(UpdateCaseEvent evt){
        Collection<UniprotFeatureChain> chains = evt.getProtein().getFeatureChains();
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        evt.setPrimaryFeatureChains(primaryChains);

        collectFeatureChainsFrom(evt, evt.getPrimaryProteins(), chains, proteinDao, true);
        collectFeatureChainsFrom(evt, evt.getSecondaryProteins(), chains, proteinDao, false);
    }

    private void collectFeatureChainsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotFeatureChain> variants, ProteinDao proteinDao, boolean collectTranscriptWithoutParents) {
        Collection<ProteinTranscript> primaryChains = evt.getPrimaryFeatureChains();

        for (Protein primary : proteins){
            List<ProteinImpl> featureChains = proteinDao.getProteinChains( primary );

            ProteinTools.loadCollections(featureChains);

            if (!featureChains.isEmpty()){
                for (Protein variant : featureChains){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (!ProteinUtils.isFromUniprot(variant)){
                        primaryChains.add(new ProteinTranscript(variant, null));
                        evt.getUniprotServiceResult().getProteins().add(variant);
                    }
                    else if (uniprotId != null){
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
                                updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The feature chain " + variant.getAc() + " doesn't match any feature chains of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript, variant));
                            }
                        }
                    }
                    else {
                        if (evt.getSource() instanceof ProteinUpdateProcessor) {
                            final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                            updateProcessor.fireOnProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The feature chain " + variant.getAc() + " doesn't have a uniprot identifier and doesn't match any feature chains of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript, variant));
                        }
                    }
                }
            }
        }

        if (collectTranscriptWithoutParents){
            for (UniprotFeatureChain variant : variants){
                addFeatureChainsWithoutParents(evt.getPrimaryFeatureChains(), variant, proteinDao);
            }
        }
    }

    public void updateAllSecondaryProteins(UpdateCaseEvent evt) {
        if (evt.getSecondaryProteins().size() > 0){
            updateSecondaryAcsForProteins(evt);
        }
        if (evt.getSecondaryIsoforms().size() > 0){
            updateSecondaryAcsForIsoforms(evt);
        }

        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnSecondaryAcsFound(evt);
        }
        else {
            throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
        }
    }

    private void updateSecondaryAcsForProteins(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();

        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        for (Protein prot : secondaryProteins){

            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateUniprotXrefs(prot, uniprotProtein, evt.getDataContext(), processor);
                serviceResult.getXrefUpdaterReports().addAll(xrefReports);
            }
            primaryProteins.add(prot);
        }

        evt.getSecondaryProteins().clear();
    }

    private void updateSecondaryAcsForIsoforms(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<ProteinTranscript> secondaryProteins = evt.getSecondaryIsoforms();
        Collection<ProteinTranscript> primaryProteins = evt.getPrimaryIsoforms();

        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        for (ProteinTranscript prot : secondaryProteins){
            UniprotProteinTranscript spliceVariant = prot.getUniprotVariant();

            if (spliceVariant != null){
                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs(prot.getProtein(), spliceVariant, uniprotProtein, evt.getDataContext(), processor);
                    serviceResult.getXrefUpdaterReports().addAll(xrefReports);
                }
                primaryProteins.add(prot);
            }
        }

        evt.getSecondaryIsoforms().clear();
    }
}
