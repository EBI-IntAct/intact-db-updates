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
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.ProtWithoutInteractionDeleterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Interaction;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.Set;

/**
 * Tester of ProtWithoutInteractionDeleterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProtWithouInteractionDeleterTest  extends IntactBasicTestCase {

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
     * A master protein without any splice variants or any feature chains which is involved in one interactions : cannot be deleted
     */
    public void protein_with_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(masterProt, randomProt);

        getCorePersister().saveOrUpdate(randomProt, interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(0, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));

        Assert.assertFalse(toDelete);
        getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A master protein with one feature chain which is involved in one interactions : cannot be deleted
     */
    public void feature_chain_with_interaction() throws Exception {
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

        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), chain));

        Assert.assertFalse(toDelete);
        Assert.assertFalse(toDeleteVariant);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A master protein without splice variants involved in one interactions : cannot be deleted
     */
    public void isoform_with_interaction() throws Exception {
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

        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), spliceVar1));

        Assert.assertFalse(toDelete);
        Assert.assertFalse(toDeleteVariant);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A master protein without any splice variants or any feature chains which is not involved in any interactions : must be deleted
     */
    public void protein_without_interaction() throws Exception {
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

        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));

        Assert.assertTrue(toDelete);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A master protein not involved in any interactions with one feature chain which is not involved in any interactions : must be deleted
     */
    public void feature_chain_without_interaction() throws Exception {
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
        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), chain));

        Assert.assertTrue(toDelete);
        Assert.assertTrue(toDeleteVariant);

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        // isDeleteProteinTranscriptWhitoutInteractions = false
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        boolean toDelete2 = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant2 = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), chain));

        Assert.assertFalse(toDelete2);
        Assert.assertTrue(toDeleteVariant2);

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A master protein not involved in any interactions with one splice variant which is not involved in any interactions : must be deleted
     */
    public void isoform_without_interaction() throws Exception {
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
        boolean toDelete = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), spliceVar1));

        Assert.assertTrue(toDelete);
        Assert.assertTrue(toDeleteVariant);

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        // isDeleteProteinTranscriptWhitoutInteractions = false
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        boolean toDelete2 = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));
        boolean toDeleteVariant2 = deleter.hasToBeDeleted(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), spliceVar1));

        Assert.assertFalse(toDelete2);
        Assert.assertTrue(toDeleteVariant2);

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Two proteins : one primary and one secondary. Both are involved in one interaction : 0 proteins to delete
     */
    public void collect_protein_with_interaction() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(masterProt, randomProt);

        getCorePersister().saveOrUpdate(randomProt, interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(0, getDaoFactory().getProteinDao().getSpliceVariants(masterProt).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(masterProt);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(randomProt);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(),
                null, primaryProteins, secondaryProteins, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "Q13948");

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());

        Set<Protein> proteinsToDelete = deleter.collectAndRemoveProteinsWithoutInteractions(evt);

        Assert.assertTrue(proteinsToDelete.isEmpty());
        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        getDataContext().commitTransaction(status);
    }
}
