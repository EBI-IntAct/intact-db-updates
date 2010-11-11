package uk.ac.ebi.intact.dbupdate.prot.listener;

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.ProteinUpdateFilter;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;

/**
 * Tester of the ProteinUpdateFilter
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>05-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class UniprotUpdateFilterListenerTest  extends IntactBasicTestCase {

    private class DummyProcessor extends ProteinProcessor {
        protected void registerListeners() {
        }
    }

    @Test
    @DirtiesContext
    /**
     * The protein is 'no-uniprot-update', don't pass the filter
     */
    public void onPreProcess_no_uniprot_update() throws Exception{
        Protein prot = getMockBuilder().createProtein("P12345", "test_no_uniprot");
        Annotation no_uniprot_update = getMockBuilder().createAnnotation(null, getMockBuilder().createCvObject(CvTopic.class, null, CvTopic.NON_UNIPROT));
        prot.addAnnotation(no_uniprot_update);

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        Assert.assertNull(evt.getUniprotIdentity());

        ProteinUpdateFilter listener = new ProteinUpdateFilter();
        listener.filterOnUniprotIdentity(evt);

        Assert.assertNull(evt.getUniprotIdentity());
        Assert.assertTrue(processor.isFinalizationRequested());
    }

    @Test
    @DirtiesContext
    /**
     * The protein doesn't have a uniprot identity, doesn't pass the filter
     */
    public void onPreProcess_no_uniprot_identity() throws Exception{
        Protein prot = getMockBuilder().createProteinRandom();
        InteractorXref ref = prot.getXrefs().iterator().next();
        ref.setCvDatabase(getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT));

        Assert.assertFalse(ProteinTools.hasUniqueDistinctUniprotIdentity(prot));

        ProteinProcessor processor = new DummyProcessor();
        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        Assert.assertNull(evt.getUniprotIdentity());

        ProteinUpdateFilter listener = new ProteinUpdateFilter();
        listener.filterOnUniprotIdentity(evt);

        Assert.assertNull(evt.getUniprotIdentity());
        Assert.assertTrue(processor.isFinalizationRequested());
    }

    @Test
    @DirtiesContext
    /**
     * The protein has several uniprot identities, doesn't pass the filter
     */
    public void onPreProcess_several_Uniprot_identities() throws Exception{
        Protein prot = getMockBuilder().createProteinRandom();
        prot.addXref(getMockBuilder().createIdentityXref(prot, "P12345", getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT)));

        Assert.assertEquals(2, ProteinTools.getAllUniprotIdentities(prot).size());

        ProteinProcessor processor = new DummyProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        Assert.assertNull(evt.getUniprotIdentity());

        ProteinUpdateFilter listener = new ProteinUpdateFilter();
        listener.filterOnUniprotIdentity(evt);

        Assert.assertNull(evt.getUniprotIdentity());
        Assert.assertTrue(processor.isFinalizationRequested());
    }

    @Test
    @DirtiesContext
    /**
     * The protein is from uniprot with a unique uniprot identity : pass the filter
     */
    public void onPreProcess_one_Uniprot_identity() throws Exception{
        Protein prot = getMockBuilder().createProteinRandom();

        ProteinProcessor processor = new DummyProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);
        Assert.assertNull(evt.getUniprotIdentity());

        ProteinUpdateFilter listener = new ProteinUpdateFilter();
        listener.filterOnUniprotIdentity(evt);

        Assert.assertNotNull(evt.getUniprotIdentity());
        Assert.assertEquals(prot.getXrefs().iterator().next().getPrimaryId(), evt.getUniprotIdentity());
        Assert.assertFalse(processor.isFinalizationRequested());
    }
}
