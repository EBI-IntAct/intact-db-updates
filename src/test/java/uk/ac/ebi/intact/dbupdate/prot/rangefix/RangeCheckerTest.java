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
package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import org.junit.*;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;

import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeCheckerTest extends IntactBasicTestCase {

    private RangeChecker rangeChecker;

    @Before
    public void before() throws Exception {
        rangeChecker = new RangeChecker();
    }

    @After
    public void after() throws Exception {
        rangeChecker = null;
    }

    @Test
    public void shiftFeatureRanges_noChanges() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals("BCD", rangeSeq);

        final Collection<Feature> updatedFeatures = rangeChecker.shiftFeatureRanges(Collections.singleton(feature), oldSequence, newSequence);

        Assert.assertTrue(updatedFeatures.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }
    
    @Test
    public void shiftFeatureRanges_substitutionWithinRange() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<Feature> updatedFeatures = rangeChecker.shiftFeatureRanges(Collections.singleton(feature), oldSequence, newSequence);

        Assert.assertTrue(updatedFeatures.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertNotSame(rangeAfterFix, rangeSeq);
    }

    @Test
    public void shiftFeatureRanges_upstreamInsertion() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ZABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<Feature> updatedFeatures = rangeChecker.shiftFeatureRanges(Collections.singleton(feature), oldSequence, newSequence);

        Assert.assertEquals(1, updatedFeatures.size());
        
        Assert.assertEquals(3, range.getFromIntervalStart());
        Assert.assertEquals(3, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

    @Test
    public void shiftFeatureRanges_insertionDownstream() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEFZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<Feature> updatedFeatures = rangeChecker.shiftFeatureRanges(Collections.singleton(feature), oldSequence, newSequence);

        Assert.assertTrue(updatedFeatures.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

     @Test
     @Ignore
    public void shiftFeatureRanges_differentSeq() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ZZAZZDZFEZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<Feature> updatedFeatures = rangeChecker.shiftFeatureRanges(Collections.singleton(feature), oldSequence, newSequence);

        Assert.assertFalse(updatedFeatures.isEmpty());

        Assert.assertEquals(-1, range.getFromIntervalStart());
        Assert.assertEquals(-1, range.getFromIntervalEnd());
        Assert.assertEquals(-1, range.getToIntervalStart());
        Assert.assertEquals(-1, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }
}
