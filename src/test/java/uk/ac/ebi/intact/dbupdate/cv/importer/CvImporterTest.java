package uk.ac.ebi.intact.dbupdate.cv.importer;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateManager;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdaterTest;

import java.io.IOException;

/**
 * Unit tester of CvImporter
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml"})
public class CvImporterTest extends IntactBasicTestCase{

    private CvUpdateManager cvManager;

    @Before
    public void clear() throws IOException, OntologyLoaderException {
        cvManager = new CvUpdateManager(CvUpdaterTest.class.getResource("/ontologies.xml"), "target/reports");
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_new_import_do_not_include_children(){

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_new_import_include_children(){

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_import_existing_parent(){

    }
}
