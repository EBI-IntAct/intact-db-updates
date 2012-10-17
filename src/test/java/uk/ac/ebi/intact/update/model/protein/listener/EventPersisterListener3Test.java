package uk.ac.ebi.intact.update.model.protein.listener;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.report.FileReportHandler;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.update.IntactUpdateContext;
import uk.ac.ebi.intact.update.model.protein.errors.MultiUniprotIdentities;
import uk.ac.ebi.intact.update.model.protein.errors.NonExistingProteinTranscript;
import uk.ac.ebi.intact.update.model.protein.errors.PersistentUpdateErrorFactory;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentReportsFactory;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentResultsFactory;
import uk.ac.ebi.intact.update.persistence.dao.UpdateDaoFactory;

import java.io.File;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/10/12</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/db-update-test.spring.xml"})
public class EventPersisterListener3Test extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    @Transactional(propagation = Propagation.NEVER)
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setBlastEnabled(false);
        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        processor = new ProteinUpdateProcessor();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        processor = null;
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    public void simulation_multiple_identities() throws Exception {

        TransactionStatus status = getDataContext().beginTransaction();

        final File dir = new File("target/simulation");
        UpdateReportHandler reportHandler = new FileReportHandler(dir);
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setReportHandler(reportHandler);
        config.setGlobalProteinUpdate(true);

        // create the proper persistent factories
        config.setProteinMappingReportFactory(new PersistentReportsFactory());
        config.setProteinMappingResultsFactory(new PersistentResultsFactory());
        config.setErrorFactory(new PersistentUpdateErrorFactory());

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

        // one protein for all interactions. This protein has the same sequence as one isoform which is not the canonical sequence
        Protein simple = getMockBuilder().createProtein("P36872", "simple_protein");
        simple.getBioSource().setTaxId("7227");
        simple.setSequence("MAGNGEASWCFSQIKGALDDDVTDADIISCVEFNHDGELLATGDKGGRVVIFQRDPASKA" +
                "ANPRRGEYNVYSTFQSHEPEFDYLKSLEIEEKINKIRWLQQKNPVHFLLSTNDKTVKLWK" +
                "VSERDKSFGGYNTKEENGLIRDPQNVTALRVPSVKQIPLLVEASPRRTFANAHTYHINSI" +
                "SVNSDQETFLSADDLRINLWHLEVVNQSYNIVDIKPTNMEELTEVITAAEFHPTECNVFV" +
                "YSSSKGTIRLCDMRSAALCDRHSKQFEEPENPTNRSFFSEIISSISDVKLSNSGRYMISR" +
                "DYLSIKVWDLHMETKPIETYPVHEYLRAKLCSLYENDCIFDKFECCWNGKDSSIMTGSYN" +
                "NFFRVFDRNSKKDVTLEASRDIIKPKTVLKPRKVCTGGKRKKDEISVDCLDFNKKILHTA" +
                "WHPEENIIAVAATNNLFIFQDKF") ;
        getCorePersister().saveOrUpdate(simple);

        // one protein with several uniprot identities and with an interaction
        Protein severalUniProtIdentity = getMockBuilder().createProtein("P12343", "several_uniprot_identities");
        InteractorXref identity2 = getMockBuilder().createXref(severalUniProtIdentity, "P12344",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY),
                getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT));
        severalUniProtIdentity.addXref(identity2);
        getCorePersister().saveOrUpdate(severalUniProtIdentity);

        Interaction interaction_3 = getMockBuilder().createInteraction(simple, severalUniProtIdentity);
        for (Component c : interaction_3.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_3);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getComponentDao().countAll());

        getDataContext().commitTransaction(status);


        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        // add the persister listener
        EventPersisterListener persisterListener = (EventPersisterListener) IntactUpdateContext.getCurrentInstance().getProteinPersisterListener();
        persisterListener.createUpdateProcess();
        protUpdateProcessor.addListener(persisterListener);
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getComponentDao().countAll());
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // one protein with multiple identities
        Collection<MultiUniprotIdentities> multiUniprotIdentities = updateFactory.getProteinUpdateErrorDao(MultiUniprotIdentities.class).getAll();

        Assert.assertEquals(1, multiUniprotIdentities.size());

        MultiUniprotIdentities multiIdent = multiUniprotIdentities.iterator().next();
        Assert.assertEquals(severalUniProtIdentity.getAc(), multiIdent.getProteinAc());
        Assert.assertEquals(2, multiIdent.getUniprotIdentities().size());
        Assert.assertNull(multiIdent.getReason());

        for (String uniprot : multiIdent.getUniprotIdentities()){
            // case which should not happen
            if (!uniprot.equals("P12343") && !uniprot.equals("P12344")){
                Assert.assertTrue(false);
            }
        }

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    public void simulation_non_existing_protein_transcript() throws Exception {

        TransactionStatus status = getDataContext().beginTransaction();

        final File dir = new File("target/simulation");
        UpdateReportHandler reportHandler = new FileReportHandler(dir);
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setReportHandler(reportHandler);
        config.setGlobalProteinUpdate(true);

        // create the proper persistent factories
        config.setProteinMappingReportFactory(new PersistentReportsFactory());
        config.setProteinMappingResultsFactory(new PersistentResultsFactory());
        config.setErrorFactory(new PersistentUpdateErrorFactory());

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

        // one protein for all interactions. This protein has the same sequence as one isoform which is not the canonical sequence
        Protein simple = getMockBuilder().createProtein("P36872", "simple_protein");
        simple.getBioSource().setTaxId("7227");
        simple.setSequence("MAGNGEASWCFSQIKGALDDDVTDADIISCVEFNHDGELLATGDKGGRVVIFQRDPASKA" +
                "ANPRRGEYNVYSTFQSHEPEFDYLKSLEIEEKINKIRWLQQKNPVHFLLSTNDKTVKLWK" +
                "VSERDKSFGGYNTKEENGLIRDPQNVTALRVPSVKQIPLLVEASPRRTFANAHTYHINSI" +
                "SVNSDQETFLSADDLRINLWHLEVVNQSYNIVDIKPTNMEELTEVITAAEFHPTECNVFV" +
                "YSSSKGTIRLCDMRSAALCDRHSKQFEEPENPTNRSFFSEIISSISDVKLSNSGRYMISR" +
                "DYLSIKVWDLHMETKPIETYPVHEYLRAKLCSLYENDCIFDKFECCWNGKDSSIMTGSYN" +
                "NFFRVFDRNSKKDVTLEASRDIIKPKTVLKPRKVCTGGKRKKDEISVDCLDFNKKILHTA" +
                "WHPEENIIAVAATNNLFIFQDKF") ;
        getCorePersister().saveOrUpdate(simple);

        // one protein with a splice variant which doesn't exist in uniprot but contains one interaction
        Protein parent = getMockBuilder().createProtein("P12350", "not_existing_splice_variant");
        parent.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Protein isoform_no_uniprot = getMockBuilder().createProteinSpliceVariant(parent, "P12350-2", "not_existing_splice_variant");
        isoform_no_uniprot.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Interaction interaction_8 = getMockBuilder().createInteraction(simple, isoform_no_uniprot);
        for (Component c : interaction_8.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_8);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getComponentDao().countAll());

        getDataContext().commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        // add the persister listener
        EventPersisterListener persisterListener = (EventPersisterListener) IntactUpdateContext.getCurrentInstance().getProteinPersisterListener();
        persisterListener.createUpdateProcess();
        protUpdateProcessor.addListener(persisterListener);
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getComponentDao().countAll());
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // one protein transcript which does not exist in uniprot
        Collection<NonExistingProteinTranscript> nonExTranscripts = updateFactory.getProteinUpdateErrorDao(NonExistingProteinTranscript.class).getAll();

        Assert.assertEquals(1, nonExTranscripts.size());

        NonExistingProteinTranscript nonExTrans = nonExTranscripts.iterator().next();
        Assert.assertEquals(isoform_no_uniprot.getAc(), nonExTrans.getProteinAc());
        Assert.assertEquals("P12350-2", nonExTrans.getDeadUniprot());
        Assert.assertEquals("P12350", nonExTrans.getMasterUniprotAc());
        Assert.assertEquals(parent.getAc(), nonExTrans.getMasterIntactAc());
        Assert.assertNull(nonExTrans.getReason());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }
}
