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
import uk.ac.ebi.intact.commons.util.Crc64;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.DuplicatesFinderImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Tester of the DuplicateFinder
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>12-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class DuplicateFinderTest extends IntactBasicTestCase {

    private DuplicatesFinderImpl duplicateFinder;

    @Before
    public void setUp(){
        duplicateFinder = new DuplicatesFinderImpl();
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        duplicateFinder = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create a primary protein.
     * Create one splice variant and one chain.
     * There are no duplicates.
     */
    public void find_no_duplicates(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        UniprotSpliceVariant variants1 = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
                break;
            }
        }

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variants1));

        Protein chain = getMockBuilder().createProteinSpliceVariant(primary, "PRO-1", "chain");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);

        UniprotFeatureChain chain1 = uniprot.getFeatureChains().iterator().next();

        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));

        // collect
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins, Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, "P60953");

        DuplicatesFoundEvent dupEvt = duplicateFinder.findProteinDuplicates(evt);
        Assert.assertNull(dupEvt);

        Collection<DuplicatesFoundEvent> dupIsoforms = duplicateFinder.findIsoformDuplicates(evt);
        Assert.assertTrue(dupIsoforms.isEmpty());

        Collection<DuplicatesFoundEvent> dupChains = duplicateFinder.findFeatureChainDuplicates(evt);
        Assert.assertTrue(dupChains.isEmpty());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two different proteins. One should be updated and is in the list of primary proteins to update
     * Create two splice variants : one attached to the protein to update, the other attached to the other protein.
     * Create two chains : one attached to the protein to update, the other attached to the other protein.
     * There are no duplicates because the parents are different for the protein transcripts.
     */
    public void find_no_duplicates_transcripts_several_transcripts_different_parents(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary, prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-2", "isoform2");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotSpliceVariant variants1 = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
                break;
            }
        }

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variants1));
        primaryIsoforms.add(new ProteinTranscript(isoform2, variants1));

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        Protein chain2 = getMockBuilder().createProteinChain(prot, "PRO-2", "chain2");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref ref2 = ProteinUtils.getUniprotXref(chain2);
        ref2.setPrimaryId("PRO-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(ref2);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotFeatureChain chain1 = uniprot.getFeatureChains().iterator().next();

        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));
        primaryChains.add(new ProteinTranscript(chain2, chain1));

        // collect
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins, Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, "P60953");

        DuplicatesFoundEvent dupEvt = duplicateFinder.findProteinDuplicates(evt);
        Assert.assertNull(dupEvt);

        Collection<DuplicatesFoundEvent> dupIsoforms = duplicateFinder.findIsoformDuplicates(evt);
        Assert.assertTrue(dupIsoforms.isEmpty());

        Collection<DuplicatesFoundEvent> dupChains = duplicateFinder.findFeatureChainDuplicates(evt);
        Assert.assertTrue(dupChains.isEmpty());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two different proteins. Both need to be updated and are primary proteins of a same uniprot protein.
     * Create two splice variants : both are attached to the same parent protein.
     * Create two chains : both are attached to the same parent protein.
     * There are duplicates because several parent proteins are primary proteins of a same uniprot entry and
     * the parents are identical for the protein transcripyts.
     */
    public void find_duplicates(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        Protein prot = getMockBuilder().createProtein("P60953x", "protein");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);
        primaryProteins.add(prot);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-2", "isoform2");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref identity = ProteinUtils.getUniprotXref(prot);
        identity.setPrimaryId("P60953");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity);

        InteractorXref identity2 = ProteinUtils.getUniprotXref(isoform2);
        identity2.setPrimaryId("P60953-1");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity2);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotSpliceVariant variants1 = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
                break;
            }
        }

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variants1));
        primaryIsoforms.add(new ProteinTranscript(isoform2, variants1));

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        Protein chain2 = getMockBuilder().createProteinChain(primary, "PRO-2", "chain2");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);

        InteractorXref identity3 = ProteinUtils.getUniprotXref(chain2);
        identity3.setPrimaryId("PRO-1");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity3);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotFeatureChain chain1 = uniprot.getFeatureChains().iterator().next();

        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));
        primaryChains.add(new ProteinTranscript(chain2, chain1));

        // collect
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins, Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, "P60953");

        DuplicatesFoundEvent dupEvt = duplicateFinder.findProteinDuplicates(evt);
        Assert.assertNotNull(dupEvt);
        Assert.assertEquals(dupEvt.getUniprotSequence(), uniprot.getSequence());
        Assert.assertEquals(dupEvt.getUniprotCrc64(), uniprot.getCrc64());
        Assert.assertEquals(2, dupEvt.getProteins().size());

        Collection<DuplicatesFoundEvent> dupIsoforms = duplicateFinder.findIsoformDuplicates(evt);
        Assert.assertEquals(1, dupIsoforms.size());
        DuplicatesFoundEvent isoEvt = dupIsoforms.iterator().next();
        Assert.assertEquals(isoEvt.getUniprotSequence(), variants1.getSequence());
        Assert.assertEquals(isoEvt.getUniprotCrc64(), Crc64.getCrc64(variants1.getSequence()));
        Assert.assertEquals(2, isoEvt.getProteins().size());

        Collection<DuplicatesFoundEvent> dupChains = duplicateFinder.findFeatureChainDuplicates(evt);
        Assert.assertEquals(1, dupChains.size());
        DuplicatesFoundEvent chainEvt = dupChains.iterator().next();
        Assert.assertEquals(chainEvt.getUniprotSequence(), chain1.getSequence());
        Assert.assertEquals(chainEvt.getUniprotCrc64(), Crc64.getCrc64(chain1.getSequence()));
        Assert.assertEquals(2, chainEvt.getProteins().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create two different proteins. Both need to be updated and are primary proteins of a same uniprot protein.
     * Create four splice variants : 2 are attached to the same parent protein and 2 other are attached to another set of parents.
     * Create four chains : 2 are attached to the same parent protein and 2 other are attached to another set of parents.
     * There are duplicates because several parent proteins are primary proteins of a same uniprot entry and
     * the parents are identical for the protein transcripts.
     * Here we have the case of two set of protein transcript duplicates
     */
    public void find_duplicates_several_transcript_several_parents(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        Protein prot = getMockBuilder().createProtein("P12345", "protein");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(primary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoform");
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-2", "isoform2");
        Protein isoform3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-3", "isoform3");
        Protein isoform4 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-4", "isoform4");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform3);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform4);

        InteractorXref identity = ProteinUtils.getUniprotXref(isoform2);
        identity.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(identity);

        InteractorXref identity2 = ProteinUtils.getUniprotXref(isoform4);
        identity2.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(identity2);

        InteractorXref identity3 = ProteinUtils.getUniprotXref(isoform3);
        identity3.setPrimaryId("P60953-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(identity3);

        InteractorXref parent = getMockBuilder().createXref(isoform4, primary.getAc(),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));

        getDaoFactory().getXrefDao(InteractorXref.class).persist(parent);

        isoform4.addXref(parent);

        InteractorXref parent2 = getMockBuilder().createXref(isoform3, primary.getAc(),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));
        identity2.setPrimaryId("P60953-1");

        getDaoFactory().getXrefDao(InteractorXref.class).persist(parent2);

        isoform3.addXref(parent2);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform3);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform4);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotSpliceVariant variants1 = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
                break;
            }
        }

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variants1));
        primaryIsoforms.add(new ProteinTranscript(isoform2, variants1));
        primaryIsoforms.add(new ProteinTranscript(isoform3, variants1));
        primaryIsoforms.add(new ProteinTranscript(isoform4, variants1));

        Protein chain = getMockBuilder().createProteinChain(primary, "PRO-1", "chain");
        Protein chain2 = getMockBuilder().createProteinChain(primary, "PRO-2", "chain2");
        Protein chain3 = getMockBuilder().createProteinChain(prot, "PRO-3", "chain");
        Protein chain4 = getMockBuilder().createProteinChain(prot, "PRO-4", "chain2");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain3);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain4);

        InteractorXref identity4 = ProteinUtils.getUniprotXref(chain2);
        identity4.setPrimaryId("PRO-1");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity4);

        InteractorXref identity5 = ProteinUtils.getUniprotXref(chain4);
        identity5.setPrimaryId("PRO-1");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity5);

        InteractorXref identity6 = ProteinUtils.getUniprotXref(chain3);
        identity6.setPrimaryId("PRO-1");
        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).update(identity6);

        InteractorXref parent3 = getMockBuilder().createXref(chain3, primary.getAc(),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));
        identity2.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).persist(parent3);

        chain3.addXref(parent3);

        InteractorXref parent4 = getMockBuilder().createXref(chain4, primary.getAc(),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF),
                IntactContext.getCurrentInstance().getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF));
        identity2.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getDaoFactory().getXrefDao(InteractorXref.class).persist(parent4);

        chain4.addXref(parent4);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain2);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain3);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain4);

        Assert.assertEquals(10, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        UniprotFeatureChain chain1 = uniprot.getFeatureChains().iterator().next();

        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));
        primaryChains.add(new ProteinTranscript(chain2, chain1));
        primaryChains.add(new ProteinTranscript(chain3, chain1));
        primaryChains.add(new ProteinTranscript(chain4, chain1));

        // collect
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins, Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, "P60953");

        DuplicatesFoundEvent dupEvt = duplicateFinder.findProteinDuplicates(evt);
        Assert.assertNull(dupEvt);

        Collection<DuplicatesFoundEvent> dupIsoforms = duplicateFinder.findIsoformDuplicates(evt);
        Assert.assertEquals(2, dupIsoforms.size());
        DuplicatesFoundEvent isoEvt1 = dupIsoforms.iterator().next();
        Assert.assertEquals(isoEvt1.getUniprotSequence(), variants1.getSequence());
        Assert.assertEquals(isoEvt1.getUniprotCrc64(), Crc64.getCrc64(variants1.getSequence()));
        Assert.assertEquals(2, isoEvt1.getProteins().size());
        DuplicatesFoundEvent isoEvt2 = dupIsoforms.iterator().next();
        Assert.assertEquals(isoEvt2.getUniprotSequence(), variants1.getSequence());
        Assert.assertEquals(isoEvt2.getUniprotCrc64(), Crc64.getCrc64(variants1.getSequence()));
        Assert.assertEquals(2, isoEvt2.getProteins().size());

        Collection<DuplicatesFoundEvent> dupChains = duplicateFinder.findFeatureChainDuplicates(evt);
        Assert.assertEquals(2, dupChains.size());
        DuplicatesFoundEvent chainEvt1 = dupChains.iterator().next();
        Assert.assertEquals(chainEvt1.getUniprotSequence(), chain1.getSequence());
        Assert.assertEquals(chainEvt1.getUniprotCrc64(), Crc64.getCrc64(chain1.getSequence()));
        Assert.assertEquals(2, chainEvt1.getProteins().size());
        DuplicatesFoundEvent chainEvt2 = dupChains.iterator().next();
        Assert.assertEquals(chainEvt2.getUniprotSequence(), chain1.getSequence());
        Assert.assertEquals(chainEvt2.getUniprotCrc64(), Crc64.getCrc64(chain1.getSequence()));
        Assert.assertEquals(2, chainEvt2.getProteins().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }
}
