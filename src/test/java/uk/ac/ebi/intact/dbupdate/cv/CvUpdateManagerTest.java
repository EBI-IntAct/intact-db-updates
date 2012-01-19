package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;

/**
 * Unit tester of CvUpdateManager
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beans.spring.xml"})
public class CvUpdateManagerTest extends IntactBasicTestCase{

    @Autowired
    CvUpdateManager cvManager;

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_update_cv(){

    }

}
