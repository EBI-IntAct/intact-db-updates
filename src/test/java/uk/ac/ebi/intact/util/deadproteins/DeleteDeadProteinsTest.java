package uk.ac.ebi.intact.util.deadproteins;

import junit.framework.Test;
import junit.framework.TestCase;
import junit.framework.TestSuite;

/**
 * DeleteDeadProteins Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>11/16/2006</pre>
 */
public class DeleteDeadProteinsTest extends TestCase {

    public DeleteDeadProteinsTest( String name ) {
        super( name );
    }

    public void setUp() throws Exception {
        super.setUp();
    }

    public void tearDown() throws Exception {
        super.tearDown();
    }

    public static Test suite() {
        return new TestSuite( DeleteDeadProteinsTest.class );
    }

    ////////////////////
    // Tests

    public void testFoo(){
        //fake test to JUnit doesn't whinge 
    }

//    public void testWithFileFilter() throws Exception {
//        File fileFilter = new File( DeleteDeadProteinsTest.class.getResource( "/predict/smallProteinFilter.txt" ).getFile() );
//        assertNotNull( fileFilter );
//
//        DeleteDeadProteins deleteDeadProteins = new DeleteDeadProteins( fileFilter, false ); // dryRun is Active
//        int proteinCount = deleteDeadProteins.process();
//        assertEquals( 7, proteinCount );
//    }
//
//    public void testOnWholeDatabase() throws Exception {
//
//        DeleteDeadProteins deleteDeadProteins = new DeleteDeadProteins( true ); // dryRun is Active
//        int proteinCount = deleteDeadProteins.process();
//        assertTrue( "Expected to have at least 10,000 proteins deleted", 10000 < proteinCount );
//    }
//
//    public void testDeleteProtein() throws IntactTransactionException {
//
//        final String ac = "EBI-81334";
//        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
//
//
//        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
//        ProteinDao pdao = daoFactory.getProteinDao();
//        ProteinImpl protein = pdao.getByAc( ac );
//
//        if ( protein != null ) {
//            pdao.delete( protein );
//        } else {
//            System.err.println( "ERROR - Could not find protein by ac: " + ac );
//        }
//
//        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
//    }
}
