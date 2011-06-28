package uk.ac.ebi.intact.dbupdate.prot;

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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * Second Tester of ProteinProcessor
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinProcessor4Test extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setUniprotService(new MockUniprotService());

        processor = new ProteinUpdateProcessor();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        processor = null;
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should be merged because
     * the configuration allows to fix duplicates and only the original protein should be updated
     */
    public void update_protein_and_fix_duplicates() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(1, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * the configuration doesn't allow to fix duplicates and both proteins should be updated.
     * The isoforms and chains cannot be updated because we don't have a single parent in intact
     */
    public void update_protein_and_fix_duplicates_no() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        context.commitTransaction(status);

        List<Protein> intactproteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(2, intactproteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        // reset
        config.setFixDuplicates(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should be merged because
     * the configuration allows to fix duplicates and only the original isoform should be updated.
     */
    public void update_protein_and_fix_duplicates_isoform() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(protein, "P60953-1", "isoform1");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "isoform2");
        isoform2.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);
        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        getCorePersister().saveOrUpdate(interaction3);
        Interaction interaction4 = getMockBuilder().createInteraction(isoform2, random);
        getCorePersister().saveOrUpdate(interaction4);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(2, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * the configuration doesn't allow to fix duplicates and both isoforms should be updated.
     */
    public void update_protein_and_fix_duplicates_isoforms_no() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(protein, "P60953-1", "isoform1");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "isoform2");
        isoform2.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);
        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        getCorePersister().saveOrUpdate(interaction3);
        Interaction interaction4 = getMockBuilder().createInteraction(isoform2, random);
        getCorePersister().saveOrUpdate(interaction4);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(4, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * there are feature conflicts and only the original protein should be updated.
     * The duplicated protein should be tagged as 'no-uniprot-update'
     */
    public void update_protein_and_fix_duplicates_conflicts_no_transcripts() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);
        secondary.setCreated(new Date(1));  // original protein

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * there are feature conflicts and only the original protein should be updated.
     * The duplicated protein is deleted because it was possible to remap the sequence to one isoform
     */
    public void update_protein_and_fix_duplicates_conflicts_transcripts_yes() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);
        secondary.setCreated(new Date(1));

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence("SYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHHH");
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(30, 30, 36, 36);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context.beginTransaction();

        Assert.assertEquals(3, intactProteins.size());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * there are feature conflicts and only the original isoform should be updated.
     * The duplicated isoform should be tagged as 'no-uniprot-update'
     */
    public void update_protein_and_fix_isoforms_duplicates_conflicts_no_transcripts() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(secondary, "P60953-1", "isoformValid");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);
        isoform.setCreated(new Date(1));

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "duplicate");
        isoform2.getBioSource().setTaxId("9606");
        isoform2.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        isoform2.getAnnotations().clear();
        isoform2.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(isoform2, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(isoform2.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        context.commitTransaction(status);

        List<Protein> intactproteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, intactproteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * there are feature conflicts and only the original isoform should be updated.
     * The duplicated isoform is deleted because it was possible to remap the sequence to another isoform
     */
    public void update_protein_and_fix_isoform_duplicates_conflicts_transcripts_yes() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(secondary, "P60953-1", "isoformValid");
        isoform.getBioSource().setTaxId("9606");
        isoform.setCreated(new Date(1));
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "duplicate");
        isoform2.getBioSource().setTaxId("9606");
        isoform2.setSequence("SYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHHH");
        isoform2.getAnnotations().clear();
        isoform2.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(30, 30, 36, 36);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(isoform2, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(isoform2.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        context.commitTransaction(status);

        List<Protein> intactproteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, intactproteins.size());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        Protein isoformLoaded = getDaoFactory().getProteinDao().getByUniprotId("P60953-2").iterator().next();
        Assert.assertEquals(1, isoformLoaded.getActiveInstances().size());

        Assert.assertTrue(hasXRef(isoformLoaded, isoform2.getAc(), CvDatabase.INTACT, "intact-secondary"));

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein is involved in one interactions which contains invalid ranges.
     */
    public void update_protein_bad_ranges_from_beginning() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(0, 0, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(secondary.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        context.commitTransaction(status);

        List<Protein> intactproteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, intactproteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        context2.commitTransaction(status2);
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

    private boolean hasAlias( Protein p, String aliasLabel, String aliasName ) {
        final Collection<InteractorAlias> aliases = p.getAliases();

        boolean hasFoundAlias = false;

        for ( InteractorAlias alias : aliases ) {
            if (alias.getCvAliasType().getShortLabel().equals(aliasLabel)){
                if (aliasName.equals(alias.getName())){
                    hasFoundAlias = true;
                }
            }
        }

        return hasFoundAlias;
    }
}
