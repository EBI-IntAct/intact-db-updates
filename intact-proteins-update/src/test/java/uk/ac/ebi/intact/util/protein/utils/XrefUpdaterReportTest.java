package uk.ac.ebi.intact.util.protein.utils;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.IntactBasicTestCase;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.Xref;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class XrefUpdaterReportTest extends IntactBasicTestCase {

    @Test
    public void updated() {
        Collection<Xref> created = Collections.emptyList();
        Collection<Xref> deleted = Collections.emptyList();

        XrefUpdaterReport report = new XrefUpdaterReport(getMockBuilder().createProteinRandom(), created, deleted);
        Assert.assertFalse(report.isUpdated());
    }

    @Test
    public void created() {
        Protein protein = getMockBuilder().createProteinRandom();
        Collection<Xref> created = new ArrayList<>();
        created.add(protein.getXrefs().iterator().next());
        Collection<Xref> deleted = Collections.emptyList();

        XrefUpdaterReport report = new XrefUpdaterReport(protein, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }

    @Test
    public void deleted() {
        Protein protein = getMockBuilder().createProteinRandom();
        Collection<Xref> created = Collections.emptyList();
        Collection<Xref> deleted = new ArrayList<>();
        deleted.add(protein.getXrefs().iterator().next());

        XrefUpdaterReport report = new XrefUpdaterReport(protein, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }

    @Test
    public void created_deleted() {
        Protein protein = getMockBuilder().createProteinRandom();
        Collection<Xref> created = new ArrayList<>();
        created.add(protein.getXrefs().iterator().next());
        Collection<Xref> deleted = new ArrayList<>();
        deleted.add(protein.getXrefs().iterator().next());

        XrefUpdaterReport report = new XrefUpdaterReport(protein, created, deleted);
        Assert.assertTrue(report.isUpdated());
    }

}