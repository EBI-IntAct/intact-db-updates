package uk.ac.ebi.intact.dbupdate.prot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>02-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"}  )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinUpdateProcessorTest3 extends IntactBasicTestCase {

    @Before
    public void before() throws Exception {

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        context.commitTransaction(status);
    }

    /**
     * One participant with range conflicts
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_create_bad_participant() throws Exception {
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        CvFeatureType type = getMockBuilder().createCvObject(CvFeatureType.class, CvFeatureType.EXPERIMENTAL_FEATURE_MI_REF, CvFeatureType.EXPERIMENTAL_FEATURE);
        getCorePersister().saveOrUpdate(type);

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);
        configUpdate.setGlobalProteinUpdate(true);

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12346", "dupe1");
        dupe1.getBioSource().setTaxId("10116");
        dupe1.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");

        getCorePersister().saveOrUpdate(dupe1);

        Protein prot1 = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        getCorePersister().saveOrUpdate(prot1, prot2, prot3);

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, prot1);
        Collection<Component> components = interaction1.getComponents();

        Range r = getMockBuilder().createRange(2, 2, 8, 8);
        Component c = null;
        for (Component comp : components){
            if (dupe1.equals(comp.getInteractor())){
                c = comp;
            }
        }

        c.getBindingDomains().clear();
        Feature f = getMockBuilder().createFeatureRandom();
        f.getRanges().clear();
        f.addRange(r);
        f.setComponent(c);
        c.addBindingDomain(f);
        getCorePersister().saveOrUpdate(interaction1, dupe1, prot1);

        Interaction interaction2 = getMockBuilder().createInteraction(dupe1, prot2);
        Collection<Component> components2 = interaction2.getComponents();

        Range r2 = getMockBuilder().createRange(2, 2, 8, 8);
        Component c2 = null;
        for (Component comp : components2){
            if (dupe1.equals(comp.getInteractor())){
                c2 = comp;
            }
        }

        c2.getBindingDomains().clear();
        Feature f2 = getMockBuilder().createFeatureRandom();
        f2.getRanges().clear();
        f2.addRange(r2);
        f2.setComponent(c2);
        c2.addBindingDomain(f2);
        getCorePersister().saveOrUpdate(interaction2, dupe1, prot2);

        Interaction interaction3 = getMockBuilder().createInteraction(dupe1, prot3);
        getCorePersister().saveOrUpdate(interaction3, dupe1, prot3);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        Assert.assertEquals(3, dupe1.getActiveInstances().size());

        boolean hasCautionBefore = false;

        for (Annotation a : dupe1.getAnnotations()){
            if (a.getCvTopic().getIdentifier().equals(CvTopic.CAUTION_MI_REF)){
                hasCautionBefore = true;
            }
        }

        Assert.assertFalse(hasCautionBefore);

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe1.getAc());
        Assert.assertNotNull(dupe2FromDb);
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());

        context2.commitTransaction(status2);
    }

}
