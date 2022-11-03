package uk.ac.ebi.intact;

import org.junit.After;
import org.junit.Before;
import org.junit.BeforeClass;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persister.CorePersister;
import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = { "classpath*:/META-INF/dbupdate.spring.xml" })
@Transactional("transactionManager")
public abstract class IntactBasicTestCase {

    @Autowired
    private ApplicationContext applicationContext;

    private IntactMockBuilder mockBuilder;

    @BeforeClass
    public static void setDefaultProteinUpdateProcessorConfig() {
        ProteinUpdateContext.getInstance().setConfig(new ProteinUpdateProcessorConfig());
    }

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