package uk.ac.ebi.intact.update.model.protein.listener;

import org.junit.*;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.report.FileReportHandler;
import uk.ac.ebi.intact.dbupdate.prot.report.UpdateReportHandler;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.update.IntactUpdateContext;
import uk.ac.ebi.intact.update.model.protein.errors.*;
import uk.ac.ebi.intact.update.model.protein.events.*;
import uk.ac.ebi.intact.update.model.protein.feature.FeatureUpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentReportsFactory;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentResultsFactory;
import uk.ac.ebi.intact.update.model.protein.range.PersistentInvalidRange;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.persistence.dao.UpdateDaoFactory;
import uk.ac.ebi.intact.update.persistence.dao.protein.*;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * Tester of eventPersisterListener (integration tests)
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/08/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/db-update-test.spring.xml"})
public class EventPersisterListenerTest extends IntactBasicTestCase{

    ProteinUpdateProcessor processor;

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
    @Ignore
    public void simulation_out_of_date_participants() throws Exception {

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

        // one secondary protein
        Protein secondary = getMockBuilder().createProtein("O34373", "secondary_protein");
        secondary.getBioSource().setTaxId("224308");
        getCorePersister().saveOrUpdate(secondary);

        Interaction interaction_6 = getMockBuilder().createInteraction(simple, secondary);
        for (Component c : interaction_6.getComponents()){
            c.getBindingDomains().clear();
        }

        // add a range which is invalid
        Feature feature4 = getMockBuilder().createFeatureRandom();
        feature4.getRanges().clear();
        Range range4 = getMockBuilder().createRange(11, 11, 8, 8);
        feature4.addRange(range4);
        secondary.getActiveInstances().iterator().next().addBindingDomain(feature4);

        getCorePersister().saveOrUpdate(secondary, interaction_6);

        // duplicates

        // dupe1 has an invalid range and will be deprecated
        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));
        dupe1.setSequence("LALASSWWAHVEMGPPDPILGVTEAYKRDTNSKK"); // real sequence, insertion of "LALA" upstream

        CvDatabase dip = getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.DIP_MI_REF, CvDatabase.DIP);
        dupe1.addXref(getMockBuilder().createXref(dupe1, "DIP:00001", null, dip));

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe2 = cloner.clone(dupe1);
        dupe2.setShortLabel("dupe2");
        dupe2.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));

        for (Iterator<InteractorXref> xrefIter = dupe2.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12351");
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xrefIter.remove();
            }
        }

        // dupe2 will be kept as original but has the same interaction as dupe3
        dupe2.setCreated(new Date(1)); // dupe2 is older
        // no need to update the sequence
        dupe2.setSequence("SSWWAHVEMGPPDPILGVTEAYKRDTNSKK");
        dupe2.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));

        // dupe 3 has a range which will be shifted before the merge and has an interaction which will be deleted because duplicated participant
        Protein dupe3 = cloner.clone(dupe2);
        dupe3.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));
        dupe3.setShortLabel("dupe3");
        dupe3.setCreated(dupe1.getCreated());

        for (Iterator<InteractorXref> xrefIter = dupe3.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12352");
            }
        }

        // no need to update the sequence
        dupe3.setSequence("SSWWAHVPPDPILGVTEAYKRDTNSKK");

        getCorePersister().saveOrUpdate(dupe1);
        getCorePersister().saveOrUpdate(dupe2);
        getCorePersister().saveOrUpdate(dupe3);

        String dupe1Ac = dupe1.getAc();

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, simple);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe2, simple);
        Interaction interaction4 = getMockBuilder().createInteraction(dupe3, simple);

        for (Component c : dupe2.getActiveInstances()){
            c.getBindingDomains().clear();
        }
        // add a range in the protein with up-to-date sequence (dupe2)
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        Range range = getMockBuilder().createRange(5, 5, 7, 7);
        feature.addRange(range);
        dupe2.getActiveInstances().iterator().next().addBindingDomain(feature);

        for (Component c : dupe3.getActiveInstances()){
            c.getBindingDomains().clear();
        }
        // add a range which will be shifted (dupe3)
        Feature feature3 = getMockBuilder().createFeatureRandom();
        feature3.getRanges().clear();
        Range range3 = getMockBuilder().createRange(8, 8, 11, 11);
        feature3.addRange(range3);
        dupe3.getActiveInstances().iterator().next().addBindingDomain(feature3);

        // add a bad range in the protein with sequence to update (dupe1). Add another component which will be a duplicated component
        Feature feature2 = getMockBuilder().createFeatureRandom();
        feature2.getRanges().clear();
        Range range2 = getMockBuilder().createRange(1, 1, 4, 4);
        range2.setFullSequence("LALA");
        feature2.addRange(range2);

        for (Component c : dupe1.getActiveInstances()){
            c.getBindingDomains().clear();

            if (dupe1.getAc().equals(c.getInteractor().getAc())){
                c.addFeature(feature2);
            }
        }

        // persist the interactions
        getCorePersister().saveOrUpdate(interaction1, interaction2, interaction4);

        // interaction with duplicated component
        Interaction interaction3 = getMockBuilder().createInteraction(dupe2, simple, dupe3);
        Component deletedComponent = null;
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(dupe3.getAc())){
                deletedComponent = c;
            }
        }

        // persist the interactions
        getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(5, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(11, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Protein dupe3Refreshed = getDaoFactory().getProteinDao().getByAc(dupe3.getAc());
        InteractorXref uniprotXref2 = ProteinUtils.getIdentityXrefs(dupe3Refreshed).iterator().next();
        uniprotXref2.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref2);

        getDataContext().commitTransaction(status);


        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        // add the persister listener
        EventPersisterListener persisterListener = (EventPersisterListener) IntactUpdateContext.getCurrentInstance().getProteinPersisterListener();
        persisterListener.createUpdateProcess();
        protUpdateProcessor.addListener(persisterListener);
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();
        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(5, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(10, getDaoFactory().getComponentDao().countAll());
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 2 out of date participants : secondary protein with invalid range and one of the duplicated protein having an out of date range
        OutOfDateParticipantEventDao outOfDatePartDao = updateFactory.getOutOfDateParticipantEventDao();
        List<OutOfDateParticipantEvent> outOfDatePartevts = outOfDatePartDao.getAll();

        Assert.assertEquals(2, outOfDatePartevts.size());

        for (OutOfDateParticipantEvent partEvt : outOfDatePartevts){
            Assert.assertNull(partEvt.getMessage());
            Assert.assertNull(partEvt.getRemappedProtein());

            if (partEvt.getProteinAc().equals(secondary.getAc())){
                Assert.assertEquals("P26904", partEvt.getUniprotAc());
                // parent = current protein because we can remap to one of its transcripts
                Assert.assertEquals(secondary.getAc(), partEvt.getRemappedParent());

                Assert.assertEquals(2, partEvt.getUpdatedFeatureAnnotations().size());

                Collection<PersistentInvalidRange> invalidRanges = partEvt.getInvalidRanges();
                Assert.assertEquals(1, invalidRanges.size());

                PersistentInvalidRange invalidRange = invalidRanges.iterator().next();
                Assert.assertEquals("11-8", invalidRange.getOldPositions());
                Assert.assertEquals(null, invalidRange.getNewPositions());
                Assert.assertEquals(null, invalidRange.getOldSequence());
                Assert.assertEquals(null, invalidRange.getNewSequence());
                Assert.assertEquals(range4.getAc(), invalidRange.getRangeAc());
                Assert.assertEquals(feature4.getAc(), invalidRange.getFeatureAc());
                Assert.assertEquals(feature4.getComponent().getAc(), invalidRange.getComponentAc());
                Assert.assertEquals(feature4.getComponent().getInteraction().getAc(), invalidRange.getInteractionAc());

                Assert.assertEquals(-1, invalidRange.getSequenceVersion());
                Assert.assertEquals("range", invalidRange.getFromStatus());
                Assert.assertEquals("range", invalidRange.getToStatus());
                Assert.assertNotNull(invalidRange.getErrorMessage());
            }
            else if (partEvt.getProteinAc().equals(dupe1Ac)){
                Assert.assertEquals("P12345", partEvt.getUniprotAc());
                // parent = original protein
                Assert.assertEquals(dupe2.getAc(), partEvt.getRemappedParent());

                // the protein was not deleted so the range is intact
                Assert.assertEquals(0, partEvt.getUpdatedFeatureAnnotations().size());

                Collection<PersistentInvalidRange> invalidRanges = partEvt.getInvalidRanges();
                Assert.assertEquals(1, invalidRanges.size());

                PersistentInvalidRange invalidRange = invalidRanges.iterator().next();
                Assert.assertEquals("1..1-4..4", invalidRange.getOldPositions());
                Assert.assertEquals("0-0", invalidRange.getNewPositions());
                Assert.assertEquals("LALA", invalidRange.getOldSequence());
                Assert.assertEquals(null, invalidRange.getNewSequence());
                Assert.assertEquals(range2.getAc(), invalidRange.getRangeAc());
                Assert.assertEquals(feature2.getAc(), invalidRange.getFeatureAc());
                Assert.assertEquals(feature2.getComponent().getAc(), invalidRange.getComponentAc());
                Assert.assertEquals(feature2.getComponent().getInteraction().getAc(), invalidRange.getInteractionAc());

                Assert.assertEquals(-1, invalidRange.getSequenceVersion());
                Assert.assertEquals("range", invalidRange.getFromStatus());
                Assert.assertEquals("range", invalidRange.getToStatus());
                Assert.assertNotNull(invalidRange.getErrorMessage());
            }
            else {
                // this case is not expected
                Assert.assertFalse(true);
            }
        }

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    @Ignore
    public void simulation_update() throws Exception {

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

        // one intact transcript without parent with an interaction
        Protein transcriptWithoutParent = getMockBuilder().createProtein("P18459-2", "isoform_no_parent");
        transcriptWithoutParent.setBioSource(getMockBuilder().createBioSource(7227, "drome"));
        getCorePersister().saveOrUpdate(transcriptWithoutParent);

        Interaction interaction_5 = getMockBuilder().createInteraction(simple, transcriptWithoutParent);
        for (Component c : interaction_5.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_5);

        // one secondary protein
        Protein secondary = getMockBuilder().createProtein("O34373", "secondary_protein");
        secondary.getBioSource().setTaxId("224308");
        getCorePersister().saveOrUpdate(secondary);

        Interaction interaction_6 = getMockBuilder().createInteraction(simple, secondary);
        for (Component c : interaction_6.getComponents()){
            c.getBindingDomains().clear();
        }

        // add a range which is invalid
        Feature feature4 = getMockBuilder().createFeatureRandom();
        feature4.getRanges().clear();
        Range range4 = getMockBuilder().createRange(11, 11, 8, 8);
        feature4.addRange(range4);
        secondary.getActiveInstances().iterator().next().addBindingDomain(feature4);

        getCorePersister().saveOrUpdate(secondary, interaction_6);

        // one protein with bad organism
        Protein bad_organism = getMockBuilder().createProtein("P12349", "bad_organism");
        bad_organism.getBioSource().setTaxId("9606");    // is 7244 in uniprot
        getCorePersister().saveOrUpdate(bad_organism);

        Interaction interaction_7 = getMockBuilder().createInteraction(simple, bad_organism);
        for (Component c : interaction_7.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_7);

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

        getDataContext().commitTransaction(status);


        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        // add the persister listener
        EventPersisterListener persisterListener = (EventPersisterListener) IntactUpdateContext.getCurrentInstance().getProteinPersisterListener();
        persisterListener.createUpdateProcess();
        protUpdateProcessor.addListener(persisterListener);
        protUpdateProcessor.updateAll();

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 6 updated proteins : simple_protein, transcript without parent and its parent, secondary protein, one protein with a splice variant which doesn't exist in uniprot and duplicated prot
        UniprotUpdateEventDao updateEventDao = updateFactory.getUniprotUpdateEventDao();
        List<UniprotUpdateEvent> updateEvents = updateEventDao.getAll();

        Assert.assertEquals(5, updateEvents.size());

        for (UniprotUpdateEvent updateEvt : updateEvents){
            Assert.assertNull(updateEvt.getMessage());
            Assert.assertNotNull(updateEvt.getUpdatedFullName());
            Assert.assertTrue(updateEvt.getUpdatedAnnotations().isEmpty());
            Assert.assertFalse(updateEvt.getUpdatedAliases().isEmpty());
            Assert.assertTrue(updateEvt.getUpdatedFeatureAnnotations().isEmpty());
            Assert.assertTrue(updateEvt.getUpdatedRanges().isEmpty());

            if (updateEvt.getProteinAc().equals(simple.getAc())){
                Assert.assertNotNull(updateEvt.getUpdatedShortLabel());
                Assert.assertEquals("P36872", updateEvt.getUniprotAc());
                Assert.assertEquals("P36872", updateEvt.getUniprotQuery());
                Assert.assertFalse(updateEvt.getUpdatedXrefs().isEmpty());
            }
            else if (updateEvt.getProteinAc().equals(transcriptWithoutParent.getAc())){
                Assert.assertNotNull(updateEvt.getUpdatedShortLabel());
                Assert.assertEquals("P18459-2", updateEvt.getUniprotAc());
                Assert.assertEquals("P18459-2", updateEvt.getUniprotQuery());
                Assert.assertTrue(updateEvt.getUpdatedXrefs().isEmpty());
            }
            else if (updateEvt.getUniprotAc().equals("P18459")){
                Assert.assertNull(updateEvt.getUpdatedShortLabel());
                Assert.assertEquals("P18459", updateEvt.getUniprotAc());
                Assert.assertEquals("P18459-2", updateEvt.getUniprotQuery());
                Assert.assertFalse(updateEvt.getUpdatedXrefs().isEmpty());
            }
            else if (updateEvt.getProteinAc().equals(secondary.getAc())){
                Assert.assertNotNull(updateEvt.getUpdatedShortLabel());
                Assert.assertEquals("P26904", updateEvt.getUniprotAc());
                Assert.assertEquals("O34373", updateEvt.getUniprotQuery());
                Assert.assertFalse(updateEvt.getUpdatedXrefs().isEmpty());
            }
            else if (updateEvt.getProteinAc().equals(parent.getAc())){
                Assert.assertNotNull(updateEvt.getUpdatedShortLabel());
                Assert.assertEquals("P12350", updateEvt.getUniprotAc());
                Assert.assertEquals("P12350", updateEvt.getUniprotQuery());
                Assert.assertFalse(updateEvt.getUpdatedXrefs().isEmpty());
            }
            else {
                // error if another protein is deleted
                Assert.assertFalse(true);
            }
        }

        // 1 range updated while fixing duplicated proteins
        Assert.assertEquals(0, updateFactory.getUpdatedRangeDao(PersistentUpdatedRange.class).countAll());

        // 2 ranges which are invalids (one invalid and one out of date)
        Assert.assertEquals(1, updateFactory.getUpdatedRangeDao(PersistentInvalidRange.class).countAll());

        // 2 annotations added to the same feature for invalid range
        Assert.assertEquals(2, updateFactory.getUpdatedAnnotationDao(FeatureUpdatedAnnotation.class).countAll());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    @Ignore
    public void simulation_secondary_protein() throws Exception {

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

        // one secondary protein
        Protein secondary = getMockBuilder().createProtein("O34373", "secondary_protein");
        secondary.getBioSource().setTaxId("224308");
        getCorePersister().saveOrUpdate(secondary);

        Interaction interaction_6 = getMockBuilder().createInteraction(simple, secondary);

        getCorePersister().saveOrUpdate(interaction_6);

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

        // 1 secondary protein
        SecondaryProteinEventDao secondaryEvtDao = updateFactory.getSecondaryProteinEventDao();
        List<SecondaryProteinEvent> secondaryEvts = secondaryEvtDao.getAll();

        Assert.assertEquals(1, secondaryEvts.size());

        SecondaryProteinEvent secEvt = secondaryEvts.iterator().next();
        Assert.assertEquals(secondary.getAc(), secEvt.getProteinAc());
        Assert.assertEquals("P26904", secEvt.getUniprotAc());
        Assert.assertEquals("O34373", secEvt.getSecondaryUniprotAc());
        Assert.assertNull(secEvt.getMessage());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    @Ignore
    public void simulation_protein_without_parent() throws Exception {

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

        // one intact transcript without parent with an interaction
        Protein transcriptWithoutParent = getMockBuilder().createProtein("P18459-2", "isoform_no_parent");
        transcriptWithoutParent.setBioSource(getMockBuilder().createBioSource(7227, "drome"));
        getCorePersister().saveOrUpdate(transcriptWithoutParent);

        Interaction interaction_5 = getMockBuilder().createInteraction(simple, transcriptWithoutParent);
        for (Component c : interaction_5.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_5);

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
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getComponentDao().countAll());
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 1 protein without parent
        IntactTranscriptUpdateEventDao transUpDao = updateFactory.getIntactTranscriptEventDao();
        List<IntactTranscriptUpdateEvent> transUpEvts = transUpDao.getAll();

        Assert.assertEquals(1, transUpEvts.size());

        IntactTranscriptUpdateEvent transUpEvt = transUpEvts.iterator().next();
        Assert.assertEquals(transcriptWithoutParent.getAc(), transUpEvt.getProteinAc());
        Assert.assertEquals("P18459-2", transUpEvt.getUniprotAc());
        Assert.assertNull(transUpEvt.getOldParentAc());
        Assert.assertNotNull(transUpEvt.getNewParentAc());
        Assert.assertNull(transUpEvt.getMessage());

        // 1 created protein because one transcript without parents
        CreatedProteinEventDao createdProteinEvent = updateFactory.getCreatedProteinEventDao();
        List<CreatedProteinEvent> createdEvents = createdProteinEvent.getAll();

        Assert.assertEquals(1, createdEvents.size());

        CreatedProteinEvent evt = createdEvents.iterator().next();
        Assert.assertEquals("P18459", evt.getUniprotAc());
        Assert.assertEquals("No Intact master protein existed (or was valid) for the uniprot ac P18459", evt.getMessage());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    @Ignore
    public void simulation_organism_conflicts() throws Exception {

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

        // one protein with bad organism
        Protein bad_organism = getMockBuilder().createProtein("P12349", "bad_organism");
        bad_organism.getBioSource().setTaxId("9606");    // is 7244 in uniprot
        getCorePersister().saveOrUpdate(bad_organism);

        Interaction interaction_7 = getMockBuilder().createInteraction(simple, bad_organism);
        for (Component c : interaction_7.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_7);

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

        // one protein with organism conflicts
        Collection<OrganismConflict> organismConflicts = updateFactory.getProteinUpdateErrorDao(OrganismConflict.class).getAll();

        Assert.assertEquals(1, organismConflicts.size());

        OrganismConflict orgConf = organismConflicts.iterator().next();
        Assert.assertEquals(bad_organism.getAc(), orgConf.getProteinAc());
        Assert.assertEquals("P12349", orgConf.getUniprotAc());
        Assert.assertEquals("9606", orgConf.getIntactTaxId());
        Assert.assertEquals("7244", orgConf.getUniprotTaxId());
        Assert.assertNull(orgConf.getReason());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }
}