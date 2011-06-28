package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.ProteinDeleterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

/**
 * Tester of ProteinDeleterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProteinDeleterTest extends IntactBasicTestCase {

    private ProteinDeleterImpl deleter;

    @Before
    public void before_schema() throws Exception {
        deleter = new ProteinDeleterImpl();

        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        deleter = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * A protein to delete
     */
    public void delete_protein() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // interaction: no
        Protein masterProt = getMockBuilder().createProtein("P12345", "master");
        masterProt.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt);

        String ac = masterProt.getAc();

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        deleter.delete(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), masterProt));

        getDataContext().commitTransaction(status);

        Assert.assertNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(ac));
    }
}
