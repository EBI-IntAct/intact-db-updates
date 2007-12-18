package uk.ac.ebi.intact.util.biosource;

import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.model.BioSource;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.config.CvPrimer;
import uk.ac.ebi.intact.config.impl.SmallCvPrimer;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.context.IntactContext;
import org.junit.Test;
import org.junit.Assert;
import org.junit.Before;

/**
 * BioSourceServiceImpl Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @since 1.0
 */
public class BioSourceServiceImplTest extends IntactBasicTestCase {

    public class MyPrimer extends SmallCvPrimer {
        public MyPrimer( DaoFactory daoFactory ) {
            super( daoFactory );
        }

        public void createCVs() {
            super.createCVs();
            getCvObject( CvDatabase.class, CvDatabase.NEWT, CvDatabase.NEWT_MI_REF );
        }
    }

    @Before
    public void initializeDatabase() throws Exception {
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        new MyPrimer( getDaoFactory() ).createCVs();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testGetBiosource_existingOne() throws Exception {
        TaxonomyService taxService = new DummyTaxonomyService();
        BioSourceService service = new BioSourceServiceImpl( taxService );
        BioSource bs = service.getBiosourceByTaxid( String.valueOf( 9606 ) );
        Assert.assertNotNull( bs );
    }

    @Test
    public void testGetBiosource_newBioSource() throws Exception {
        TaxonomyService taxService = new DummyTaxonomyService();
        BioSourceService service = new BioSourceServiceImpl( taxService );
        BioSource bs = service.getBiosourceByTaxid( String.valueOf( 9999999 ) );
        Assert.assertNotNull( bs );

        BioSource bs2 = service.getBiosourceByTaxid( String.valueOf( 9999999 ) );
        Assert.assertNotNull( bs2 );

        Assert.assertEquals( bs.getTaxId(), bs2.getTaxId() );
        Assert.assertEquals( bs.getShortLabel(), bs2.getShortLabel() );
        Assert.assertEquals( bs.getFullName(), bs.getFullName());
    }
}