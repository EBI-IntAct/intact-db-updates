package uk.ac.ebi.intact.update;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persister.CorePersister;
import uk.ac.ebi.intact.core.unit.IntactMockBuilder;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/standalone/update-jpa-standalone.spring.xml",
        "classpath*:/META-INF/db-update-test.spring.xml"
})
@Transactional("transactionManager")
@DirtiesContext
public abstract class IntactBasicTestCase {

    @Autowired
    private ApplicationContext applicationContext;

    private IntactMockBuilder mockBuilder;

    @Before
    public void prepareBasicTest() {
        mockBuilder = new IntactMockBuilder(getIntactContext().getConfig().getDefaultInstitution());
    }

    @After
    public void afterBasicTest() {
        mockBuilder = null;
    }

    protected IntactContext getIntactContext() {
        return (IntactContext) applicationContext.getBean("intactContext");
    }

    protected DataContext getDataContext() {
        return getIntactContext().getDataContext();
    }

    protected DaoFactory getDaoFactory() {
        return getDataContext().getDaoFactory();
    }

    protected IntactMockBuilder getMockBuilder() {
        return mockBuilder;
    }

    public CorePersister getCorePersister() {
        return getIntactContext().getCorePersister();
    }
}