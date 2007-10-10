package uk.ac.ebi.intact.util.protein;

import org.junit.Test;
import static org.junit.Assert.*;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.AbstractUniprotService;

import java.util.Collection;
import java.util.Map;

/**
 * ProteinLoaderServiceFactory Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version 1.0
 * @since <pre>02/09/2007</pre>
 */
public class ProteinServiceFactoryTest extends IntactBasicTestCase {

    ////////////////////
    // Tests

    @Test
    public void testGetInstance() throws Exception {
        ProteinServiceFactory f1 = ProteinServiceFactory.getInstance();
        assertNotNull( f1 );

        ProteinServiceFactory f2 = ProteinServiceFactory.getInstance();
        assertNotNull( f2 );

        // the factory is a singleton
        assertTrue( "The factory is not a singleton", f1 == f2 );
    }

    // implementation of the service for the sake of the test below.
    protected class DummyUniprotService extends AbstractUniprotService {
        public Collection<UniprotProtein> retrieve( String ac ) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public Collection<UniprotProtein> retreive(String s) {
            return retrieve(s);
        }

        public Map<String, Collection<UniprotProtein>> retrieve( Collection<String> acs ) {
            throw new UnsupportedOperationException();
        }

        @Deprecated
        public Map<String, Collection<UniprotProtein>> retreive(Collection<String> strings) {
            return retrieve(strings);
        }
    }

    @Test
    public void testBuildProteinService() {
        ProteinServiceFactory factory = ProteinServiceFactory.getInstance();
        assertNotNull( factory );

        ProteinService service = factory.buildProteinService();
        assertNotNull( service );

        ProteinService dummy = factory.buildProteinService( new DummyUniprotService() );
        assertNotNull( dummy );

        try {
            factory.buildProteinService( null );
            fail( "null is not allowed." );
        } catch ( Exception e ) {
            // ok
        }
    }
}
