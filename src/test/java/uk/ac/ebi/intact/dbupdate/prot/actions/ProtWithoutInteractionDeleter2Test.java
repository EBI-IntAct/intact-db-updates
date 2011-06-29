package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.ProtWithoutInteractionDeleterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Interaction;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Second tester of ProtWithoutInteractionDeleterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>07-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProtWithoutInteractionDeleter2Test  extends IntactBasicTestCase {

    private ProtWithoutInteractionDeleterImpl deleter;
    @Before
    public void before_schema() throws Exception {
        deleter = new ProtWithoutInteractionDeleterImpl();

        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        deleter = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Three proteins : one primary without interaction but with one isoform (primary as well) involved in one interaction
     * and one secondary involved in one interaction. 0 proteins to delete
     */
    public void collect_feature_chain_with_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein chain = getMockBuilder().createProteinChain(masterProt, "P12345-1", "fc11");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(chain, randomProt);

        getCorePersister().saveOrUpdate(chain, randomProt, interaction);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getProteinChains(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryFeatureChains = new ArrayList<ProteinTranscript>();
        primaryFeatureChains.add(new ProteinTranscript(chain, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryFeatureChains, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertTrue(proteinsToDelete.isEmpty());
        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Three proteins : one primary without interaction but with one feature chain involved in one interaction
     * and one secondary involved in one interaction. 0 proteins to delete
     */
    public void collect_isoform_with_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar1 = getMockBuilder().createProteinSpliceVariant(masterProt, "P12345-1", "sv11");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(spliceVar1, randomProt);

        getCorePersister().saveOrUpdate(spliceVar1, randomProt, interaction);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(spliceVar1, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertTrue(proteinsToDelete.isEmpty());
        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Two proteins : one primary and one secondary. Both are not involved in any interactions : 2 proteins to delete
     */
    public void collect_protein_without_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: no
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        getCorePersister().saveOrUpdate(randomProt);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(0, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(0, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(0, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertEquals(2, proteinsToDelete.size());
        Assert.assertEquals(0, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getSecondaryProteins().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Three proteins : one primary without interaction with one feature chain without interaction
     * and one secondary without interaction. 3 proteins to delete if isDeleteProteinTranscriptWithoutInteractions, 2 otherwise
     */
    public void collect_feature_chain_without_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: no
        Protein chain = getMockBuilder().createProteinChain(masterProt, "P12345-1", "fc11");

        // interaction: no
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        getCorePersister().saveOrUpdate(chain, randomProt);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(0, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getProteinChains(masterProt).size());
        Assert.assertEquals(0, getDaoFactory().getInteractionDao().countAll());

        // isDeleteProteinTranscriptWhitoutInteractions = true
        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertEquals(3, proteinsToDelete.size());
        Assert.assertEquals(0, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getSecondaryProteins().size());
        Assert.assertEquals(0, evt.getPrimaryFeatureChains().size());

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        // isDeleteProteinTranscriptWhitoutInteractions = false
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        Collection<Protein> primaryProteins2 = new ArrayList<Protein>();
        primaryProteins2.add(masterProt);
        Collection<Protein> secondaryProteins2 = new ArrayList<Protein>();
        secondaryProteins2.add(randomProt);
        Collection<ProteinTranscript> primaryChains2 = new ArrayList<ProteinTranscript>();
        primaryChains2.add(new ProteinTranscript(chain, null));

        UpdateCaseEvent evt2 = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins2, secondaryProteins2, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains2, "P12345");

        Assert.assertEquals(1, evt2.getPrimaryProteins().size());
        Assert.assertEquals(1, evt2.getSecondaryProteins().size());
        Assert.assertEquals(1, evt2.getPrimaryFeatureChains().size());

        Set<Protein> proteinsToDelete2 = deleter.collectAndRemoveProteinsWithoutInteractions(evt2);

        Assert.assertEquals(2, proteinsToDelete2.size());
        Assert.assertEquals(1, evt2.getPrimaryProteins().size());
        Assert.assertEquals(0, evt2.getSecondaryProteins().size());
        Assert.assertEquals(0, evt2.getPrimaryFeatureChains().size());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Three proteins : one primary without interaction with one splice variant without interaction
     * and one secondary without interaction. 3 proteins to delete if isDeleteProteinTranscriptWithoutInteractions, 2 otherwise
     */
    public void collect_isoform_without_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: no
        Protein spliceVar1 = getMockBuilder().createProteinSpliceVariant(masterProt, "P12345-1", "sv11");

        // interaction: no
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        getCorePersister().saveOrUpdate(spliceVar1, randomProt);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(0, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(0, getDaoFactory().getInteractionDao().countAll());

        // isDeleteProteinTranscriptWhitoutInteractions = true
        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(spliceVar1, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertEquals(3, proteinsToDelete.size());
        Assert.assertEquals(0, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getSecondaryProteins().size());
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        // isDeleteProteinTranscriptWhitoutInteractions = false
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        Collection<Protein> primaryProteins2 = new ArrayList<Protein>();
        primaryProteins2.add(masterProt);
        Collection<Protein> secondaryProteins2 = new ArrayList<Protein>();
        secondaryProteins2.add(randomProt);
        Collection<ProteinTranscript> primaryIsoforms2 = new ArrayList<ProteinTranscript>();
        primaryIsoforms2.add(new ProteinTranscript(spliceVar1, null));

        UpdateCaseEvent evt2 = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins2, secondaryProteins2, primaryIsoforms2, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "P12345");

        Assert.assertEquals(1, evt2.getPrimaryProteins().size());
        Assert.assertEquals(1, evt2.getSecondaryProteins().size());
        Assert.assertEquals(1, evt2.getPrimaryIsoforms().size());

        Set<Protein> proteinsToDelete2 = deleter.collectAndRemoveProteinsWithoutInteractions(evt2);

        Assert.assertEquals(2, proteinsToDelete2.size());
        Assert.assertEquals(1, evt2.getPrimaryProteins().size());
        Assert.assertEquals(0, evt2.getSecondaryProteins().size());
        Assert.assertEquals(0, evt2.getPrimaryIsoforms().size());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Four proteins : one primary without interaction with two feature chains (one with interaction, the other without)
     * and one secondary without interaction. 1 proteins to delete
     */
    public void collect_feature_chain_with_and_without_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein chain = getMockBuilder().createProteinChain(masterProt, "P12345-1", "fc11");

        // interaction: no
        Protein chain2 = getMockBuilder().createProteinChain(masterProt, "P12345-2", "fc12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(chain, randomProt);

        getCorePersister().saveOrUpdate(chain, chain2, randomProt, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getProteinChains(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryFeatureChains = new ArrayList<ProteinTranscript>();
        primaryFeatureChains.add(new ProteinTranscript(chain, null));
        primaryFeatureChains.add(new ProteinTranscript(chain2, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryFeatureChains, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getPrimaryFeatureChains().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertEquals(1, proteinsToDelete.size());
        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Four proteins : one primary without interaction with two splice variants (one with interaction (primary), the other without(secondary))
     * and one secondary without interaction. 1 proteins to delete
     */
    public void collect_isoform_with_and_without_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar1 = getMockBuilder().createProteinSpliceVariant(masterProt, "P12345-1", "sv11");
        // interaction: no
        Protein spliceVar2 = getMockBuilder().createProteinSpliceVariant(masterProt, "P12345-2", "sv12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(spliceVar1, randomProt);

        getCorePersister().saveOrUpdate(spliceVar1, spliceVar2, randomProt, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(spliceVar1, null));
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(spliceVar2, null));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, primaryIsoforms, secondaryIsoforms, Collections.EMPTY_LIST, "P12345");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertEquals(1, proteinsToDelete.size());
        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

}
