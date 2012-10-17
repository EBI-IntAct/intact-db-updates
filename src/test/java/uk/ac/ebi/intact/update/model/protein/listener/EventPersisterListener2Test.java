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
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.update.IntactUpdateContext;
import uk.ac.ebi.intact.update.model.protein.errors.ImpossibleMerge;
import uk.ac.ebi.intact.update.model.protein.errors.MultiUniprotIdentities;
import uk.ac.ebi.intact.update.model.protein.errors.NonExistingProteinTranscript;
import uk.ac.ebi.intact.update.model.protein.errors.PersistentUpdateErrorFactory;
import uk.ac.ebi.intact.update.model.protein.events.*;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentReportsFactory;
import uk.ac.ebi.intact.update.model.protein.mapping.factories.PersistentResultsFactory;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.persistence.dao.UpdateDaoFactory;
import uk.ac.ebi.intact.update.persistence.dao.protein.*;

import java.io.File;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17/10/12</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/db-update-test.spring.xml"})
public class EventPersisterListener2Test extends IntactBasicTestCase {

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
    public void simulation_duplicates() throws Exception {

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

        Assert.assertEquals(1, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Protein dupe3Refreshed = getDaoFactory().getProteinDao().getByAc(dupe3.getAc());
        InteractorXref uniprotXref2 = ProteinUtils.getIdentityXrefs(dupe3Refreshed).iterator().next();
        uniprotXref2.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());
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
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe3.getAc()));
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 3 duplicated proteins
        DuplicatedProteinEventDao duplicatedEventDao = updateFactory.getDuplicatedProteinEventDao();
        List<DuplicatedProteinEvent> duplicatedEvents = duplicatedEventDao.getAll();

        Assert.assertEquals(3, duplicatedEvents.size());

        for (DuplicatedProteinEvent evt : duplicatedEvents){
            Assert.assertEquals(dupe2.getAc(), evt.getOriginalProtein());
            Assert.assertEquals("P12345", evt.getUniprotAc());
            Assert.assertNull(evt.getMessage());

            if (evt.getProteinAc().equals(dupe1.getAc())){
                Assert.assertFalse(evt.isMergeSuccessful());
                Assert.assertEquals(0, evt.getUpdatedRanges().size());
                Assert.assertEquals(0, evt.getUpdatedFeatureAnnotations().size());
                // one interaction with invalid ranges so no interactions have been moved successfully
                Assert.assertEquals(0, evt.getMovedInteractions().size());
                Assert.assertEquals(0, evt.getMovedXrefs().size());
            }
            else if (evt.getProteinAc().equals(dupe2.getAc())){
                Assert.assertTrue(evt.isMergeSuccessful());
                Assert.assertEquals(0, evt.getUpdatedRanges().size());
                Assert.assertEquals(0, evt.getUpdatedFeatureAnnotations().size());
                Assert.assertEquals(0, evt.getMovedInteractions().size());
                Assert.assertEquals(0, evt.getMovedXrefs().size());
            }
            else if (evt.getProteinAc().equals(dupe3.getAc())){
                Assert.assertTrue(evt.isMergeSuccessful());
                // one range successfully updated before merging
                Assert.assertEquals(1, evt.getUpdatedRanges().size());
                PersistentUpdatedRange updatedRange = evt.getUpdatedRanges().iterator().next();
                Assert.assertEquals("8..8-11..11", updatedRange.getOldPositions());
                Assert.assertEquals("11..11-14..14", updatedRange.getNewPositions());
                Assert.assertEquals(null, updatedRange.getOldSequence());
                Assert.assertEquals("PPDP", updatedRange.getNewSequence());
                Assert.assertEquals(range3.getAc(), updatedRange.getRangeAc());
                Assert.assertEquals(feature3.getAc(), updatedRange.getFeatureAc());
                Assert.assertEquals(feature3.getComponent().getAc(), updatedRange.getComponentAc());
                Assert.assertEquals(feature3.getComponent().getInteraction().getAc(), updatedRange.getInteractionAc());

                Assert.assertEquals(0, evt.getUpdatedFeatureAnnotations().size());
                // the moved interaction plus the deleted one so only one moved interaction
                Assert.assertEquals(1, evt.getMovedInteractions().size());
                Assert.assertEquals(interaction4.getAc(), evt.getMovedInteractions().iterator().next());

                Assert.assertEquals(0, evt.getMovedXrefs().size());
            }
            else {
                // this case is not expected
                Assert.assertFalse(true);
            }
        }

        // one impossible merge because of range conflicts (dupe1)
        Collection<ImpossibleMerge> impossibleMerges = updateFactory.getProteinUpdateErrorDao(ImpossibleMerge.class).getAll();

        Assert.assertEquals(1, impossibleMerges.size());

        ImpossibleMerge impMerge = impossibleMerges.iterator().next();
        Assert.assertEquals(dupe1.getAc(), impMerge.getProteinAc());
        Assert.assertEquals(dupe2.getAc(), impMerge.getOriginalProtein());
        Assert.assertEquals("P12345", impMerge.getUniprotAc());
        Assert.assertNotNull(impMerge.getReason());

        // 1 deleted participant
        DeletedComponentEventDao deletedComponentDao = updateFactory.getDeletedComponentEventDao();
        List<DeletedComponentEvent> deletedComponents = deletedComponentDao.getAll();

        Assert.assertEquals(1, deletedComponents.size());

        DeletedComponentEvent deletedComp = deletedComponents.iterator().next();
        Assert.assertEquals(dupe3.getAc(), deletedComp.getProteinAc());
        Assert.assertEquals("P12345", deletedComp.getUniprotAc());
        Assert.assertEquals(1, deletedComp.getDeletedComponents().size());
        Assert.assertEquals(deletedComponent.getAc(), deletedComp.getDeletedComponents().iterator().next());
        Assert.assertNull(deletedComp.getMessage());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    public void simulation_deleted_proteins() throws Exception {

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

        // one protein without any interactions
        Protein noInteractions = getMockBuilder().createProtein("P12348", "no_interaction");
        getCorePersister().saveOrUpdate(noInteractions);

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
        Assert.assertEquals(4, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(9, getDaoFactory().getComponentDao().countAll());

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
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(4, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(8, getDaoFactory().getComponentDao().countAll());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe3.getAc()));
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 2 deleted proteins : one duplicate and one without any interactions
        DeletedProteinEventDao deletedProteinDao = updateFactory.getDeletedProteinEventDao();
        List<DeletedProteinEvent> deletedEvents = deletedProteinDao.getAll();

        Assert.assertEquals(2, deletedEvents.size());

        for (DeletedProteinEvent evt : deletedEvents){
            if (evt.getProteinAc().equals(noInteractions.getAc())){
                Assert.assertEquals("P12348", evt.getUniprotAc());
                Assert.assertEquals("Protein without any interactions", evt.getMessage());

            }
            else if (evt.getProteinAc().equals(dupe3.getAc())){
                Assert.assertEquals("P12345", evt.getUniprotAc());
                Assert.assertEquals("Duplicate of "+dupe2.getAc(), evt.getMessage());
            }
            else {
                // error if another protein is deleted
                Assert.assertFalse(true);
            }
        }

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    public void simulation_dead_protein() throws Exception {

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

        // one dead protein with an interaction
        Protein deadProtein = getMockBuilder().createProtein("Pxxxx", "dead_protein");
        getCorePersister().saveOrUpdate(deadProtein);

        Interaction interaction_4 = getMockBuilder().createInteraction(simple, deadProtein);

        getCorePersister().saveOrUpdate(interaction_4);

        // one protein with a splice variant which doesn't exist in uniprot but contains one interaction
        Protein parent = getMockBuilder().createProtein("P12350", "not_existing_splice_variant");
        parent.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Protein isoform_no_uniprot = getMockBuilder().createProteinSpliceVariant(parent, "P12350-2", "not_existing_splice_variant");
        isoform_no_uniprot.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Interaction interaction_8 = getMockBuilder().createInteraction(simple, isoform_no_uniprot);
        getCorePersister().saveOrUpdate(interaction_8);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(4, getDaoFactory().getComponentDao().countAll());
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
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(4, getDaoFactory().getComponentDao().countAll());
        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = IntactUpdateContext.getCurrentInstance().beginTransaction();
        // checks what has been saved
        UpdateDaoFactory updateFactory = IntactUpdateContext.getCurrentInstance().getUpdateFactory();

        // 2 dead proteins : one dead master protein and one non existing splice variant
        DeadProteinEventDao deadProteinDao = updateFactory.getDeadProteinEventDao();
        List<DeadProteinEvent> deadEvents = deadProteinDao.getAll();

        Assert.assertEquals(2, deadEvents.size());

        for (DeadProteinEvent deadEvt : deadEvents){
            Assert.assertNull(deadEvt.getMessage());
            Assert.assertTrue(deadEvt.getDeletedXrefs().isEmpty());

            if (deadEvt.getProteinAc().equals(deadProtein.getAc())){
                Assert.assertEquals("Pxxxx", deadEvt.getUniprotAc());

            }
            else if (deadEvt.getProteinAc().equals(isoform_no_uniprot.getAc())){
                Assert.assertEquals("P12350-2", deadEvt.getUniprotAc());
            }
            else {
                // error if another protein is dead
                Assert.assertFalse(true);
            }
        }

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @DirtiesContext
    public void simulation_same_sequence() throws Exception {

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

        // one protein no uniprot update with one interaction
        Protein noUniProtUpdate = getMockBuilder().createProtein("P12342", "no_uniprot_update");
        noUniProtUpdate.getXrefs().clear();
        Annotation noUniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        noUniProtUpdate.addAnnotation(noUniprot);
        getCorePersister().saveOrUpdate(noUniProtUpdate);

        Interaction interaction_1 = getMockBuilder().createInteraction(simple, noUniProtUpdate);
        getCorePersister().saveOrUpdate(interaction_1);

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

        // 1 protein having same sequence as another of its transcripts
        SequenceIdenticalToTranscriptEventDao seqIdEvtDao = updateFactory.getSequenceIdenticalToTranscriptEventDao();
        List<SequenceIdenticalToTranscriptEvent> seqIdEvts = seqIdEvtDao.getAll();

        Assert.assertEquals(1, seqIdEvts.size());

        SequenceIdenticalToTranscriptEvent seqIdEvt = seqIdEvts.iterator().next();
        Assert.assertEquals(simple.getAc(), seqIdEvt.getProteinAc());
        Assert.assertEquals("P36872", seqIdEvt.getUniprotAc());
        Assert.assertEquals("P36872-2", seqIdEvt.getMatchingUniprotTranscript());
        Assert.assertNull(seqIdEvt.getMessage());

        IntactUpdateContext.getCurrentInstance().commitTransaction(status3);
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
