package uk.ac.ebi.intact.util.protein;

import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.AbstractUniprotService;

import java.util.Collection;
import java.util.Map;

import static org.junit.Assert.*;

/**
 * ProteinLoaderServiceFactory Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version 1.0
 * @since <pre>02/09/2007</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProteinServiceFactoryTest extends IntactBasicTestCase {

    ////////////////////
    // Tests

    @Test @DirtiesContext
    @Ignore
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

        public Collection<UniprotProtein> retrieve( String ac, boolean processSpliceVars ) {
            return null;
        }

        @Deprecated
        public Collection<UniprotProtein> retreive(String s) {
            return retrieve(s);
        }

        public Map<String, Collection<UniprotProtein>> retrieve( Collection<String> acs ) {
            throw new UnsupportedOperationException();
        }

        public Map<String, Collection<UniprotProtein>> retrieve( Collection<String> acs, boolean processSpliceVars ) {
            return null;
        }

        @Deprecated
        public Map<String, Collection<UniprotProtein>> retreive(Collection<String> strings) {
            return retrieve(strings);
        }

        @Override
        public Collection<UniprotProteinTranscript> retrieveProteinTranscripts(String ac) {
            return null;
        }

        @Override
        public Collection<UniprotSpliceVariant> retrieveSpliceVariant(String ac) {
            return null;
        }

        @Override
        public Collection<UniprotFeatureChain> retrieveFeatureChain(String ac) {
            return null;
        }

        @Override
        public void close() {
            //To change body of implemented methods use File | Settings | File Templates.
        }
    }

    @Test @DirtiesContext
    @Ignore
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
