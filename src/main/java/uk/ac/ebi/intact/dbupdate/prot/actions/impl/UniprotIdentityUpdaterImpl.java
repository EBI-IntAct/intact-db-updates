package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotIdentityUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
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
        UpdateCaseEvent caseEvt = new UpdateCaseEvent(evt.getSource(), evt.getDataContext(), evt.getUniprotProtein(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprotId);

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
        caseEvt.addAllToProteins(primaryProteins);
        caseEvt.addAllToProteins(secondaryProteins);

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
        addTranscriptWithoutParents(primaryIsoforms, secondaryIsoforms, uniprotTranscript, true, proteinDao);
    }

    /**
     * collect all intact proteins having a uniprot identity related to a specific uniprot transcript
     * but without proper parent and add them to the list of feature chains
     * @param primaryFeatureChains : the list of feature chains in intact
     * @param uniprotTranscript : the transcript
     * @param proteinDao
     */
    private void addFeatureChainsWithoutParents(Collection<ProteinTranscript> primaryFeatureChains, UniprotProteinTranscript uniprotTranscript, ProteinDao proteinDao){
        addTranscriptWithoutParents(primaryFeatureChains, Collections.EMPTY_LIST, uniprotTranscript, false, proteinDao);
    }

    /**
     * collect all intact proteins having a uniprot identity related to a specific uniprot transcript
     * but without proper parent and add them to the list of protein transcripts to update
     * @param primary : list of primary isoforms or feature chains
     * @param secondary : list of secondary isoforms (feature chains doesn't have secondary acs)
     * @param uniprotTranscript : the transcript
     * @param hasSecondary : true if isoform, false if feature chain
     * @param proteinDao
     */
    private void addTranscriptWithoutParents(Collection<ProteinTranscript> primary, Collection<ProteinTranscript> secondary, UniprotProteinTranscript uniprotTranscript, boolean hasSecondary, ProteinDao proteinDao){
        // get all proteins having same primary ac as the protein transcript
        List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotTranscript.getPrimaryAc());

        // load
        ProteinTools.loadCollections(proteinsInIntact);

        // for each isoform in intact having this primary ac
        for (ProteinImpl p : proteinsInIntact){
            // if it is not a splice variant, it means that the protein doesn't have parent which should be added later
            // add it to the list of primary isoforms
            if (!ProteinUtils.isSpliceVariant(p) && !ProteinUtils.isFeatureChain(p)){
                primary.add(new ProteinTranscript(p, uniprotTranscript));
            }
        }

        if (hasSecondary){
            // try to catch all proteins in intact having one of the secondary acs of the component
            for (String secondaryAc : uniprotTranscript.getSecondaryAcs()) {
                proteinsInIntact.clear();
                proteinsInIntact = proteinDao.getByUniprotId(secondaryAc);
                ProteinTools.loadCollections(proteinsInIntact);

                for (ProteinImpl p : proteinsInIntact){
                    // if it is not a splice variant, it means that the protein doesn't have isoform parent which should be added later
                    // add it to the list of primary isoforms
                    if (!ProteinUtils.isSpliceVariant(p) && !ProteinTools.isFeatureChain(p)){
                        secondary.add(new ProteinTranscript(p, uniprotTranscript));
                    }
                }
            }
        }
    }

    /**
     * Fill the updateCase event with isoforms from intact which are related to the uniprot entry of this event
     * @param evt
     */
    private void collectSpliceVariants(UpdateCaseEvent evt){
        // collect all splice variants of the uniprot entry
        Collection<UniprotSpliceVariant> variants = evt.getProtein().getSpliceVariants();

        // lists of splice variants in intact
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        // set lists of isoforms
        evt.setPrimaryIsoforms(primaryIsoforms);
        evt.setSecondaryIsoforms(secondaryIsoforms);

        // collect primary and secondary isoforms
        collectSpliceVariantsFrom(evt, evt.getPrimaryProteins(), variants, proteinDao, true);
        collectSpliceVariantsFrom(evt, evt.getSecondaryProteins(), variants, proteinDao, false);
    }

    /**
     * Fill the updateCase event with isoforms from intact which are related to the uniprot entry of this event
     * @param evt
     */
    private void collectFeatureChains(UpdateCaseEvent evt){
        // collect all feature chains of the uniprot entry
        Collection<UniprotFeatureChain> chains = evt.getProtein().getFeatureChains();
        // list of feature chains in intact
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();
        // set lists of isoforms
        evt.setPrimaryFeatureChains(primaryChains);

        // collect feature chains
        collectFeatureChainsFrom(evt, evt.getPrimaryProteins(), chains, proteinDao, true);
        collectFeatureChainsFrom(evt, evt.getSecondaryProteins(), chains, proteinDao, false);
    }

    /**
     * Collect all splice variants in intact matching one of the uniprot splice variants
     * @param evt
     * @param proteins
     * @param variants
     * @param proteinDao
     * @param collectTranscriptWithoutParents : true if we want to collect splice variants without parents as well having a uniprot ac related to this uniprot entry
     */
    private void collectSpliceVariantsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotSpliceVariant> variants, ProteinDao proteinDao, boolean collectTranscriptWithoutParents) {
        // isoforms
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();

        // for each master protein, collect its isoforms and try to remap it to one of the uniprot transcript
        for (Protein primary : proteins){
            List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants( primary );

            ProteinTools.loadCollections(spliceVariants);

            if (!spliceVariants.isEmpty()){
                for (Protein variant : spliceVariants){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (!ProteinUtils.isFromUniprot(variant)){
                        primaryIsoforms.add(new ProteinTranscript(variant, null));
                        evt.getProteins().add(variant.getAc());
                    }
                    else if (uniprotId != null){
                        boolean hasFoundAc = false;

                        for (UniprotSpliceVariant sv : variants){
                            Collection<String> variantAcs = sv.getSecondaryAcs();

                            if (sv.getPrimaryAc().equalsIgnoreCase(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                primaryIsoforms.add(new ProteinTranscript(variant, sv));
                                evt.getProteins().add(variant.getAc());
                            }
                            else if (variantAcs.contains(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                secondaryIsoforms.add(new ProteinTranscript(variant, sv));
                                evt.getProteins().add(variant.getAc());
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

        // if enabled, collect all splice variants having one of the variant acs but without parents attached to it
        if (collectTranscriptWithoutParents){
            for (UniprotSpliceVariant variant : variants){
                addIsoformsWithoutParents(evt.getPrimaryIsoforms(), evt.getSecondaryIsoforms(), variant, proteinDao);
            }
        }
    }

    /**
     * Collect all feature chains in intact matching one of the uniprot feature chain
     * @param evt
     * @param proteins
     * @param variants
     * @param proteinDao
     * @param collectTranscriptWithoutParents : true if we want to collect splice variants without parents as well having a uniprot ac related to this uniprot entry
     */
    private void collectFeatureChainsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotFeatureChain> variants, ProteinDao proteinDao, boolean collectTranscriptWithoutParents) {
        Collection<ProteinTranscript> primaryChains = evt.getPrimaryFeatureChains();

        // for each master protein, collect its feature chains and try to remap it to one of the uniprot transcript
        for (Protein primary : proteins){
            List<ProteinImpl> featureChains = proteinDao.getProteinChains( primary );

            ProteinTools.loadCollections(featureChains);

            if (!featureChains.isEmpty()){
                for (Protein variant : featureChains){
                    InteractorXref uniprotId = ProteinUtils.getUniprotXref(variant);

                    if (!ProteinUtils.isFromUniprot(variant)){
                        primaryChains.add(new ProteinTranscript(variant, null));
                        evt.getProteins().add(variant.getAc());
                    }
                    else if (uniprotId != null){
                        boolean hasFoundAc = false;

                        for (UniprotFeatureChain fc : variants){

                            if (fc.getPrimaryAc().equalsIgnoreCase(uniprotId.getPrimaryId())){
                                hasFoundAc = true;
                                primaryChains.add(new ProteinTranscript(variant, fc));
                                evt.getProteins().add(variant.getAc());
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

        // if enabled, collect all feature chains having one of the variant acs but without parents attached to it
        if (collectTranscriptWithoutParents){
            for (UniprotFeatureChain variant : variants){
                addFeatureChainsWithoutParents(evt.getPrimaryFeatureChains(), variant, proteinDao);
            }
        }
    }

    /**
     * All secondary proteins and secondary isoforms are updated to have a valid primary ac as uniprot identity and not a secondary ac
     * @param evt
     */
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

    /**
     * Update the secondary proteins
     * @param evt
     * @throws ProcessorException
     */
    private void updateSecondaryAcsForProteins(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();

        UniprotProtein uniprotProtein = evt.getProtein();

        for (Protein prot : secondaryProteins){

            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                XrefUpdaterReport xrefReport = XrefUpdaterUtils.updateUniprotXrefs(prot, uniprotProtein, evt.getDataContext(), processor);
                evt.getXrefUpdaterReports().add(xrefReport);
            }
            primaryProteins.add(prot);
        }

        evt.getSecondaryProteins().clear();
    }

    /**
     * Update the secondary isoforms
     * @param evt
     * @throws ProcessorException
     */
    private void updateSecondaryAcsForIsoforms(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<ProteinTranscript> secondaryProteins = evt.getSecondaryIsoforms();
        Collection<ProteinTranscript> primaryProteins = evt.getPrimaryIsoforms();

        UniprotProtein uniprotProtein = evt.getProtein();

        for (ProteinTranscript prot : secondaryProteins){
            UniprotProteinTranscript spliceVariant = prot.getUniprotVariant();

            if (spliceVariant != null){
                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    XrefUpdaterReport xrefReport = XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs(prot.getProtein(), spliceVariant, uniprotProtein, evt.getDataContext(), processor);
                    evt.getXrefUpdaterReports().add(xrefReport);
                }
                primaryProteins.add(prot);
            }
        }

        evt.getSecondaryIsoforms().clear();
    }    
}
