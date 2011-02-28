/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.junit.Assert;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.commons.util.Crc64;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Interaction;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * Tester of the SequenceChangedListener
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class SequenceChangeListenerTest extends IntactBasicTestCase {

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is not different : no caution should be added
     */
    public void onProteinSequenceChanged_cautionNo() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);

        String oldSequence = "ABCD";
        String newSequence = "ABCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener();
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsAfter.size());
    }

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is very different : a caution should be added
     */
    public void onProteinSequenceChanged_cautionYes() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);

        String oldSequence = "ABCD";
        String newSequence = "ZZZZ";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener();
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsAfter.size());
        System.out.println(cautionsAfter);
    }

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is very different : a caution should be added
     */
    public void onProteinSequenceChanged_cautionYes2() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);

        String oldSequence = "ABCD";
        String newSequence = "ZZCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsAfter.size());
    }

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is very different : a caution should be added at the level of the interaction as well
     */
    public void onProteinSequenceChanged_cautionOnInteraction() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);

        String oldSequence = "ABCD";
        String newSequence = "ZZCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        Interaction interaction = getMockBuilder().createInteraction(prot);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsAfter.size());
        final Collection<Annotation> interactionCautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot.getActiveInstances().iterator().next().getInteraction(), Collections.singleton(caution));
        Assert.assertEquals(1, interactionCautionsAfter.size());
    }

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is very different : a caution should be added
     */
    public void onProteinSequenceChanged_cautionYes3() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);

        String oldSequence = "ABCD";
        String newSequence = "ZZZD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, cautionsAfter.size());
    }

    @Test
    @DirtiesContext
    @Ignore
    /**
     * The sequence is very similar : no caution should be added
     */
    public void onProteinSequenceChanged_cautionNo2() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
        
        String oldSequence = "ABCD";
        String newSequence = "ZBCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                prot, oldSequence, newSequence, Crc64.getCrc64(newSequence)));

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, cautionsAfter.size());
    }
}
