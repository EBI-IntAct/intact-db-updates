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
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.DeadUniprotProteinFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinMapperImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

/**
 * Tester of UniprotProteinMapper
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>06-Jan-2011</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinMapperTest extends IntactBasicTestCase {

    private UniprotProteinMapper mapper;

    @Before
    public void setUp(){
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setBlastEnabled(false);
        mapper = new UniprotProteinMapperImpl(config.getUniprotService());
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        mapper = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void remap_dead_protein_protein_successful(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        String sequence = "MSTGRRLAKRSIIGTKVCAKGPDGLWYSGTISDVKTPPSYSGPLSPPPPPTFVVPGEAPI" +
                "NADTRYLVRFDFKTAVESPTATTSSAASTSSTSSTDPSVIVETRRAANVHISPAQALRRS" +
                "AMIKEFRESDLIGPGFRSIMDTELQPGQRVYFTYNGREQSGDVVKHDATKDEVIVKITTV" +
                "GNEEPIELKKRLEEVRLLESRRSARLADQDRDTDFARLADMSGERRRTTTHSIEVPSQLT" +
                "AQHNSRKRPPSDHQDYGNYLETCRAAEILSSMKLQSPHGSMADKCSSPGSSSSASWSSGS" +
                "PSPPLSDDGHAHHSPHNIMSPHDADNARTRTASVSTSDEGIVIDYKEERKKKSKKFRCVY" +
                "RGCVGVVDDLNGVVRHIRKTHLGKDSHRSADDDGNEEDFYLEDADDDVEQVKPTLASEPT" +
                "LSHRDMARPPHEDPEYQKQIVGNFKQGRGGSHYNHLAQSHGRTISGSNIPSTHQQQLQNN" +
                "NTSCIPTSHLAHHNYTCPAATATVGSYSSTGTGSVAASSSASPIGKHARSSSSRPTHSVA" +
                "PYPSPTYVQQQQHHQHTHHHNYAGSSGSSNSSSSSSPVIHSNSSANNMLQQLSQQNVTVT" +
                "AHHSQQQQQLQQQQHHQQQQQHSHQQQQQHLLSSVTITPNFHPAQQQHHHQPMRGHQQQH" +
                "PQTTAGNMVAQNNSNNHSNGSNPLQQQQHMAQQVAVKHTPHSPGKRTRGENKKCRKVYGM" +
                "EKRDQWCTQCRWKKACSRFGD";
        String ac = "Q9VH52";
        Protein protein = getMockBuilder().createProtein(ac, "prot");
        protein.getBioSource().setTaxId( "7227");
        protein.setSequence(sequence);
        protein.getXrefs().clear();

        Annotation noUniprotUpdate = getMockBuilder().createAnnotation(null, null, "no-uniprot-update");
        Annotation caution = getMockBuilder().createAnnotation(DeadUniprotProteinFixerImpl.CAUTION_OBSOLETE, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        protein.addAnnotation(noUniprotUpdate);
        protein.addAnnotation(caution);

        InteractorXref ref = getMockBuilder().createXref(protein, "Pxxxxx", getMockBuilder().createCvObject(CvXrefQualifier.class, null, CvXrefQualifier.UNIPROT_REMOVED_AC), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT));
        protein.addXref(ref);
        getCorePersister().saveOrUpdate(protein);

        Assert.assertTrue(mapper.processProteinRemappingFor(new ProteinEvent(new ProteinUpdateProcessor(), getDataContext(), protein)));
        InteractorXref identity = ProteinUtils.getUniprotXref(protein);
        Assert.assertNotNull(identity);
        Assert.assertEquals(ac, identity.getPrimaryId());
        Assert.assertTrue(protein.getAnnotations().isEmpty());
        Assert.assertEquals(2, protein.getXrefs().size());

        context.commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void dead_protein_protein_remap_not_allowed_and_not_possible(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        String sequence = "MSTGRRLAKRSIIGTKVCAKGPDGLWYSGTISDVKTPPSYSGPLSPPPPPTFVVPGEAPI" +
                "NADTRYLVRFDFKTAVESPTATTSSAASTSSTSSTDPSVIVETRRAANVHISPAQALRRS" +
                "AMIKEFRESDLIGPGFRSIMDTELQPGQRVYFTYNGREQSGDVVKHDATKDEVIVKITTV" +
                "GNEEPIELKKRLEEVRLLESRRSARLADQDRDTDFARLADMSGERRRTTTHSIEVPSQLT" +
                "AQHNSRKRPPSDHQDYGNYLETCRAAEILSSMKLQSPHGSMADKCSSPGSSSSASWSSGS" +
                "PSPPLSDDGHAHHSPHNIMSPHDADNARTRTASVSTSDEGIVIDYKEERKKKSKKFRCVY" +
                "RGCVGVVDDLNGVVRHIRKTHLGKDSHRSADDDGNEEDFYLEDADDDVEQVKPTLASEPT" +
                "LSHRDMARPPHEDPEYQKQIVGNFKQGRGGSHYNHLAQSHGRTISGSNIPSTHQQQLQNN" +
                "NTSCIPTSHLAHHNYTCPAATATVGSYSSTGTGSVAASSSASPIGKHARSSSSRPTHSVA" +
                "PYPSPTYVQQQQHHQHTHHHNYAGSSGSSNSSSSSSPVIHSNSSANNMLQQLSQQNVTVT" +
                "AHHSQQQQQLQQQQHHQQQQQHSHQQQQQHLLSSVTITPNFHPAQQQHHHQPMRGHQQQH" +
                "PQTTAGNMVAQNNSNNHSNGSNPLQQQQHMAQQVAVKHTPHSPGKRTRGENKKCRKVYGM" +
                "EKRDQWCTQCRWKKACSRFGD";
        String ac = "Q9VH52";
        Protein protein = getMockBuilder().createProtein("P12345", "prot");
        protein.getBioSource().setTaxId( "7227");
        protein.setSequence(sequence);

        Annotation noUniprotUpdate = getMockBuilder().createAnnotation(null, null, "no-uniprot-update");
        Annotation caution = getMockBuilder().createAnnotation(DeadUniprotProteinFixerImpl.CAUTION_OBSOLETE, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        protein.addAnnotation(noUniprotUpdate);
        protein.addAnnotation(caution);

        getCorePersister().saveOrUpdate(protein);

        Assert.assertFalse(mapper.processProteinRemappingFor(new ProteinEvent(new ProteinUpdateProcessor(), getDataContext(), protein)));
        InteractorXref identity = ProteinUtils.getUniprotXref(protein);
        Assert.assertNotNull(identity);
        Assert.assertEquals("P12345", identity.getPrimaryId());
        Assert.assertEquals(2, protein.getAnnotations().size());
        Assert.assertEquals(1, protein.getXrefs().size());

        context.commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void dead_protein_protein_remap_not_allowed_but_possible(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        String sequence = "MSTGRRLAKRSIIGTKVCAKGPDGLWYSGTISDVKTPPSYSGPLSPPPPPTFVVPGEAPI" +
                "NADTRYLVRFDFKTAVESPTATTSSAASTSSTSSTDPSVIVETRRAANVHISPAQALRRS" +
                "AMIKEFRESDLIGPGFRSIMDTELQPGQRVYFTYNGREQSGDVVKHDATKDEVIVKITTV" +
                "GNEEPIELKKRLEEVRLLESRRSARLADQDRDTDFARLADMSGERRRTTTHSIEVPSQLT" +
                "AQHNSRKRPPSDHQDYGNYLETCRAAEILSSMKLQSPHGSMADKCSSPGSSSSASWSSGS" +
                "PSPPLSDDGHAHHSPHNIMSPHDADNARTRTASVSTSDEGIVIDYKEERKKKSKKFRCVY" +
                "RGCVGVVDDLNGVVRHIRKTHLGKDSHRSADDDGNEEDFYLEDADDDVEQVKPTLASEPT" +
                "LSHRDMARPPHEDPEYQKQIVGNFKQGRGGSHYNHLAQSHGRTISGSNIPSTHQQQLQNN" +
                "NTSCIPTSHLAHHNYTCPAATATVGSYSSTGTGSVAASSSASPIGKHARSSSSRPTHSVA" +
                "PYPSPTYVQQQQHHQHTHHHNYAGSSGSSNSSSSSSPVIHSNSSANNMLQQLSQQNVTVT" +
                "AHHSQQQQQLQQQQHHQQQQQHSHQQQQQHLLSSVTITPNFHPAQQQHHHQPMRGHQQQH" +
                "PQTTAGNMVAQNNSNNHSNGSNPLQQQQHMAQQVAVKHTPHSPGKRTRGENKKCRKVYGM" +
                "EKRDQWCTQCRWKKACSRFGD";
        String ac = "Q9VH52";
        Protein protein = getMockBuilder().createProtein(ac, "prot");
        protein.getBioSource().setTaxId( "7227");
        protein.setSequence(sequence);
        protein.getXrefs().clear();

        Annotation noUniprotUpdate = getMockBuilder().createAnnotation(null, null, "no-uniprot-update");
        Annotation caution = getMockBuilder().createAnnotation(DeadUniprotProteinFixerImpl.CAUTION_OBSOLETE, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        protein.addAnnotation(noUniprotUpdate);
        protein.addAnnotation(caution);

        InteractorXref ref = getMockBuilder().createXref(protein, "Pxxxxx", getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.SEE_ALSO_MI_REF, CvXrefQualifier.SEE_ALSO), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT));
        protein.addXref(ref);
        getCorePersister().saveOrUpdate(protein);

        Assert.assertFalse(mapper.processProteinRemappingFor(new ProteinEvent(new ProteinUpdateProcessor(), getDataContext(), protein)));
        InteractorXref identity = ProteinUtils.getUniprotXref(protein);
        Assert.assertNull(identity);
        Assert.assertEquals(2, protein.getAnnotations().size());
        Assert.assertEquals(1, protein.getXrefs().size());

        context.commitTransaction(status);
    }
}
