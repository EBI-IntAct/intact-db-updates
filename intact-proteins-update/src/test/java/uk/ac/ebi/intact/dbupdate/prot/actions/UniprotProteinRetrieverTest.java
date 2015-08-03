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
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.DeadUniprotProteinFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinMapperImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinRetrieverImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

/**
 * Tester of UniprotProteinRetrieverImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinRetrieverTest extends IntactBasicTestCase {
    UniprotProteinRetrieverImpl retriever;

    @Before
    public void before() throws Exception {
        UniprotService uniprotService = new MockUniprotService();
        retriever = new UniprotProteinRetrieverImpl(uniprotService, new UniprotProteinMapperImpl(uniprotService), new DeadUniprotProteinFixerImpl());
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        retriever = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One single uniprot entry. Can be updated
     */
    public void onPreProcess_one_uniprot_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P60952", "one_uniprot_organim");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P60952");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_CANFA(), uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries, different organims, no organism matching the one of the protein.
     */
    public void several_uniprot_proteins_several_organims_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P21181", "test_several_uniprot");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P21181");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries, one organim, organim different from the one of the protein.
     */
    public void several_uniprot_proteins_one_organim_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P00012");
        Assert.assertNull(evt.getUniprotProtein());

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries, several organisms and one matching the organims of the protein. Should return the
     * uniprot entry with the same organism.
     */
    public void several_uniprot_proteins_several_organims_identical_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P21181", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P21181");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_HUMAN(), uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries, one organism, organism identical to the one of the protein.
     */
    public void several_uniprot_proteins_one_organim_same_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");
        // protein is human
        prot.getBioSource().setTaxId("9606");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P00012");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
        getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * No uniprot entries.
     */
    public void no_uniprot_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P12345", "no_uniprot_protein");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);
        evt.setUniprotIdentity("P12345");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries for a same transcript, several organisms, different from the organism of the transcript.
     */
    public void several_uniprot_transcripts_several_organims_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P21181-1", "test_several_uniprot_transcript");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181-1");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Several uniprot entries for a same transcript. One organism, different from the one of the protein. No single master protein with the
     * same uniprot ac (before '-'), cannot be updated
     */
    public void several_uniprot_transcripts_one_organim_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P00012-1", "test_several_uniprot_transcript");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P00012-1");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);

        getDataContext().commitTransaction(status);
    }
}
