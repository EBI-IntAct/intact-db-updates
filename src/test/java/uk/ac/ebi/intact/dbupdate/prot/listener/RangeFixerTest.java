package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Aug-2010</pre>
 */

public class RangeFixerTest extends IntactBasicTestCase {

    @Test
    @DirtiesContext
    public void onRangeOutOfBound_beforeUpdate() throws Exception {
        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");

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

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsBefore.size());

        RangeFixer listener = new RangeFixer();
        listener.onInvalidRange(new InvalidRangeEvent(getDataContext(), new InvalidRange(range, oldSequence, "out of bound")));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, cautionsAfter.size());
    }
}
