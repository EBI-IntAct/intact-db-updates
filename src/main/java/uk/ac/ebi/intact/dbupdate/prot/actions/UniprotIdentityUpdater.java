package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.hibernate.Hibernate;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.UpdateError;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateErrorEvent;
import uk.ac.ebi.intact.model.Component;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
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

        List<ProteinImpl> proteinsInIntact = proteinDao.getByUniprotId(uniprotAc);

        loadCollections(proteinsInIntact);

        primaryProteins.addAll(proteinsInIntact);

        Collection<Protein> secondaryProteins = new ArrayList<Protein>();

        for (String secondaryAc : uniprotProtein.getSecondaryAcs()) {
            proteinsInIntact.clear();
            proteinsInIntact = proteinDao.getByUniprotId(secondaryAc);
            loadCollections(proteinsInIntact);
            secondaryProteins.addAll(proteinsInIntact);
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

    private void loadCollections(List<ProteinImpl> proteinsInIntact) {
        for (Protein p : proteinsInIntact){
            Hibernate.initialize(p.getXrefs());
            Hibernate.initialize(p.getAnnotations());
            Hibernate.initialize(p.getAliases());
            for (Component c : p.getActiveInstances()){
                Hibernate.initialize(c.getXrefs());
                Hibernate.initialize(c.getAnnotations());
                Hibernate.initialize(c.getBindingDomains());
                Hibernate.initialize(c.getExperimentalRoles());
                Hibernate.initialize(c.getAliases());
                Hibernate.initialize(c.getExperimentalPreparations());
                Hibernate.initialize(c.getParameters());
                Hibernate.initialize(c.getParticipantDetectionMethods());
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

        collectSpliceVariantsFrom(evt, evt.getPrimaryProteins(), variants, proteinDao);
        collectSpliceVariantsFrom(evt, evt.getSecondaryProteins(), variants, proteinDao);
    }

    private void collectSpliceVariantsFrom(UpdateCaseEvent evt, Collection<Protein> proteins, Collection<UniprotSpliceVariant> variants, ProteinDao proteinDao) {
        Collection<ProteinTranscript> primaryIsoforms = evt.getPrimaryIsoforms();
        Collection<ProteinTranscript> secondaryIsoforms = evt.getSecondaryIsoforms();

        for (Protein primary : proteins){
            List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants( primary );

            loadCollections(spliceVariants);

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
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't match any splice variants of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                            }
                        }
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
            List<ProteinImpl> featureChains = proteinDao.getProteinChains( primary );

            loadCollections(featureChains);

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
                                updateProcessor.fireonProcessErrorFound(new UpdateErrorEvent(updateProcessor, evt.getDataContext(), "The splice variant " + variant.getAc() + " doesn't match any feature chains of the uniprot entry " + evt.getProtein().getPrimaryAc(), UpdateError.not_matching_protein_transcript));
                            }
                        }
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

    public void updateAllSecondaryProteins(UpdateCaseEvent evt) {
        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnSecondaryAcsFound(evt);
        }
        else {
            throw new ProcessorException("The proteinProcessor should be of type ProteinUpdateProcessor but is of type " + evt.getSource().getClass().getName());
        }

        if (evt.getSecondaryProteins().size() > 0){
            updateSecondaryAcsForProteins(evt);
        }
        if (evt.getSecondaryIsoforms().size() > 0){
            updateSecondaryAcsForIsoforms(evt);
        }
    }

    private void updateSecondaryAcsForProteins(UpdateCaseEvent evt)  throws ProcessorException {
        Collection<Protein> secondaryProteins = evt.getSecondaryProteins();
        Collection<Protein> primaryProteins = evt.getPrimaryProteins();

        UniprotServiceResult serviceResult = evt.getUniprotServiceResult();
        UniprotProtein uniprotProtein = evt.getProtein();

        for (Protein prot : secondaryProteins){

            Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateUniprotXrefs(prot, uniprotProtein, evt.getDataContext());
            serviceResult.getXrefUpdaterReports().addAll(xrefReports);
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
                Collection<XrefUpdaterReport> xrefReports = XrefUpdaterUtils.updateProteinTranscriptUniprotXrefs(prot.getProtein(), spliceVariant, uniprotProtein, evt.getDataContext());
                serviceResult.getXrefUpdaterReports().addAll(xrefReports);
                primaryProteins.add(prot);
            }
        }

        evt.getSecondaryIsoforms().clear();
    }
}
