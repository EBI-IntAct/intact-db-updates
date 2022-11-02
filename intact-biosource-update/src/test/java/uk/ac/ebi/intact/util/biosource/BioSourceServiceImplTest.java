package uk.ac.ebi.intact.util.biosource;

import org.junit.Assert;
import org.junit.Test;
import psidev.psi.mi.jami.bridges.fetcher.mock.MockOrganismFetcher;
import psidev.psi.mi.jami.model.Organism;
import psidev.psi.mi.jami.model.impl.DefaultOrganism;
import uk.ac.ebi.intact.model.BioSource;

/**
 * BioSourceServiceImpl Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @since 1.0
 */
public class BioSourceServiceImplTest {

    public static final int HUMAN_TAX_ID = 9606;
    public static final int NEW_TAX_ID = 9999999;

    @Test
    public void testGetBiosource_existingOne() throws Exception {

        Organism mockOrganism = new DefaultOrganism(HUMAN_TAX_ID);

        MockOrganismFetcher taxService = new MockOrganismFetcher();
        taxService.addEntry(String.valueOf(HUMAN_TAX_ID), mockOrganism);

        BioSourceService service = new BioSourceServiceImpl(taxService);
        BioSource bs = service.getBiosourceByTaxid(String.valueOf(HUMAN_TAX_ID));
        Assert.assertNotNull(bs);
    }

    @Test
    public void testGetBiosource_newBioSource() throws Exception {

        Organism mockOrganism = new DefaultOrganism(NEW_TAX_ID);

        MockOrganismFetcher taxService = new MockOrganismFetcher();
        taxService.addEntry(String.valueOf(NEW_TAX_ID), mockOrganism);

        BioSourceService service = new BioSourceServiceImpl(taxService);

        BioSource bs = service.getBiosourceByTaxid(String.valueOf(NEW_TAX_ID));
        Assert.assertNotNull(bs);

        BioSource bs2 = service.getBiosourceByTaxid(String.valueOf(NEW_TAX_ID));
        Assert.assertNotNull(bs2);

        Assert.assertEquals(bs.getTaxId(), bs2.getTaxId());
        Assert.assertEquals(bs.getShortLabel(), bs2.getShortLabel());
        Assert.assertEquals(bs.getFullName(), bs.getFullName());
    }


}