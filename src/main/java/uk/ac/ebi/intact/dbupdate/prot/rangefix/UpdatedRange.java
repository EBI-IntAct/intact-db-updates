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

import uk.ac.ebi.intact.model.Range;

/**
 * Represents an updated range and contains the old version and the new version of the Range.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UpdatedRange {

    private Range oldRange;
    private Range newRange;
    private String message;

    public UpdatedRange(Range oldRange, Range newRange) {
        this(oldRange, newRange, null);
    }

    public UpdatedRange(Range oldRange, Range newRange, String message) {
        this.oldRange = oldRange;
        this.newRange = newRange;
        this.message = message;
    }

    public Range getOldRange() {
        return oldRange;
    }

    public Range getNewRange() {
        return newRange;
    }

    public String getMessage() {
        return message;
    }

    public void setMessage(String message) {
        this.message = message;
    }

    public boolean isSequenceChanged() {
        if (isRangeLengthChanged()) {
            return true;
        }

        return !(rangeSequence(oldRange).equals(rangeSequence(newRange)));
    }

    public boolean isRangeLengthChanged() {
        return rangeLength(oldRange) != rangeLength(newRange);
    }

    protected int rangeLength(Range range) {
        if (range.getToIntervalEnd() == -1) {
            return -1;
        }

        return range.getToIntervalEnd()+1 - range.getFromIntervalStart();
    }

    protected String rangeSequence(Range range) {
        int length = rangeLength(range);

        if (length <= 0 || range.getSequence() == null) {
            return "";
        }

        return range.getSequence().substring(0, rangeLength(range));
    }
}
