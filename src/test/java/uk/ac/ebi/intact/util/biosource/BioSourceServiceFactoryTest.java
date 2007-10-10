package uk.ac.ebi.intact.util.biosource;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;

/**
 * BioSourceServiceFactory Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @since <pre>02/13/2007</pre>
 * @version 1.0
 */
public class BioSourceServiceFactoryTest extends TestCase {
    
    public BioSourceServiceFactoryTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite(BioSourceServiceFactoryTest.class);
    }

    ////////////////////
    // Tests
    
    public void testGetInstance() throws Exception {
        BioSourceServiceFactory factory = BioSourceServiceFactory.getInstance();
        BioSourceService service = factory.buildBioSourceService( new DummyTaxonomyService() );
        assertNotNull( service );

        try {
            factory.buildBioSourceService( null );
            fail();
        } catch ( Exception e ) {
            // ok
        }
    }

    public void testGetInstance_noParam() throws Exception {
        // Test Spring bean injection
        BioSourceServiceFactory factory = BioSourceServiceFactory.getInstance();
        BioSourceService service = factory.buildBioSourceService();
        assertNotNull( service );
    }
}
