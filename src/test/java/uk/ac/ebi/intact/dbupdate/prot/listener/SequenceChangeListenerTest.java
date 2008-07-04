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
import org.junit.Test;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class SequenceChangeListenerTest extends IntactBasicTestCase {

    @Test
    public void onProteinSequenceChanged_cautionNo() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);

        String oldSequence = "ABCD";
        String newSequence = "ABCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        final Collection<Annotation> topicsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener();
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                                                                         prot, oldSequence));

        final Collection<Annotation> topicsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsAfter.size());
    }
    
    @Test
    public void onProteinSequenceChanged_cautionYes() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);

        String oldSequence = "ABCD";
        String newSequence = "ZZZZ";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        final Collection<Annotation> topicsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener();
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                                                                         prot, oldSequence));

        final Collection<Annotation> topicsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, topicsAfter.size());
    }

    @Test
    public void onProteinSequenceChanged_cautionYes2() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);

        String oldSequence = "ABCD";
        String newSequence = "ZZCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        final Collection<Annotation> topicsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                                                                         prot, oldSequence));

        final Collection<Annotation> topicsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, topicsAfter.size());
    }

    @Test
    public void onProteinSequenceChanged_cautionYes3() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);

        String oldSequence = "ABCD";
        String newSequence = "ZZZD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        final Collection<Annotation> topicsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                                                                         prot, oldSequence));

        final Collection<Annotation> topicsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(1, topicsAfter.size());
    }
    
    @Test
    public void onProteinSequenceChanged_cautionNo2() throws Exception {
        CvTopic caution = getMockBuilder().createCvObject(CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);

        String oldSequence = "ABCD";
        String newSequence = "ZBCD";

        Protein prot = getMockBuilder().createProteinRandom();
        prot.setSequence(newSequence);

        final Collection<Annotation> topicsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsBefore.size());

        SequenceChangedListener listener = new SequenceChangedListener(0.50);
        listener.onProteinSequenceChanged(new ProteinSequenceChangeEvent(new ProteinUpdateProcessor(), getDataContext(),
                                                                         prot, oldSequence));

        final Collection<Annotation> topicsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(prot, Collections.singleton(caution));
        Assert.assertEquals(0, topicsAfter.size());
    }
}
