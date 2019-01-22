package uk.ac.ebi.intact.util.biosource;

import org.junit.Assert;
import org.junit.Test;
import psidev.psi.mi.jami.bridges.fetcher.mock.MockOrganismFetcher;

/**
 * BioSourceServiceFactory Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @since <pre>02/13/2007</pre>
 * @version 1.0
 */
public class BioSourceServiceFactoryTest {

    @Test
    public void testGetInstance() throws Exception {
        BioSourceServiceFactory factory = BioSourceServiceFactory.getInstance();
        BioSourceService service = factory.buildBioSourceService( new MockOrganismFetcher() );
        Assert.assertNotNull( service );

        try {
            factory.buildBioSourceService( null );
            Assert.fail();
        } catch ( Exception e ) {
            // ok
        }
    }

    @Test
    public void testGetInstance_noParam() throws Exception {
        // Test Spring bean injection
        BioSourceServiceFactory factory = BioSourceServiceFactory.getInstance();
        BioSourceService service = factory.buildBioSourceService();
        Assert.assertNotNull( service );
    }
}
