package uk.ac.ebi.intact.dbupdate.dataset;

import org.junit.Assert;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.BioSource;
import uk.ac.ebi.intact.model.InteractorAlias;
import uk.ac.ebi.intact.model.Protein;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Jun-2010</pre>
 */

public class BasicDatasetTest extends IntactBasicTestCase {

    protected IntactContext intactContext;

    public void createProteinsHumanMouseAndRat(){
        BioSource human = getMockBuilder().createBioSource(9606, "human");
        BioSource mouse = getMockBuilder().createBioSource(10090, "mouse");
        BioSource rat = getMockBuilder().createBioSource(10116, "rat");

        intactContext.getCorePersister().saveOrUpdate(human);
        intactContext.getCorePersister().saveOrUpdate(mouse);
        intactContext.getCorePersister().saveOrUpdate(rat);

        Protein prot1 = getMockBuilder().createProtein("P01234", "amph_human", human);
        Protein prot2 = getMockBuilder().createProtein("P01235", "amph_mouse", mouse);
        Protein prot3 = getMockBuilder().createProtein("P01236", "amph_rat", rat);
        Protein prot4 = getMockBuilder().createProtein("P01237", "apba1_human", human);
        Protein prot5 = getMockBuilder().createProtein("P01238", "apba1_mouse", mouse);
        Protein prot6 = getMockBuilder().createProtein("P01239", "apba2_human", human);

        prot1.getAliases().iterator().next().setName("AMPH");
        prot2.getAliases().iterator().next().setName("AMPH");
        prot3.getAliases().iterator().next().setName("AMPH");
        prot4.getAliases().iterator().next().setName("APBA1");
        prot5.getAliases().iterator().next().setName("APBA1");
        prot6.getAliases().iterator().next().setName("APBA2");

        intactContext.getCorePersister().saveOrUpdate(prot1);
        intactContext.getCorePersister().saveOrUpdate(prot2);
        intactContext.getCorePersister().saveOrUpdate(prot3);
        intactContext.getCorePersister().saveOrUpdate(prot4);
        intactContext.getCorePersister().saveOrUpdate(prot5);
        intactContext.getCorePersister().saveOrUpdate(prot6);

        Assert.assertEquals(intactContext.getDaoFactory().getAliasDao(InteractorAlias.class).countAll(), 6);
    }

    public void setUpDatabase(){
        this.intactContext = IntactContext.getCurrentInstance();

        createProteinsHumanMouseAndRat();
    }
}
