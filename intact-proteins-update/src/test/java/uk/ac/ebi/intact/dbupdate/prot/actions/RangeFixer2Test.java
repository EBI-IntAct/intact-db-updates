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
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.FeatureUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.Collection;
import java.util.Collections;

/**
 * Second
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>07-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class RangeFixer2Test extends IntactBasicTestCase {
    private RangeFixerImpl rangeFixer;
    @Before
    public void setUp(){
        rangeFixer = new RangeFixerImpl();

        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        rangeFixer = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence not up-to-date with uniprot and one range cannot be shifted.
     * When updating the ranges attached to this protein, one component with range conflicts should be returned but the feature
     * should not have any cautions or 'invalid-range' annotation.
     * The ranges should not be shifted.
     */
    public void update_range_sequence_change_one_range_impossible_to_shift() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("range-conflicts");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(uniprot.getSequence());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);
        Component componentWithConflicts = null;

        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
                componentWithConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, uniprot.getSequence().substring(5), new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());
        Collection<Component> componentToFix = rangeReport.getInvalidComponents().keySet();

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence null and all the ranges can match the uniprot sequence without any problems.
     * When updating the ranges attached to this protein, no components with range conflicts should be returned and the feature
     * should not have any cautions or 'invalid-range' annotation.
     * The range should not be shifted, only the sequence has been set.
     */
    public void update_range_sequence_change_previous_sequence_null() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(null);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
            }
        }

        Assert.assertNull(range.getFullSequence());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());
        Collection<Component> componentToFix = rangeReport.getInvalidComponents().keySet();

        Assert.assertTrue(componentToFix.isEmpty());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());
        Assert.assertNotNull(range.getFullSequence());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence null but one range has conflicts with the uniprot sequence.
     * When updating the ranges attached to this protein, one components with range conflicts should be returned and the feature
     * should not have any cautions or 'invalid-range' annotation.
     * The range should not be shifted, and the sequence cannot be set.
     */
    public void update_range_sequence_change_one_range_impossible_to_shift_previous_sequence_null() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(null);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRange(2, 2, uniprot.getSequence().length() + 1, uniprot.getSequence().length() + 1);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);
        Component componentWithConflicts = null;

        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
                componentWithConflicts = c;
            }
        }

        Assert.assertNull(range.getFullSequence());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());
        Collection<Component> componentToFix = rangeReport.getInvalidComponents().keySet();

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(uniprot.getSequence().length() + 1, range.getToIntervalStart());
        Assert.assertEquals(uniprot.getSequence().length() + 1, range.getToIntervalEnd());
        Assert.assertNull(range.getFullSequence());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence not up-to-date with uniprot and one range cannot be shifted because invalid with the current protein sequence.
     * When updating the ranges attached to this protein, one component with range conflicts should be returned but the feature
     * should have a caution and 'invalid-range' annotation.
     * The ranges should not be shifted.
     */
    public void update_range_sequence_change_one_range_impossible_to_shift_invalidWithPreviousSequence() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(uniprot.getSequence());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRange(0, 0, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);
        Component componentWithConflicts = null;

        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
                componentWithConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());
        Collection<Component> componentToFix = rangeReport.getInvalidComponents().keySet();

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void update_range_sequence_change_one_feature_out_of_date_range() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(uniprot.getSequence());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRangeUndetermined();
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);
        feature.addAnnotation(getMockBuilder().createAnnotation("[xxxxx]invalid", invalid_range));
        feature.addAnnotation(getMockBuilder().createAnnotation("[xxxxx]0-0", invalid_positions));

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);
        Component componentWithConflicts = null;

        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
                componentWithConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());
        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidBefore.size());
        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(1, cautionsBefore.size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());
        Collection<Component> componentToFix = rangeReport.getInvalidComponents().keySet();

        Assert.assertTrue(rangeReport.getInvalidComponents().isEmpty());

        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void update_range_sequence_change_one_feature_out_of_date_range_possible_to_update() throws Exception {
        String sequenceAfter = "MDEEQMWKGFFFKLQELKMNPPTEHEHQLQDGDDGFFEKFGSLLRQKSQIDEGVLKSSLA" +
                "PLEENGSGGEEDSDESPDGTLQLSQDSECTSIDTFATESTLASSSDKDISTSVLDSAPVM" +
                "NVTDLYEEILFEIFNNIGCENNEECTNSLVEFVQDAFKIPNATHEEIYEAARLKEPPNVR" +
                "LNVEIIKAENLMSKDSNGLSDPFVTLYLESNGSHRYNSSVKPATLNPIWEEHFSLDFDAA" +
                "ETVKEKVNKILDVKGVKGLSKLMKEIAVTASSGKHDNELIGRAAITLKSIPVSGLTVWYN" +
                "LEKGSKGRSRGSLLVNLALSAEKNKSVAVQEHKNLLKLLLMYELETSQVANYWWSGKFSP" +
                "NAELIRSQHAAQSGLTPFDCALSQWHAYSTIHETHKLNFTLFNSILDVVVPVITYMQNDS" +
                "EDVKTFWDGVKRLLPSCFAVLRKLRSKNTSDKNIIRALNEVLDILKKIKELEVPESVDIF" +
                "PKSVYGWLHTNDTDETCNIDTAIEDAINTGTREWLEHIVEGSRQSKNTETDDEKLQYVIK" +
                "LIQMVRSDLQRAMEYFDKIFYHKIQLNYSAVLYLFYDSKLAEICKSIIIEVCNNIKRLDV" +
                "PDDQFEYLPNLENVNMGTTLFEVYLILKRYVQLGESLCSEPLELSNFYPWFERGVTHWLD" +
                "ISIIKALSRIQKAIDLDQLKAVDETVKYSSSAVDTLSIFYQIKIFWQQLDWPEVEGSYIF" +
                "VAKIVNDLCRCCIFYAQQMSRRVENIFIADDNNKNFSKFYWKPFFIIYCLLGEYRTNLEA" +
                "ERCASTIKTVIENALDTERNQIVELIEIVARKMAPPIRRYLAEGAEVLAKDSNSMDQLMM" +
                "YLESSLATLYDTLNEINFQRILDGIWSELSIIMYDLIQSNLDKRRPPAFFQNLNNTLQTM" +
                "MDCFKMGNLQTSDIKILSSIQSRLRLYSLETSDLIHQYYLERLENQKSQESSPYGQLTIT" +
                "AQLTDTGLLVGLQSFET";

        String sequence2 = "MWKGFFFKLQELKMNPPTEHEHQLQDGDDGFFEKFGSLLRQKSQIDEGVLKSSLAPLEEN" +
                "GSGGEEDSDESPDGTLQLSQDSECTSIDTFATESTLASSSDKDISTSVLDSAPVMNVTDL" +
                "YEEILFEIFNNIGCENNEECTNSLVEFVQDAFKIPNATHEEIYEAARLKEPPNVRLNVEI" +
                "IKAENLMSKDSNGLSDPFVTLYLESNGSHRYNSSVKPATLNPIWEEHFSLDFDAAETVKE" +
                "KVNKILDVKGVKGLSKLMKEIAVTASSGKHDNELIGRAAITLKSIPVSGLTVWYNLEKGS" +
                "KGRSRGSLLVNLALSAEKNKSVAVQEHKNLLKLLLMYELETSQVANYWWSGKFSPNAELI" +
                "RSQHAAQSGLTPFDCALSQWHAYSTIHETHKLNFTLFNSILDVVVPVITYMQNDSEDVKT" +
                "FWDGVKRLLPSCFAVLRKLRSKNTSDKNIIRALNEVLDILKKIKELEVPESVDIFPKSVY" +
                "GWLHTNDTDETCNIDTAIEDAINTGTREWLEHIVEGSRQSKNTETDDEKLQYVIKLIQMV" +
                "RSDLQRAMEYFDKIFYHKIQLNYSAVLYLFYDSKLAEICKSIIIEVCNNIKRLDVPDDQF" +
                "EYLPNLENVNMGTTLFEVYLILKRYVQLGESLCSEPLELSNFYPWFERGVTHWLDISIIK" +
                "ALSRIQKAIDLDQLKAVDETVKYSSSAVDTLSIFYQIKIFWQQLDWPEVEGSYIFVAKIV" +
                "NDLCRCCIFYAQQMSRRVENIFIADDNNKNFSKFYWKPFFIIYCLLGEYRTNLEAERCAS" +
                "TIKTVIENALDTERNQIVELIEIVARKMAPPIRRYLAEGAEVLAKDSNSMDQLMMYLESS" +
                "LATLYDTLNEINFQRILDGIWSELSIIMYDLIQSNLDKRRPPAFFQNLNNTLQTMMDCFK" +
                "MGNLQTSDIKILSSIQSRLRLYSLETSDLIHQYYLERLENQKSQESSPYGQLTITAQLTD" +
                "TGLLVGLQSFET";

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("range-conflicts");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");
        CvTopic seq_version = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("sequence-version");

        Protein prot = getMockBuilder().createProtein("Q9VBY8", "protein");
        prot.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        Protein randomProtein1 = getMockBuilder().createProteinRandom();
        Protein randomProtein2 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(randomProtein1, randomProtein2);

        Range range = getMockBuilder().createRangeUndetermined();
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot, randomProtein1);

        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(prot.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, randomProtein2);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        feature.addAnnotation(getMockBuilder().createAnnotation("["+range.getAc()+"]invalid", invalid_range));
        feature.addAnnotation(getMockBuilder().createAnnotation("["+range.getAc()+"]1-7", invalid_positions));
        feature.addAnnotation(getMockBuilder().createAnnotation("["+range.getAc()+"]Q9VBY8,2", seq_version));

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, prot.getActiveInstances().size());
        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidBefore.size());
        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(1, cautionsBefore.size());

        // update ranges and collect components with feature conflicts
        RangeUpdateReport rangeReport = rangeFixer.updateRanges(prot, sequenceAfter, new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext());

        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals("6-12", FeatureUtils.convertRangeIntoString(range));
        Assert.assertTrue(rangeReport.getInvalidComponents().isEmpty());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    private boolean hasAnnotation( Feature f, String text, String cvTopic) {
        final Collection<Annotation> annotations = f.getAnnotations();
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
