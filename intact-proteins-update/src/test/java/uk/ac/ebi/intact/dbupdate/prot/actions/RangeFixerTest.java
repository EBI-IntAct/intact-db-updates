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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.Arrays;
import java.util.Collection;
import java.util.Collections;

/**
 * Tester of RangeFixerImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Aug-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class RangeFixerTest extends IntactBasicTestCase {

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
     * One range is invalid with the current protein sequence.
     * An annotation 'invalid-range' should be added and a caution should be added as well at the level of the feature
     */
    public void fix_invalid_range() throws Exception {
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        String oldSequence = "ABCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(oldSequence);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component component = interaction.getComponents().iterator().next();
        component.getBindingDomains().clear();
        component.addBindingDomain(feature);

        getCorePersister().saveOrUpdate(interaction);

        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalidBefore.size());
        final Collection<Annotation> invalidPosBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, invalidPosBefore.size());

        InvalidRange invalidRange = new InvalidRange(range, null, oldSequence, "out of bound", "certain", "certain", false);

        RangeUpdateReport rangeReport = new RangeUpdateReport();
        rangeReport.getInvalidComponents().put(component, Arrays.asList(invalidRange));

        // fix invalid ranges
        rangeFixer.fixInvalidRanges(new InvalidRangeEvent(context, invalidRange, rangeReport), new ProteinUpdateProcessor());

        Assert.assertTrue(hasAnnotation(feature, "["+range.getAc()+"]out of bound", invalid_range.getShortLabel()));
        Assert.assertTrue(hasAnnotation(feature, "["+range.getAc()+"]2-5", invalid_positions.getShortLabel()));
        Assert.assertTrue(range.getFromCvFuzzyType().isUndetermined());
        Assert.assertTrue(range.getToCvFuzzyType().isUndetermined());
        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One range is invalid with the new protein sequence.
     * An annotation 'range-conflicts' should be added at the level of the feature
     */
    public void fix_out_of_Date_range() throws Exception {
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("range-conflicts");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");
        CvTopic sequence_version = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("sequence-version");

        String oldSequence = "MRFAVGALLACAALGLCLAVPDKTVKWCAVSEHENTKCISFRDHMKTVLPADGPRLACVK" +
                "KTSYQDCIKAISGGEADAITLDGGWVYDAGLTPNNLKPVAAEFYGSLEHPQTHYLAVAVV" +
                "KKGTDFQLNQLQGKKSCHTGLGRSAGWIIPIGLLFCNLPEPRKPLEKAVASFFSGSCVPC" +
                "ADPVAFPQLCQLCPGCGCSPTQPFFGYVGAFKCLRDGGGDVAFVKHTTIFEVLPQKADRD" +
                "QYELLCLDNTRKPVDQYEDCYLARIPSHAVVARNGDGKEDLIWEILKVAQEHFGKGKSKD" +
                "FQLFGSPLGKDLLFKDSAFGCYGVPPRMDYRLYLGHSYVTAIRNQREGVCPEASIDSAPV" +
                "KWCALSHQERAKCDEWSVTSNGQIECESAESTEDCIDKIVNGEADAMSLDGGHAYIAGQC" +
                "GLVPVMAENYDISSCTNPQSDVFPKGYYAVAVVKASDSSINWNNLKGKKSCHTGVDRTAG" +
                "WNIPMGLLFSRINHCKFDEFFSQGCAPGYKKNSTLCDLCIGPAKCAPNNREGYNGYTGAF" +
                "QCLVEKGDVAFVKHQTVLENTNGKNTAAWAKDLKQEDFQLLCPDGTKKPVTEFATCHLAQ" +
                "APNHVVVSRKEKAARVSTVLTAQKDLFWKGDKDCTGNFCLFRSSTKDLLFRDDTKCLTKL" +
                "PEGTTYEEYLGAEYLQAVGNIRKCSTSRLLEACTFHKS";

        Protein prot = getMockBuilder().createProtein("P12346", "protein");
        prot.setSequence(oldSequence);

        Range range = getMockBuilder().createRange(378, 378, 382, 382);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component component = interaction.getComponents().iterator().next();
        component.getBindingDomains().clear();
        component.addBindingDomain(feature);

        getCorePersister().saveOrUpdate(interaction);

        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalidBefore.size());
        final Collection<Annotation> invalidPosBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, invalidPosBefore.size());
        final Collection<Annotation> seVersionBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(sequence_version));
        Assert.assertEquals(0, seVersionBefore.size());

        InvalidRange invalidRange = new InvalidRange(range, range, oldSequence, "out of bound", "certain", "certain", true);

        RangeUpdateReport rangeReport = new RangeUpdateReport();
        rangeReport.getInvalidComponents().put(component, Arrays.asList(invalidRange));

        // fix invalid ranges
        rangeFixer.fixOutOfDateRanges(new InvalidRangeEvent(context, invalidRange, rangeReport), new ProteinUpdateProcessor());

        Assert.assertTrue(hasAnnotation(feature, "["+range.getAc()+"]out of bound", invalid_range.getShortLabel()));
        Assert.assertTrue(hasAnnotation(feature, "["+range.getAc()+"]378-382", invalid_positions.getShortLabel()));
        Assert.assertFalse(hasAnnotation(feature, "["+range.getAc()+"]2", sequence_version.getShortLabel()));
        Assert.assertTrue(range.getFromCvFuzzyType().isUndetermined());
        Assert.assertTrue(range.getToCvFuzzyType().isUndetermined());
        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One range is invalid with the current protein sequence. The feature already contains the caution and the 'invalid-range' annotation
     * No supplementary annotation 'invalid-range' should be added and the same for the caution
     */
    public void fix_invalid_range_several_cautions_sameMessage() throws Exception {
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalidPositions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        String oldSequence = "ABCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(oldSequence);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component component = interaction.getComponents().iterator().next();
        component.getBindingDomains().clear();
        component.addBindingDomain(feature);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Annotation invalidRange = new Annotation(invalid_range, "["+range.getAc()+"]out of bound");
        feature.addAnnotation(invalidRange);
        Annotation cautionRange = new Annotation(invalidPositions, "["+range.getAc()+"]2-5");
        feature.addAnnotation(cautionRange);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidBefore.size());
        final Collection<Annotation> invalidPosBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalidPositions));
        Assert.assertEquals(1, invalidPosBefore.size());

        InvalidRange invalidRangeEvt = new InvalidRange(range, null, oldSequence, "out of bound", "certain", "certain", false);

        RangeUpdateReport rangeReport = new RangeUpdateReport();
        rangeReport.getInvalidComponents().put(component, Arrays.asList(invalidRangeEvt));

        // fix invalid ranges
        rangeFixer.fixInvalidRanges(new InvalidRangeEvent(getDataContext(), invalidRangeEvt, rangeReport), new ProteinUpdateProcessor());

        final Collection<Annotation> invalidAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidAfter.size());
        final Collection<Annotation> invalidPosAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalidPositions));
        Assert.assertEquals(1, invalidPosAfter.size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence up-to-date with uniprot and no bad ranges are attached to it.
     * When updating the ranges attached to this protein, no components with range conflicts should be returned and the feature
     * should not have any cautions or 'invalid-range' annotation
     */
    public void update_range_no_sequence_change_no_bad_range() throws Exception {
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

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence up-to-date with uniprot and one invalid range is attached to it.
     * When updating the ranges attached to this protein, one component with range conflicts should be returned and the feature
     * should have a caution and 'invalid-range' annotation
     */
    public void update_range_no_sequence_change_one_bad_range() throws Exception {
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

        // the annotations are not added when updating the ranges
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_positions));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(uniprot.getSequence().length() + 1, range.getToIntervalStart());
        Assert.assertEquals(uniprot.getSequence().length() + 1, range.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein has a sequence not up-to-date with uniprot and all the ranges can be shifted without any problems.
     * When updating the ranges attached to this protein, no components with range conflicts should be returned and the feature
     * should not have any cautions or 'invalid-range' annotation.
     * The ranges should be shifted.
     */
    public void update_range_sequence_change_range_possible_to_shift() throws Exception {
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic invalid_positions = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-positions");

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(uniprot.getSequence().substring(5));

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
        Assert.assertEquals(7, range.getFromIntervalStart());
        Assert.assertEquals(7, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

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
