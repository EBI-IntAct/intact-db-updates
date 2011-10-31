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
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.DeadUniprotProteinFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinMapperImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinRetrieverImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Second tester of UniprotProteinRetrieverImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>07-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinRetriever2Test extends IntactBasicTestCase {

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
     * Several uniprot entries for the same transcript, several organism and a single one equals to the one of the protein.
     * Can be updated
     */
    public void several_uniprot_transcripts_several_organims_identical_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P21181-1", "several_uniprot_organim_match");
        // one of the protein is human
        prot.getBioSource().setTaxId("9606");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        evt.setUniprotIdentity("P21181-1");

        UniprotProtein uniprot = retriever.retrieveUniprotEntry(evt);

        Assert.assertNotNull(uniprot);
        Assert.assertEquals(MockUniprotProtein.build_CDC42_HUMAN(), uniprot);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One single uniprot entry. Two secondary proteins and two isoforms pass the filter
     */
    public void filter_one_uniprot_protein() throws Exception{

        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P60953", "one_uniprot_organim");
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants1 = null;
        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
            }
            else if (v.getPrimaryAc().equals("P60953-2")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-1", "one_uniprot"); // should not be removed
        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P60953-2", "one_uniprot"); // should not be removed
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, variants1));
        secondaryIsoforms.add(new ProteinTranscript(prot4, variants2));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One secondary protein and one isoform pass the filter because of a single uniprot entry
     * One secondary protein and one isoform doesn't pass the filter : Several uniprot entries, different organims,
     * no organism matching the one of the protein.
     */
    public void filter_several_uniprot_proteins_several_organims_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P21181", "test_several_uniprot"); // should be removed
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants1 = null;
        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
            }
            else if (v.getSecondaryAcs().contains("P00012-2")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-1", "one_uniprot"); // should not be removed
        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P00012-2", "several_uniprot"); // should be removed
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, variants1));
        secondaryIsoforms.add(new ProteinTranscript(prot4, null));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One secondary protein and one isoform pass the filter because of a single uniprot entry
     * One secondary protein and one isoform doesn't pass the filter : Several uniprot entries, one organims,
     * organims different from the one of the protein. Not possible to update
     */
    public void filter_several_uniprot_proteins_one_organim_different_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P00012", "test_several_uniprot_same_organism");
        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants1 = null;
        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
            }
            else if (v.getSecondaryAcs().contains("P00012-1")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-1", "one_uniprot"); // should not be removed
        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P00012-1", "several_uniprot"); // should be removed
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, variants1));
        secondaryIsoforms.add(new ProteinTranscript(prot4, variants2));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One secondary protein and one isoform pass the filter because of a single uniprot entry
     * One secondary protein and one isoform pass the filter : Several uniprot entries, several organisms and
     * one matching the organims of the protein : can be updated
     */
    public void filter_several_uniprot_proteins_several_organims_identical_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

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

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants1 = null;
        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
            }
            else if (v.getSecondaryAcs().contains("P21181-4")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-1", "one_uniprot"); // should not be removed
        prot3.getBioSource().setTaxId("9606");

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P21181-4", "several_uniprot"); // should be removed
        prot4.getBioSource().setTaxId("9606");

        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, variants1));
        secondaryIsoforms.add(new ProteinTranscript(prot4, variants2));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One secondary protein and one isoform pass the filter because of a single uniprot entry
     * One secondary protein and one isoform pass the filter : Several uniprot entries, one organism,
     * organism identical to the one of the protein. Cannot be updated
     */
    public void filter_several_uniprot_proteins_one_organim_same_taxId() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

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

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants1 = null;
        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants1 = v;
            }
            else if (v.getSecondaryAcs().contains("P00012-1")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P60953-1", "one_uniprot"); // should not be removed
        prot3.getBioSource().setTaxId("9606");

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P00012-1", "several_uniprot"); // should be removed
        prot4.getBioSource().setTaxId("9606");

        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, variants1));
        secondaryIsoforms.add(new ProteinTranscript(prot4, variants2));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    /**
     * One secondary protein and one isoform pass the filter because of a single uniprot entry
     * One secondary protein and one isoform pass the filter : No uniprot entries. Can be updated because we can process dead proteins
     *
     */
    public void onSecondaryAcFound_no_uniprot_protein() throws Exception{

        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P12345", "no_uniprot_protein");;

        Protein prot2 = getMockBuilder().createProtein("Q7L8R5", "test_one_uniprot");
        Collection<Protein> secondary = new ArrayList<Protein>();
        secondary.add(prot);
        secondary.add(prot2);
        UniprotProtein cdc = MockUniprotProtein.build_CDC42_HUMAN();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2);

        UniprotSpliceVariant variants2 = null;

        for (UniprotSpliceVariant v : cdc.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variants2 = v;
            }
        }

        Protein prot3 = getMockBuilder().createProteinSpliceVariant(prot, "P12345-1", "no_uniprot");

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot2, "P60953-1", "one_uniprot");

        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot3, null));
        secondaryIsoforms.add(new ProteinTranscript(prot4, variants2));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot3, prot4);

        ProteinUpdateProcessor processor = new ProteinUpdateProcessor();
        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), cdc, Collections.EMPTY_LIST, secondary, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, "P60953");
        Assert.assertEquals(2, evt.getSecondaryProteins().size());
        Assert.assertEquals(2, evt.getSecondaryIsoforms().size());

        retriever.filterAllSecondaryProteinsAndTranscriptsPossibleToUpdate(evt);

        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());

        getDataContext().commitTransaction(status);
    }
}
