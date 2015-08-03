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
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.*;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tester of DuplicateFixer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class DuplicateFixerTest extends IntactBasicTestCase{

    private DuplicatesFixerImpl duplicateFixer;

    @Before
    public void setUp(){
        duplicateFixer = new DuplicatesFixerImpl(new ProteinDeleterImpl(), new OutOfDateParticipantFixerImpl(new RangeFixerImpl()), new DuplicatesFinderImpl());
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }
    @After
    public void after() throws Exception {
        duplicateFixer = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with the same sequence. Each of the duplicates has a splice variant and a feature chain
     * Should be merged without updating the sequence and/or shifting the ranges.
     * The duplicate should be deleted, a secondary accession should be added to the original protein and all splice variants and
     * feature chains should be remapped.
     * The interactions should be remapped.
     */
    public void merge_duplicate_same_sequence(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.getBioSource().setTaxId("9606");
        prot.setSequence(sequence);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        isoform.getBioSource().setTaxId("9606");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform3");
        isoform2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        chain.getBioSource().setTaxId("9606");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain");
        chain2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getSpliceVariants(primary).size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getProteinChains(primary).size());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(2, evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertTrue(hasXRef(evt.getReferenceProtein(), prot.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getSpliceVariants(evt.getReferenceProtein()).size());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getProteinChains(evt.getReferenceProtein()).size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void merge_duplicate_same_sequence_duplicated_component_only_2_participants(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        primary.setSequence(sequence);
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.getBioSource().setTaxId("9606");
        prot.setSequence(sequence);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        isoform.getBioSource().setTaxId("9606");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform3");
        isoform2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        chain.getBioSource().setTaxId("9606");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain");
        chain2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);

        for (Component c : interactionPrimary.getComponents()){
            c.getFeatures().clear();
        }

        Interaction interactionToMove = getMockBuilder().createInteraction(prot, primary);
        for (Component c : interactionToMove.getComponents()){
            c.getFeatures().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());
        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().countAll());
        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(3, evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().countAll());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void merge_duplicate_same_sequence_duplicated_component(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        primary.setSequence(sequence);
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.getBioSource().setTaxId("9606");
        prot.setSequence(sequence);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        isoform.getBioSource().setTaxId("9606");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform3");
        isoform2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        chain.getBioSource().setTaxId("9606");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain");
        chain2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);

        for (Component c : interactionPrimary.getComponents()){
            c.getFeatures().clear();
        }

        Interaction interactionToMove = getMockBuilder().createInteraction(prot, primary, random2);
        for (Component c : interactionToMove.getComponents()){
            c.getFeatures().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());
        Assert.assertEquals(5, IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().countAll());
        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(2, evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().countAll());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with the same sequence. Each of the duplicates has a splice variant and a feature chain.
     * The duplicate contains another intact-secondary cross reference which should be added to the original protein at the end
     * Should be merged without updating the sequence and/or shifting the ranges.
     * The duplicate should be deleted and all splice variants and
     * feature chains should be remapped.
     * The interactions should be remapped.
     */
    public void merge_duplicate_same_sequence_copy_several_intact_secondary(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String previousSecondary = "EBI-XXXX";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.setSequence(sequence);
        prot.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        InteractorXref secondary = getMockBuilder().createXref(prot, previousSecondary,
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary"),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));

        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).persist(secondary);
        prot.addXref(secondary);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        isoform.getBioSource().setTaxId("9606");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform3");
        isoform2.getBioSource().setTaxId("9606");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        chain.getBioSource().setTaxId("9606");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain");
        chain2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertTrue(hasXRef(evt.getReferenceProtein(), prot.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertTrue(hasXRef(evt.getReferenceProtein(), previousSecondary, CvDatabase.INTACT, "intact-secondary"));

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with the same sequence. Each of the duplicates has a splice variant and a feature chain.
     * One of the splice variants has two parents and only the proper parent should be updated.
     * Should be merged without updating the sequence and/or shifting the ranges.
     * The duplicate should be deleted and all splice variants and
     * feature chains should be remapped.
     * The interactions should be remapped.
     */
    public void merge_duplicate_same_sequence_several_parents(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String previousSecondary = "EBI-XXXX";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.setSequence(sequence);
        prot.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        isoform.getBioSource().setTaxId("9606");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform3");
        isoform2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref parent = getMockBuilder().createXref(isoform2, previousSecondary,
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).persist(parent);
        isoform2.addXref(parent);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        chain.getBioSource().setTaxId("9606");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain");
        chain2.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getSpliceVariants(primary).size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getProteinChains(primary).size());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertTrue(hasXRef(isoform2, previousSecondary, CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertTrue(hasXRef(isoform2, evt.getReferenceProtein().getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with different sequences and no features.
     * Should be merged after updating the sequence and/or shifting the ranges.
     * The duplicate should be deleted and all splice variants and
     * feature chains should be remapped.
     * The interactions should be remapped.
     */
    public void merge_duplicate_different_sequences_no_range_conflicts(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String sequence2 = "AAPPPFFFLLLMMMMM";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        primary.setSequence(sequence);
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.getBioSource().setTaxId("9606");
        prot.setSequence(sequence2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }

        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(2, evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertEquals(uniprot.getSequence(), evt.getUniprotSequence());
        Assert.assertEquals(sequence, evt.getReferenceProtein().getSequence());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with different sequences and the duplicate contains features with range conflicts.
     * Should be merged after updating the sequence and/or shifting the ranges.
     * The duplicate should not be deleted (no-uniprot-update and caution) and all splice variants and
     * feature chains should be remapped.
     * The valid interactions should be remapped, the interaction with range conflicts should be attached to the duplicate 'no-uniprot-update'.
     */
    public void merge_duplicate_different_sequences_range_conflicts_duplicate(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String sequence2 = "AAPPPFFFLLLMMMMM";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.getBioSource().setTaxId("9606");
        prot.setSequence(sequence2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);
        Interaction interactionWithConflict = getMockBuilder().createInteraction(prot, random1);

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();
        }

        Feature f = getMockBuilder().createFeatureRandom();
        Range r = getMockBuilder().createRange(1, 1, 50, 50);

        f.getRanges().clear();
        f.addRange(r);

        for (Component c : interactionWithConflict.getComponents()){
            c.getBindingDomains().clear();

            if (prot.getAc().equalsIgnoreCase(c.getInteractor().getAc())){

                c.addBindingDomain(f);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionWithConflict);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(2, prot.getActiveInstances().size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().countAll());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertEquals(1, evt.getComponentsWithFeatureConflicts().size());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNotNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(2, evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertEquals(uniprot.getSequence(), evt.getUniprotSequence());
        Assert.assertEquals(sequence, evt.getReferenceProtein().getSequence());
        Assert.assertEquals(prot.getAc(), evt.getComponentsWithFeatureConflicts().keySet().iterator().next().getAc());
        Assert.assertEquals(sequence2, prot.getSequence());
        Assert.assertEquals(1, prot.getActiveInstances().size());
        Assert.assertTrue(hasAnnotation(prot, null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(prot, null, CvTopic.CAUTION));

        // the ranges have not been reset by the duplicate fixer
        Assert.assertEquals(1, r.getFromIntervalStart());
        Assert.assertEquals(1, r.getFromIntervalEnd());
        Assert.assertEquals(50, r.getToIntervalStart());
        Assert.assertEquals(50, r.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with different sequences and the duplicate contains features with range to shift without any conflicts..
     * Should be merged after updating the sequence and shifting the ranges.
     * The duplicate should be deleted and all splice variants and
     * feature chains should be remapped.
     * All the interactions should be remapped,
     */
    public void merge_duplicate_different_sequences_range_shifted_duplicate(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String sequence2 = uniprot.getSequence().substring(4);

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.setSequence(sequence2);
        prot.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);
        Interaction interactionWithFeatures = getMockBuilder().createInteraction(prot, random1);

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();
        }

        for (Component c : interactionWithFeatures.getComponents()){
            c.getBindingDomains().clear();

            if (prot.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                Feature f = getMockBuilder().createFeatureRandom();
                Range r = getMockBuilder().createRange(1, 1, 7, 7);

                f.getRanges().clear();
                f.addRange(r);
                c.addBindingDomain(f);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionWithFeatures);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(2, prot.getActiveInstances().size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().countAll());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(),"P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertTrue(evt.getComponentsWithFeatureConflicts().isEmpty());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(3,evt.getReferenceProtein().getActiveInstances().size());
        Assert.assertEquals(uniprot.getSequence(), evt.getUniprotSequence());
        Assert.assertEquals(sequence, evt.getReferenceProtein().getSequence());

        for (Component c : interactionWithFeatures.getComponents()){

            if (primary.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                Feature f = c.getBindingDomains().iterator().next();

                Range r = f.getRanges().iterator().next();

                Assert.assertEquals(5, r.getFromIntervalStart());
                Assert.assertEquals(5, r.getFromIntervalEnd());
                Assert.assertEquals(11, r.getToIntervalStart());
                Assert.assertEquals(11, r.getToIntervalEnd());
            }
        }

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two duplicates with different sequences and the original protein contains features with range conflicts.
     * Should not be merged after updating the sequence and/or shifting the ranges.
     * A new protein should be created (no-uniprot-update and caution).
     * The valid interactions should be remapped, the interaction with range conflicts should be attached to the 'no-uniprot-update' protein.
     */
    public void merge_duplicate_different_sequences_range_conflicts_originalProt(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";
        String sequence2 = "AAPPPFFFLLLMMMMM";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        prot.setSequence(sequence2);
        prot.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(prot, random2);
        Interaction interactionWithConflict = getMockBuilder().createInteraction(primary, random1);

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();
        }

        Feature f = getMockBuilder().createFeatureRandom();
        Range r = getMockBuilder().createRange(1, 1, 50, 50);

        f.getRanges().clear();
        f.addRange(r);

        for (Component c : interactionWithConflict.getComponents()){
            c.getBindingDomains().clear();

            if (primary.getAc().equalsIgnoreCase(c.getInteractor().getAc())){

                c.addBindingDomain(f);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionWithConflict);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());
        Assert.assertEquals(1, prot.getActiveInstances().size());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getRangeDao().countAll());

        // fix duplicates
        DuplicatesFoundEvent evt = new DuplicatesFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryProteins, uniprot.getSequence(), uniprot.getCrc64(), "P60953", "9606");

        duplicateFixer.fixProteinDuplicates(evt);

        Assert.assertNotNull(evt.getReferenceProtein());
        Assert.assertEquals(1, evt.getComponentsWithFeatureConflicts().size());

        Assert.assertEquals(primary.getAc(), evt.getReferenceProtein().getAc());
        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(prot.getAc()));
        Assert.assertEquals(3, evt.getReferenceProtein().getActiveInstances().size()); // not moved yet because it has to be processed by out of date participant fixerfirst
        Assert.assertEquals(uniprot.getSequence(), evt.getUniprotSequence());
        Assert.assertEquals(sequence, evt.getReferenceProtein().getSequence());

        Protein no_uniprot = evt.getComponentsWithFeatureConflicts().keySet().iterator().next();
        Assert.assertEquals(sequence, no_uniprot.getSequence());
        Assert.assertEquals(3, no_uniprot.getActiveInstances().size()); // not moved yet because it has to be processed by out of date participant fixer first
        Assert.assertFalse(hasAnnotation(no_uniprot, null, CvTopic.NON_UNIPROT));
        Assert.assertFalse(hasAnnotation(no_uniprot, null, CvTopic.CAUTION));

        // the ranges have not been reset by the duplicate fixer
        Assert.assertEquals(1, r.getFromIntervalStart());
        Assert.assertEquals(1, r.getFromIntervalEnd());
        Assert.assertEquals(50, r.getToIntervalStart());
        Assert.assertEquals(50, r.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    private boolean hasXRef( Protein p, String primaryAc, String databaseName, String qualifierName ) {
        final Collection<InteractorXref> refs = p.getXrefs();
        boolean hasXRef = false;

        for ( InteractorXref ref : refs ) {
            if (databaseName.equalsIgnoreCase(ref.getCvDatabase().getShortLabel())){
                if (qualifierName.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel())){
                    if (primaryAc.equalsIgnoreCase(ref.getPrimaryId())){
                        hasXRef = true;
                    }
                }
            }
        }

        return hasXRef;
    }

    private boolean hasAnnotation( Protein p, String text, String cvTopic) {
        final Collection<Annotation> annotations = p.getAnnotations();
        boolean hasAnnotation = false;

        for ( Annotation a : annotations ) {
            if (cvTopic.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                if (text == null){
                    hasAnnotation = true;
                }
                else if (text != null && a.getAnnotationText() != null){
                    if (text.equalsIgnoreCase(a.getAnnotationText())){
                        hasAnnotation = true;
                    }
                }
            }
        }

        return hasAnnotation;
    }
}
