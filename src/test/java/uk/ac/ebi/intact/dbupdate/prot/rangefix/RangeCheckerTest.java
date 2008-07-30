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

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.CvFuzzyType;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;

import java.util.Collection;

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

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);

        Assert.assertTrue(updatedRanges.isEmpty());

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
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        Assert.assertTrue(range.getSequence().startsWith("BCD"));

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);

        Assert.assertTrue(updatedRanges.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        Assert.assertTrue(range.getSequence().startsWith("BZD"));

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

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);

        Assert.assertEquals(1, updatedRanges.size());
        
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

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);

        Assert.assertTrue(updatedRanges.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

     @Test
    public void shiftFeatureRanges_differentSeq() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ZZAZZDZFEZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(1, updatedRanges.size());

        UpdatedRange updatedRange = updatedRanges.iterator().next();
        Assert.assertTrue(updatedRange.isRangeLengthChanged());
        Assert.assertTrue(updatedRange.isSequenceChanged());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(6, range.getToIntervalStart());
        Assert.assertEquals(6, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_substitutionInExactPosition() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(3, 3, 3, 3);
        range.prepareSequence(oldSequence);
        feature.addRange(range);
        Assert.assertEquals('C',range.getSequence().charAt(0));
        
        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(1, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_undeterminedRange() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setUndetermined(true);
        range.prepareSequence(oldSequence);
        feature.addRange(range);
        
        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_undeterminedRange_reset() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 3, 3);
        range.setUndetermined(true);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(1, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_cTerminal_from() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_MI_REF, CvFuzzyType.C_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_cTerminal_to() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_MI_REF, CvFuzzyType.C_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setToCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_nTerminal_from() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_MI_REF, CvFuzzyType.N_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }
    
    @Test
    public void shiftFeatureRanges_nTerminal_to() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_MI_REF, CvFuzzyType.N_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setToCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test
    public void shiftFeatureRanges_toOutOfBounds() throws Exception {
        String oldSequence = "MDNCLAAAALNGVDRRSLQRSAKLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                             "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                             "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";
        String newSequence = "MDNCLAAAALNGVDRRSLQRSARLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                             "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                             "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 220, 220);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(1, updatedRanges.size());
        
        UpdatedRange updatedRange = updatedRanges.iterator().next();
        Assert.assertTrue(updatedRange.isRangeLengthChanged());
        Assert.assertTrue(updatedRange.isSequenceChanged());

        Assert.assertEquals(1, range.getFromIntervalStart());
        Assert.assertEquals(1, range.getFromIntervalEnd());
        Assert.assertEquals(210, range.getToIntervalStart());
        Assert.assertEquals(210, range.getToIntervalEnd());
    }

     @Test
    public void shiftFeatureRanges_toOutOfBounds2() throws Exception {
        String oldSequence = "MDNCLAAAAL";
        String newSequence = "MDNCLAAAALNGVDRRSLQRSARLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                             "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                             "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 40, 40);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);
        Assert.assertEquals(1, updatedRanges.size());

        UpdatedRange updatedRange = updatedRanges.iterator().next();
        Assert.assertTrue(updatedRange.isRangeLengthChanged());
        Assert.assertTrue(updatedRange.isSequenceChanged());

        Assert.assertEquals(1, range.getFromIntervalStart());
        Assert.assertEquals(1, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());
    }
}
