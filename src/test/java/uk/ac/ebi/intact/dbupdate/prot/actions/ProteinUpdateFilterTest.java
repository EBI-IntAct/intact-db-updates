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
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.ProteinUpdateFilterImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinMapperImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.uniprot.service.UniprotRemoteService;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Tester of the ProteinUpdateFilterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProteinUpdateFilterTest extends IntactBasicTestCase {

    ProteinUpdateFilterImpl filter;

    @Before
    public void before() throws Exception {
        filter = new ProteinUpdateFilterImpl(new UniprotProteinMapperImpl(new UniprotRemoteService()));
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        filter = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein is from uniprot with a unique uniprot identity : pass the filter
     */
    public void uniprot_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);

        String uniprotIdentity = filter.filterOnUniprotIdentity(evt);

        Assert.assertNotNull(uniprotIdentity);
        Assert.assertEquals(prot.getXrefs().iterator().next().getPrimaryId(), uniprotIdentity);

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }
    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein is 'no-uniprot-update', don't pass the filter
     */
    public void no_uniprot_update() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P12345", "test_no_uniprot");
        Annotation no_uniprot_update = getMockBuilder().createAnnotation(null, getMockBuilder().createCvObject(CvTopic.class, null, CvTopic.NON_UNIPROT));
        prot.addAnnotation(no_uniprot_update);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);

        String uniprotIdentity = filter.filterOnUniprotIdentity(evt);

        Assert.assertNull(uniprotIdentity);

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }


    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein doesn't have a uniprot identity, doesn't pass the filter
     */
    public void no_uniprot_identity() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        InteractorXref ref = prot.getXrefs().iterator().next();
        ref.setCvDatabase(getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);
        
        Assert.assertFalse(ProteinTools.hasUniqueDistinctUniprotIdentity(prot));

        ProteinProcessor processor = new ProteinUpdateProcessor();
        ProteinEvent evt = new ProteinEvent(processor, IntactContext.getCurrentInstance().getDataContext(), prot);

        String uniprotIdentity = filter.filterOnUniprotIdentity(evt);

        Assert.assertNull(uniprotIdentity);

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein has several uniprot identities, doesn't pass the filter
     */
    public void several_Uniprot_identities() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        prot.addXref(getMockBuilder().createIdentityXref(prot, "P12345", getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT)));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot);
        
        Assert.assertEquals(2, ProteinTools.getAllUniprotIdentities(prot).size());

        ProteinProcessor processor = new ProteinUpdateProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);

        String uniprotIdentity = filter.filterOnUniprotIdentity(evt);

        Assert.assertNull(uniprotIdentity);

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein is from uniprot with a unique uniprot identity : pass the filter
     */
    public void filter_uniprot_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2, prot3);

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot, "Pxxx1-1", "splice1");
        Protein prot5 = getMockBuilder().createProteinSpliceVariant(prot2, "Pxxx2-2", "splice2");
        Protein prot6 = getMockBuilder().createProteinChain(prot3, "Pxxx3-1", "chain1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot4, prot5, prot6);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(prot);
        primaryProteins.add(prot3);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(prot2);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(prot4, null));
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot5, null));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(prot6, null));

        ProteinProcessor processor = new ProteinUpdateProcessor();

        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), null, primaryProteins,
                secondaryProteins, primaryIsoforms, secondaryIsoforms, primaryChains, "P12345");

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        filter.filterNonUniprotAndMultipleUniprot(evt);

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }
    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein is 'no-uniprot-update', don't pass the filter
     */
    public void filter_no_uniprot_update() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProtein("P12345", "test_no_uniprot");
        Annotation no_uniprot_update = getMockBuilder().createAnnotation(null, getMockBuilder().createCvObject(CvTopic.class, null, CvTopic.NON_UNIPROT));
        prot.addAnnotation(no_uniprot_update);
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2, prot3);

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot, "Pxxx1-1", "splice1");
        prot4.addAnnotation(no_uniprot_update);
        Protein prot5 = getMockBuilder().createProteinSpliceVariant(prot2, "Pxxx2-2", "splice2");
        Protein prot6 = getMockBuilder().createProteinChain(prot3, "Pxxx3-1", "chain1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot4, prot5, prot6);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(prot);
        primaryProteins.add(prot3);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(prot2);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(prot4, null));
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot5, null));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(prot6, null));

        ProteinProcessor processor = new ProteinUpdateProcessor();

        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), null, primaryProteins,
                secondaryProteins, primaryIsoforms, secondaryIsoforms, primaryChains, "P12345");

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        filter.filterNonUniprotAndMultipleUniprot(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }


    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein doesn't have a uniprot identity, doesn't pass the filter
     */
    public void filter_no_uniprot_identity() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        InteractorXref ref = prot2.getXrefs().iterator().next();
        ref.setCvDatabase(getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT));
        Assert.assertFalse(ProteinTools.hasUniqueDistinctUniprotIdentity(prot2));

        Protein prot3 = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2, prot3);

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot, "Pxxx1-1", "splice1");
        Protein prot5 = getMockBuilder().createProteinSpliceVariant(prot2, "Pxxx2-2", "splice2");
        InteractorXref ref2 = prot5.getXrefs().iterator().next();
        ref2.setCvDatabase(getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT));
        Assert.assertFalse(ProteinTools.hasUniqueDistinctUniprotIdentity(prot5));

        Protein prot6 = getMockBuilder().createProteinChain(prot3, "Pxxx3-1", "chain1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot4, prot5, prot6);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(prot);
        primaryProteins.add(prot3);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(prot2);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(prot4, null));
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot5, null));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(prot6, null));

        ProteinProcessor processor = new ProteinUpdateProcessor();

        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), null, primaryProteins,
                secondaryProteins, primaryIsoforms, secondaryIsoforms, primaryChains, "P12345");

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        filter.filterNonUniprotAndMultipleUniprot(evt);

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)    
    /**
     * The protein has several uniprot identities, doesn't pass the filter
     */
    public void filter_several_Uniprot_identities() throws Exception{

        TransactionStatus status = getDataContext().beginTransaction();

        Protein prot = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();
        prot3.addXref(getMockBuilder().createIdentityXref(prot3, "P12345", getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT)));
        Assert.assertEquals(2, ProteinTools.getAllUniprotIdentities(prot3).size());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot, prot2, prot3);

        Protein prot4 = getMockBuilder().createProteinSpliceVariant(prot, "Pxxx1-1", "splice1");
        Protein prot5 = getMockBuilder().createProteinSpliceVariant(prot2, "Pxxx2-2", "splice2");
        Protein prot6 = getMockBuilder().createProteinChain(prot3, "Pxxx3-1", "chain1");
        prot6.addXref(getMockBuilder().createIdentityXref(prot6, "P12345", getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT)));
        Assert.assertEquals(2, ProteinTools.getAllUniprotIdentities(prot6).size());

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(prot4, prot5, prot6);

        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(prot);
        primaryProteins.add(prot3);
        Collection<Protein> secondaryProteins = new ArrayList<Protein>();
        secondaryProteins.add(prot2);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(prot4, null));
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(prot5, null));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(prot6, null));

        ProteinProcessor processor = new ProteinUpdateProcessor();

        UpdateCaseEvent evt = new UpdateCaseEvent(processor, IntactContext.getCurrentInstance().getDataContext(), null, primaryProteins,
                secondaryProteins, primaryIsoforms, secondaryIsoforms, primaryChains, "P12345");

        Assert.assertEquals(2, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());

        filter.filterNonUniprotAndMultipleUniprot(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getSecondaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(0, evt.getPrimaryFeatureChains().size());

        getDataContext().commitTransaction(status);
    }
}
