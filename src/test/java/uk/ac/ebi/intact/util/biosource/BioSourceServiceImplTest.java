package uk.ac.ebi.intact.util.biosource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.model.BioSource;

/**
 * BioSourceServiceImpl Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @since 1.0
 */
public class BioSourceServiceImplTest extends TestCase {

    public BioSourceServiceImplTest( String name ) {
        super( name );
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite( BioSourceServiceImplTest.class );
    }

    ////////////////////
    // Tests

    public void testGetBiosource_existingOne() throws Exception {
        TaxonomyService taxService = new DummyTaxonomyService();
        BioSourceService service = new BioSourceServiceImpl( taxService );
        BioSource bs = service.getBiosourceByTaxid( String.valueOf( 9606 ) );
        assertNotNull( bs );
    }

    public void testGetBiosource_newBioSource() throws Exception {
        TaxonomyService taxService = new DummyTaxonomyService();
        BioSourceService service = new BioSourceServiceImpl( taxService );
        BioSource bs = service.getBiosourceByTaxid( String.valueOf( 9999999 ) );
        assertNotNull( bs );

        BioSource bs2 = service.getBiosourceByTaxid( String.valueOf( 9999999 ) );
        assertNotNull( bs2 );

        assertEquals( bs.getTaxId(), bs2.getTaxId() );
        assertEquals( bs.getShortLabel(), bs2.getShortLabel() );
        assertEquals( bs.getFullName(), bs.getFullName());
    }
}