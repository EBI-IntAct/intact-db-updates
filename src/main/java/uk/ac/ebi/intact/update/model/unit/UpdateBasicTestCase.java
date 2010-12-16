package uk.ac.ebi.intact.update.model.unit;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.test.context.transaction.TransactionConfiguration;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.update.persistence.CurationToolsDaoFactory;

import javax.persistence.EntityManager;
import javax.persistence.PersistenceContext;

/**
 * The class to extend for testing the Hibernate annotated classes
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20-May-2010</pre>
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/intact.spring.xml", "classpath*:/META-INF/standalone/jpa-standalone.spring.xml",
        "classpath*:/META-INF/standalone/intact-standalone.spring.xml",
        "/META-INF/standalone/update-jpa.spring.xml"})
@TransactionConfiguration( transactionManager = "updateTransactionManager" )
@Transactional
public abstract class UpdateBasicTestCase {

    /**
     * the daoFactory instance
     */
    @Autowired
    private CurationToolsDaoFactory daoFactory;

    /**
     * The entity manager
     */
    @PersistenceContext(unitName = "intact-update")    
    private EntityManager entityManager;

    /**
     * The CurationMockBuilder
     */
    private CurationMockBuilder mockBuilder;

    /**
     * Create a new MockBuilder before testing
     * @throws Exception
     */
    @Before
    public void prepareBasicTest() throws Exception {
        mockBuilder = new CurationMockBuilder();
    }

    /**
     * Unset the MockBuilder after testing
     * @throws Exception
     */
    @After
    public void afterBasicTest() throws Exception {
        mockBuilder = null;
    }

    /**
     * 
     * @return the entity manager
     */
    public EntityManager getEntityManager() {
        return entityManager;
    }

    /**
     *
     * @return the DaoFactory
     */
    public CurationToolsDaoFactory getDaoFactory() {
        return daoFactory;
    }

    /**
     *
     * @return The mockBuilder
     */
    public CurationMockBuilder getMockBuilder() {
        return mockBuilder;
    }
}
