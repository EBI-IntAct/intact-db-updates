package uk.ac.ebi.intact.dbupdate.dataset;

import org.junit.Assert;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;

import java.util.Collection;

/**
 * Abstract class to enter data for testing
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>10-Jun-2010</pre>
 */

public abstract class BasicDatasetTest extends IntactBasicTestCase {

    protected IntactContext intactContext;
    protected Protein prot1;
    protected Protein prot2;
    protected Protein prot3;
    protected Protein prot4;
    protected Protein prot5;
    protected Protein prot6;
    protected Protein prot7;
    protected Publication p1;
    protected Publication p2;
    protected Publication p3;

    private void createInterproXRefs(){

        InteractorXref x = getMockBuilder().createXref(prot1, "IPR001564", null, this.intactContext.getDataContext().getDaoFactory().getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTERPRO_MI_REF ));
        prot1.addXref(x);

        this.intactContext.getCorePersister().saveOrUpdate(prot1);
    }

    public void createProteinsHumanMouseAndRat(){
        BioSource human = getMockBuilder().createBioSource(9606, "human");
        BioSource mouse = getMockBuilder().createBioSource(10090, "mouse");
        BioSource rat = getMockBuilder().createBioSource(10116, "rat");

        intactContext.getCorePersister().saveOrUpdate(human);
        intactContext.getCorePersister().saveOrUpdate(mouse);
        intactContext.getCorePersister().saveOrUpdate(rat);

        prot1 = getMockBuilder().createProtein("P01234", "amph_human", human);
        prot2 = getMockBuilder().createProtein("P01235", "amph_mouse", mouse);
        prot3 = getMockBuilder().createProtein("P01236", "amph_rat", rat);
        prot4 = getMockBuilder().createProtein("P01237", "apba1_human", human);
        prot5 = getMockBuilder().createProtein("P01238", "apba1_mouse", mouse);
        prot6 = getMockBuilder().createProtein("P01239", "apba2_human", human);
        prot7 = getMockBuilder().createProtein("Q00005", "2abb_human", human);

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
        intactContext.getCorePersister().saveOrUpdate(prot6, prot7);

        Assert.assertEquals(intactContext.getDaoFactory().getAliasDao(InteractorAlias.class).countAll(), 7);
    }

    public void createExperimentsWithProteinOfInterest(){
        Experiment exp1 = getMockBuilder().createExperimentRandom("amph_2020_1",10);
        Experiment exp2 = getMockBuilder().createExperimentRandom("apba1_2010_1", 3);
        Experiment exp3 = getMockBuilder().createExperimentRandom("apba2_2010_1", 7);
        Experiment exp4 = getMockBuilder().createExperimentRandom("apba2_2010_2", 2);
        Experiment exp5 = getMockBuilder().createExperimentRandom("amph_2020_2",5);

        p1 = getMockBuilder().createPublication("123456789");
        p2 = getMockBuilder().createPublication("123456790");
        p3 = getMockBuilder().createPublication("123456791");

        exp1.setPublication(p1);
        exp1.getXrefs().iterator().next().setPrimaryId(p1.getPublicationId());
        Collection<Component> components1 = exp1.getInteractions().iterator().next().getComponents();
        components1.iterator().next().setInteractor(prot1);
        exp2.setPublication(p2);
        exp2.getXrefs().iterator().next().setPrimaryId(p2.getPublicationId());
        Collection<Component> components2 = exp2.getInteractions().iterator().next().getComponents();
        components2.iterator().next().setInteractor(prot4);
        exp3.setPublication(p3);
        exp3.getXrefs().iterator().next().setPrimaryId(p3.getPublicationId());
        Collection<Component> components3 = exp3.getInteractions().iterator().next().getComponents();
        components3.iterator().next().setInteractor(prot6);
        exp4.setPublication(p3);
        exp4.getXrefs().iterator().next().setPrimaryId(p3.getPublicationId());
        Collection<Component> components4 = exp4.getInteractions().iterator().next().getComponents();
        components4.iterator().next().setInteractor(prot6);
        exp5.setPublication(p1);
        exp5.getXrefs().iterator().next().setPrimaryId(p1.getPublicationId());

        this.intactContext.getCorePersister().saveOrUpdate(exp1);
        this.intactContext.getCorePersister().saveOrUpdate(exp2);
        this.intactContext.getCorePersister().saveOrUpdate(exp3);
        this.intactContext.getCorePersister().saveOrUpdate(exp4);
        this.intactContext.getCorePersister().saveOrUpdate(exp5);
    }

    public void createDatasetCVTopic(){
        CvTopic topic = getMockBuilder().createCvObject(CvTopic.class, CvTopic.DATASET_MI_REF, CvTopic.DATASET);
        this.intactContext.getCorePersister().saveOrUpdate(topic);
    }

    public void createCVXRefs(){
        CvDatabase interpro = getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTERPRO_MI_REF, CvDatabase.INTERPRO);

        this.intactContext.getCorePersister().saveOrUpdate(interpro);
    }

    public void setUpDatabase(){
        this.intactContext = IntactContext.getCurrentInstance();

        createProteinsHumanMouseAndRat();
        createExperimentsWithProteinOfInterest();
        createDatasetCVTopic();
        createCVXRefs();
        createInterproXRefs();
    }
}
