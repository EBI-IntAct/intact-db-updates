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
import uk.ac.ebi.intact.dbupdate.prot.actions.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.Collection;
import java.util.Collections;

/**
 * Tester of RangeFixer
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Aug-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class RangeFixerTest extends IntactBasicTestCase {

    private RangeFixer rangeFixer;
    @Before
    public void setUp(){
        rangeFixer = new RangeFixer();

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
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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

        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalidBefore.size());
        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        // fix invalid ranges
        rangeFixer.fixInvalidRanges(new InvalidRangeEvent(getDataContext(), new InvalidRange(range, oldSequence, "out of bound")));

        Assert.assertTrue(hasAnnotation(feature, "out of bound", invalid_range.getShortLabel()));
        Assert.assertTrue(hasAnnotation(feature, "out of bound", caution.getShortLabel()));

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        String oldSequence = "ABCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(oldSequence);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Annotation invalidRange = new Annotation(invalid_range, "out of bound");
        feature.addAnnotation(invalidRange);
        Annotation cautionRange = new Annotation(caution, "out of bound");
        feature.addAnnotation(cautionRange);

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component component = interaction.getComponents().iterator().next();
        component.getBindingDomains().clear();
        component.addBindingDomain(feature);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        final Collection<Annotation> invalidBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidBefore.size());
        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsBefore.size());

        // fix invalid ranges
        rangeFixer.fixInvalidRanges(new InvalidRangeEvent(getDataContext(), new InvalidRange(range, oldSequence, "out of bound")));

        final Collection<Annotation> invalidAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalidAfter.size());
        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsAfter.size());

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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor());

        Assert.assertTrue(componentToFix.isEmpty());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor());

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(1, cautions.size());
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor());

        Assert.assertTrue(componentToFix.isEmpty());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(0, cautions.size());
        Assert.assertEquals(7, range.getFromIntervalStart());
        Assert.assertEquals(7, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
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

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence().substring(5), new ProteinUpdateProcessor());

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence(), new ProteinUpdateProcessor());

        Assert.assertTrue(componentToFix.isEmpty());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence().substring(5), new ProteinUpdateProcessor());

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
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
        CvTopic caution = getDaoFactory().getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

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
        Collection<Component> componentToFix = rangeFixer.updateRanges(prot, uniprot.getSequence().substring(5), new ProteinUpdateProcessor());

        Assert.assertEquals(1, componentToFix.size());
        Assert.assertEquals(componentWithConflicts.getAc(), componentToFix.iterator().next().getAc());
        final Collection<Annotation> invalid = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, invalid.size());
        final Collection<Annotation> cautions = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(caution));
        Assert.assertEquals(1, cautions.size());
        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

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
