package uk.ac.ebi.intact.util.deadproteins;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;
import uk.ac.ebi.intact.context.IntactContext;

/**
 * UpdateDeadProteins Tester.
 *
 * @author <Authors name>
 * @since <pre>11/17/2006</pre>
 * @version 1.0
 */
public class UpdateDeadProteinsTest extends TestCase {
    public UpdateDeadProteinsTest(String name) {
        super(name);
    }

    public void setUp() throws Exception {
        super.setUp();
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
    }

    public void tearDown() throws Exception {
        super.tearDown();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    public static Test suite() {
        return new TestSuite(UpdateDeadProteinsTest.class);
    }

    ////////////////////
    // Tests

    public void testNoTestInThatClassYet() {

    }

//    public void testParse() throws Exception {
//        File inputFile = new File( getClass().getResource( "/deadProteins/full-remapping.txt" ).getFile() );
//        assertNotNull( inputFile );
//
//        UpdateDeadProteins updator = new UpdateDeadProteins();
//        Map<String,Collection<RemappingEntry>> mapping = MappingParser.parse( inputFile );
//        assertEquals( 1038, mapping.size() );
//
//        // check an entry
//        Collection<RemappingEntry> entries = mapping.get( "Q9V8Z0" );
//        assertEquals( 1, entries.size() );
//        RemappingEntry entry = entries.iterator().next();
//        assertEquals( "Q9V8Z0", entry.getOriginalId() );
//        assertTrue( entry.getIdentifiers().contains( "Q7KIN0" ) );
//        assertTrue( entry.getIdentifiers().contains( "Q9V8Z0" ) );
//        assertEquals( 2, entry.getIdentifiers().size() );
//        assertEquals( "UPI00000765D5", entry.getUpi() );
//        assertEquals( 1446, entry.getSequenceLength() );
//    }
//
//    public void testUpdate() throws Exception {
//        File inputFile = new File( getClass().getResource( "/deadProteins/full-remapping.txt" ).getFile() );
//        assertNotNull( inputFile );
//
//        File proteinToUpdateFile = new File( inputFile.getParent() + File.separator + "protein-to-update.txt" );
//        if( proteinToUpdateFile.exists() ) {
//            proteinToUpdateFile.delete();
//        }
//
//        UpdateDeadProteins updator = new UpdateDeadProteins();
//        updator.setProteinsToUpdate( proteinToUpdateFile );
//        Map<String,Collection<RemappingEntry>> mapping = MappingParser.parse( inputFile );
//        updator.remapProteins( mapping );
//    }
}
