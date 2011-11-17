package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.CvDagObject;
import uk.ac.ebi.intact.model.CvTopic;

/**
 * Unit tester of the CvUpdater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml"})
public class CvUpdaterTest extends IntactBasicTestCase{

    private CvUpdater cvUpdater = new CvUpdater();

    @Before
    public void clear(){
        cvUpdater.clear();
    }

    @Test
    @DirtiesContext
    public void test_update_identifiers(){

        CvDagObject cv = getMockBuilder().createCvObject(CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
        cv.setIdentifier("IA:xxxxx");
        getCorePersister().saveOrUpdate(cv);
    }

}
