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

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.Range;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UpdatedRangeTest extends IntactBasicTestCase {

    @Test
    public void isSequenceChanged_no() throws Exception {
        String oldSequence = "ABCDEDF";
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        oldRange.prepareSequence(oldSequence);

        String newSequence = "ABCDEDF";
        Range newRange = getMockBuilder().createRange(2, 2, 4, 4);
        newRange.prepareSequence(newSequence);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertFalse(updated.isSequenceChanged());

        Assert.assertEquals(3, updated.rangeLength(oldRange));
        Assert.assertEquals(3, updated.rangeLength(newRange));

        Assert.assertEquals("BCD", updated.rangeSequence(oldRange));
        Assert.assertEquals("BCD", updated.rangeSequence(newRange));
    }
    
    @Test
    public void isSequenceChanged_yes() throws Exception {
        String oldSequence = "ABCDEDF";
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        oldRange.prepareSequence(oldSequence);

        String newSequence = "ABZDEDF";
        Range newRange = getMockBuilder().createRange(2, 2, 4, 4);
        newRange.prepareSequence(newSequence);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertTrue(updated.isSequenceChanged());

        Assert.assertEquals(3, updated.rangeLength(oldRange));
        Assert.assertEquals(3, updated.rangeLength(newRange));

        Assert.assertEquals("BCD", updated.rangeSequence(oldRange));
        Assert.assertEquals("BZD", updated.rangeSequence(newRange));
    }

    @Test
    public void isSequenceChanged_yes2() throws Exception {
        String oldSequence = "ABCDEDF";
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        oldRange.prepareSequence(oldSequence);

        String newSequence = "ABZDEDF";
        Range newRange = getMockBuilder().createRange(-1, -1, -1, -1);
        newRange.prepareSequence(newSequence);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertTrue(updated.isSequenceChanged());
    }

    @Test
    public void isRangeChanged_no1() throws Exception {
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        Range newRange = getMockBuilder().createRange(2, 2, 4, 4);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertFalse(updated.isRangeLengthChanged());
    }

    @Test
    public void isRangeChanged_yes1() throws Exception {
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        Range newRange = getMockBuilder().createRange(1, 1, 4, 4);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertTrue(updated.isRangeLengthChanged());
    }
    
    @Test
    public void isRangeChanged_yes2() throws Exception {
        Range oldRange = getMockBuilder().createRange(2, 2, 4, 4);
        Range newRange = getMockBuilder().createRange(-1, -1, -1, -1);

        UpdatedRange updated = new UpdatedRange(oldRange, newRange);
        Assert.assertTrue(updated.isRangeLengthChanged());
    }

}
