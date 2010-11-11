package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinRetriever;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class UniprotProteinRetrieverListenerTest   extends IntactBasicTestCase {

    private class DummyProcessor extends ProteinUpdateProcessor {
        protected void registerListeners() {
        }
    }

    private UniprotProteinRetriever buildProteinListener() {
        UniprotService uniprotService = new MockUniprotService();
        UniprotProteinRetriever listener = new UniprotProteinRetriever(uniprotService);
        return listener;
    }

    @Test
    @DirtiesContext
    /**
     * Several unirpto entries, different organims, no organism matching the one of the protein. Cannot be updated
     */
    public void several_uniprot_proteins_several_organims_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181", "test_several_uniprot");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, one organims, organims different from the one of the protein. Not possible to update
     */
    public void several_uniprot_proteins_one_organim_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P00012");
        Assert.assertNull(evt.getUniprotProtein());

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, several organisms and one matching the organims of the protein : can be updated
     */
    public void several_uniprot_proteins_several_organims_identical_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_HUMAN(), uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, one organism, organism identical to the one of the protein. Cannot be updated
     */
    public void several_uniprot_proteins_one_organim_same_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");
        // protein is human
        prot.getBioSource().setTaxId("9606");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P00012");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * One single uniprot entry. Can be updated
     */
    public void onPreProcess_one_uniprot_protein() throws Exception{
        Protein prot = getMockBuilder().createProtein("P60952", "one_uniprot_organim");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P60952");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_CANFA(), uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * No uniprot entries. Can be updated because we can process dead proteins
     */
    public void no_uniprot_protein() throws Exception{
        Protein prot = getMockBuilder().createProtein("P12345", "no_uniprot_protein");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P12345");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * No uniprot entries. Cannot be updated because we don't process dead proteins
     */
    public void no_uniprot_protein_dont_process_dead_proteins() throws Exception{
        Protein prot = getMockBuilder().createProtein("P12345", "no_uniprot_protein");;
        ProteinUpdateContext.getInstance().getConfig().setProcessProteinNotFoundInUniprot(false);

        ProteinUpdateProcessor processor = new DummyProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P12345");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for a same transcript, several organisms, different from the organism of teh ranscript. Cannot be updated
     */
    public void several_uniprot_transcripts_several_organims_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181-1", "test_several_uniprot_transcript");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181-1");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for a same transcript. One organism, different from the one of the protein. No single master protein with the
     * same uniprot ac (before '-'), cannot be updated
     */
    public void several_uniprot_transcripts_one_organim_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012-1", "test_several_uniprot_transcript");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P00012-1");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNull(uniprot);
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for the same transcript, several organism and a single one equals to the one of the protein.
     * Can be updated
     */
    public void several_uniprot_transcripts_several_organims_identical_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181-1", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181-1");

        UniprotProteinRetriever retriever = buildProteinListener();
        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_HUMAN(), uniprot);
    }

    //@Test
    //@DirtiesContext
    /**
     * Several uniprot entries, different organims, no organism matching the one of the protein. Cannot be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_proteins_several_organims_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181", "test_several_uniprot"); // should be removed
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, one organims, organims different from the one of the protein. Not possible to update
     */
    /*public void onSecondaryAcFound_several_uniprot_proteins_one_organim_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, several organisms and one matching the organims of the protein : can be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_proteins_several_organims_identical_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(2, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries, one organism, organism identical to the one of the protein. Cannot be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_proteins_one_organim_same_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");
        // protein is human
        prot.getBioSource().setTaxId("9606");

        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * One single uniprot entry. Can be updated
     */
    /*public void onSecondaryAcFound_one_uniprot_protein() throws Exception{
        Protein prot = getMockBuilder().createProtein("P60952", "one_uniprot_organim");;
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");

        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(2, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * No uniprot entries. Can be updated because we can process dead proteins
     */
    /*public void onSecondaryAcFound_no_uniprot_protein() throws Exception{
        Protein prot = getMockBuilder().createProtein("P12345", "no_uniprot_protein");;

        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for a same transcript, several organisms, different from the organism of the transcript. Cannot be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_transcripts_several_organims_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181-1", "test_several_uniprot_transcript");

        Protein prot2 = getMockBuilder().createProtein("P60953-1", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953-1"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for a same transcript. One organism, different from the one of the protein. No single master protein with the
     * same uniprot ac (before '-'), cannot be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_transcripts_one_organim_different_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P00012-1", "test_several_uniprot_transcript");

        Protein prot2 = getMockBuilder().createProtein("P60953-1", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953-1"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
    }

    @Test
    @DirtiesContext
    /**
     * Several uniprot entries for the same transcript, several organism and a single one equals to the one of the protein.
     * Can be updated
     */
    /*public void onSecondaryAcFound_several_uniprot_transcripts_several_organims_identical_taxId() throws Exception{
        Protein prot = getMockBuilder().createProtein("P21181-1", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        Protein prot2 = getMockBuilder().createProtein("P60953-1", "test_one_uniprot");
        // one of the protein is human
        prot2.getBioSource().setTaxId("9606");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        ProteinProcessor processor = new DummyProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, null, cdc, Collections.EMPTY_LIST, secondary);
        evt.setUniprotServiceResult(new UniprotServiceResult("P60953-1"));

        UniprotProteinRetriever listener = buildProteinListener();
        listener.filterSecondaryProteinsPossibleToUpdate(evt);

        Assert.assertEquals(2, evt.getSecondaryProteins().size());
    }*/
}
