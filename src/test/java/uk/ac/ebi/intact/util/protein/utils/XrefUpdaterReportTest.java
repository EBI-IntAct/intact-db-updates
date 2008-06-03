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
package uk.ac.ebi.intact.util.protein.utils;

import org.junit.Assert;
import org.junit.Test;
import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.model.Institution;
import uk.ac.ebi.intact.model.Xref;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class XrefUpdaterReportTest {

    @Test
    public void updated() {
        Xref[] created = new Xref[0];
        Xref[] deleted = new Xref[0];

        XrefUpdaterReport report = new XrefUpdaterReport(null, created, deleted);
        Assert.assertFalse(report.isUpdated());
    }
    
    @Test
    public void created() {
        Xref[] created = new Xref[] { getMockBuilder().createProteinRandom().getXrefs().iterator().next() };
        Xref[] deleted = new Xref[0];

        XrefUpdaterReport report = new XrefUpdaterReport(null, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }

    @Test
    public void deleted() {
        Xref[] created = new Xref[0];
        Xref[] deleted = new Xref[] { getMockBuilder().createProteinRandom().getXrefs().iterator().next() };

        XrefUpdaterReport report = new XrefUpdaterReport(null, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }
    
    @Test
    public void created_deleted() {
        Xref[] created = new Xref[] { getMockBuilder().createProteinRandom().getXrefs().iterator().next() };
        Xref[] deleted = new Xref[] { getMockBuilder().createProteinRandom().getXrefs().iterator().next() };

        XrefUpdaterReport report = new XrefUpdaterReport(null, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }

    private IntactMockBuilder getMockBuilder() {
        return new IntactMockBuilder(new Institution("lalaInst"));}
}
