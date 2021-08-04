package uk.ac.ebi.intact.dbupdate.prot.referencefilter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.uniprot.service.DefaultCrossReferenceFilter;
import uk.ac.ebi.intact.uniprot.service.referenceFilter.CrossReferenceFilter;

/**
 * IntactCrossReferenceSelector Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>10/19/2006</pre>
 */
public class IntactCrossReferenceFilterTest {

    private CrossReferenceFilter filter;

    @Before
    public void before() {
       filter = new DefaultCrossReferenceFilter();
    }

    @After
    public void after() {
        filter = null;
    }

    @Test
    public void isSelected() {
        Assert.assertTrue( filter.isSelected( "GO" ) );
        Assert.assertTrue( filter.isSelected( "go" ) );
        Assert.assertTrue( filter.isSelected( "Go" ) );
        Assert.assertTrue( filter.isSelected( " gO " ) );
        Assert.assertTrue( filter.isSelected( "interpro" ) );
        Assert.assertTrue( filter.isSelected( "SGD" ) );
        Assert.assertTrue( filter.isSelected( "FlyBase" ) );
        Assert.assertTrue( filter.isSelected( "Refseq" ) );
        Assert.assertTrue( filter.isSelected( "refseq" ) );
        Assert.assertTrue( filter.isSelected( "reactome" ) );
        Assert.assertTrue( filter.isSelected( "Reactome" ) );

        Assert.assertFalse( filter.isSelected( "GOGO" ) );
        Assert.assertFalse( filter.isSelected( "gogo" ) );
        Assert.assertFalse( filter.isSelected( "foobar" ) );
    }

    @Test (expected = IllegalArgumentException.class)
    public void isSelected_null() throws Exception {
        filter.isSelected( null );
    }

    @Test (expected = IllegalArgumentException.class)
    public void isSelected_empty() throws Exception {
        filter.isSelected( "" );
    }
}