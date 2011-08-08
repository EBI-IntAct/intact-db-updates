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
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotIdentityUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

/**
 * Tester of UniprotIdentityUpdaterTest
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>12-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotIdentityUpdaterTest extends IntactBasicTestCase {

    private UniprotIdentityUpdaterImpl updater;

    @Before
    public void setUp(){
        updater = new UniprotIdentityUpdaterImpl();
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        updater = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create a protein for each primary ac and secondary acs of cdc42_human (1 primary, 3 secondary)
     * Create a protein for each isoform  of cdc42_human (2 primary, 3 secondary)
     * Create a protein for a feature chain
     * Collect all these proteins and update the secondary ac
     *
     */
    public void collect_and_update_cdc42_human(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        Protein secondary1 = getMockBuilder().createProtein("P21181", "secondary1");
        secondary1.getBioSource().setTaxId("9606");
        Protein secondary2 = getMockBuilder().createProtein("P25763", "secondary2");
        secondary2.getBioSource().setTaxId("9606");
        Protein secondary3 = getMockBuilder().createProtein("Q7L8R5", "secondary3");
        secondary3.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary, secondary1, secondary2, secondary3);

        Protein isoformPrimary1 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoformPrimary1");
        isoformPrimary1.getBioSource().setTaxId("9606");
        Protein isoformSecondary1 = getMockBuilder().createProteinSpliceVariant(primary, "P21181-1", "isoformSecondary1");
        isoformSecondary1.getBioSource().setTaxId("9606");
        Protein isoformPrimary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P60953-2", "isoformPrimary2");
        isoformPrimary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P21181-4", "isoformSecondary2");
        isoformSecondary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2_2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P00012-2", "isoformSecondary2_2");
        isoformSecondary2_2.getBioSource().setTaxId("9606");
        Protein chain1 = getMockBuilder().createProteinChain(secondary2, "PRO-1", "chain1");
        chain1.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoformPrimary1, isoformSecondary1, isoformPrimary2, isoformSecondary2, isoformSecondary2_2, chain1);

        // collect
        ProteinEvent evt = new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary);
        evt.setUniprotIdentity("P60953");
        evt.setUniprotProtein(uniprot);
        UpdateCaseEvent caseEvt = updater.collectPrimaryAndSecondaryProteins(evt);

        Assert.assertEquals(caseEvt.getQuerySentToService(), "P60953");
        Assert.assertEquals(1, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(3, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(2, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(3, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());
        Assert.assertEquals(10, caseEvt.getProteins().size());

        // update
        updater.updateAllSecondaryProteins(caseEvt);
        Assert.assertEquals(4, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(0, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(5, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());

        Assert.assertEquals(ProteinUtils.getUniprotXref(secondary1).getPrimaryId(), "P60953");
        Assert.assertEquals(ProteinUtils.getUniprotXref(secondary2).getPrimaryId(), "P60953");
        Assert.assertEquals(ProteinUtils.getUniprotXref(secondary3).getPrimaryId(), "P60953");
        Assert.assertEquals(ProteinUtils.getUniprotXref(isoformSecondary1).getPrimaryId(), "P60953-1");
        Assert.assertEquals(ProteinUtils.getUniprotXref(isoformSecondary2).getPrimaryId(), "P60953-2");
        Assert.assertEquals(ProteinUtils.getUniprotXref(isoformSecondary2_2).getPrimaryId(), "P60953-2");

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create 3 proteins for cdc42_human (1 primary, 2 secondary) and  one protein which is not in uniprot
     * Create a protein for each isoform  of cdc42_human (2 primary, 3 secondary).
     * Create a protein for a feature chain but is linked to the master protein which is not in uniprot
     * Collect all these proteins and update the secondary ac : the protein not in uniprot should be ignored and the chain
     * attached to it as well
     *
     */
    public void collect_cdc42_human_excludeTranscript_parentExcluded(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        Protein secondary1 = getMockBuilder().createProtein("P21181", "secondary1");
        secondary1.getBioSource().setTaxId("9606");
        Protein prot = getMockBuilder().createProtein("P12345", "protein not updated");
        prot.getBioSource().setTaxId("9606");
        Protein secondary3 = getMockBuilder().createProtein("Q7L8R5", "secondary3");
        secondary3.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary, secondary1, prot, secondary3);

        Protein isoformPrimary1 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoformPrimary1");
        isoformPrimary1.getBioSource().setTaxId("9606");
        Protein isoformSecondary1 = getMockBuilder().createProteinSpliceVariant(primary, "P21181-1", "isoformSecondary1");
        isoformSecondary1.getBioSource().setTaxId("9606");
        Protein isoformPrimary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P60953-2", "isoformPrimary2");
        isoformPrimary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P21181-4", "isoformSecondary2");
        isoformSecondary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2_2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P00012-2", "isoformSecondary2_2");
        isoformSecondary2_2.getBioSource().setTaxId("9606");
        Protein chain1 = getMockBuilder().createProteinChain(prot, "PRO-1", "chain1");
        chain1.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoformPrimary1, isoformSecondary1, isoformPrimary2, isoformSecondary2, isoformSecondary2_2, chain1);

        // collect
        ProteinEvent evt = new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary);
        evt.setUniprotIdentity("P60953");
        evt.setUniprotProtein(uniprot);
        UpdateCaseEvent caseEvt = updater.collectPrimaryAndSecondaryProteins(evt);

        Assert.assertEquals(caseEvt.getQuerySentToService(), "P60953");
        Assert.assertEquals(1, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(2, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(2, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(3, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getPrimaryFeatureChains().size());
        Assert.assertEquals(8, caseEvt.getProteins().size());

        // update
        updater.updateAllSecondaryProteins(caseEvt);
        Assert.assertEquals(3, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(0, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(5, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getPrimaryFeatureChains().size());

        Assert.assertEquals(ProteinUtils.getUniprotXref(prot).getPrimaryId(), "P12345");
        Assert.assertEquals(ProteinUtils.getUniprotXref(chain1).getPrimaryId(), "PRO-1");

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create 4 proteins for cdc42_human (1 primary, 3 secondary)
     * Create a protein for each isoform  of cdc42_human (2 primary, 3 secondary). Create another isoform which is not in uniprot
     * and doesn't have 'no-uniprot-update'
     * Create a protein for a feature chain
     * Collect all these proteins and update the secondary ac : the isoform not in uniprot should not be added
     *
     */
    public void collect_cdc42_human_excludeTranscript_notInUniprot(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        Protein secondary1 = getMockBuilder().createProtein("P21181", "secondary1");
        secondary1.getBioSource().setTaxId("9606");
        Protein secondary2 = getMockBuilder().createProtein("P25763", "secondary2");
        secondary2.getBioSource().setTaxId("9606");
        Protein secondary3 = getMockBuilder().createProtein("Q7L8R5", "secondary3");
        secondary3.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary, secondary1, secondary2, secondary3);

        Protein isoformPrimary1 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoformPrimary1");
        isoformPrimary1.getBioSource().setTaxId("9606");
        Protein isoformSecondary1 = getMockBuilder().createProteinSpliceVariant(primary, "P21181-1", "isoformSecondary1");
        isoformSecondary1.getBioSource().setTaxId("9606");
        Protein isoformPrimary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P60953-2", "isoformPrimary2");
        isoformPrimary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P21181-4", "isoformSecondary2");
        isoformSecondary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2_2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P00012-2", "isoformSecondary2_2");
        isoformSecondary2_2.getBioSource().setTaxId("9606");
        Protein isoformSecondary3 = getMockBuilder().createProteinSpliceVariant(secondary3, "P12345-1", "isoformSecondary3");
        isoformSecondary3.getBioSource().setTaxId("9606");
        Protein chain1 = getMockBuilder().createProteinChain(secondary2, "PRO-1", "chain1");
        chain1.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoformPrimary1, isoformSecondary1, isoformPrimary2, isoformSecondary2, isoformSecondary2_2, isoformSecondary3, chain1);

        // collect
        ProteinEvent evt = new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary);
        evt.setUniprotIdentity("P60953");
        evt.setUniprotProtein(uniprot);
        UpdateCaseEvent caseEvt = updater.collectPrimaryAndSecondaryProteins(evt);

        Assert.assertEquals(caseEvt.getQuerySentToService(), "P60953");
        Assert.assertEquals(1, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(3, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(2, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(3, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());
        Assert.assertEquals(10, caseEvt.getProteins().size());

        // update
        updater.updateAllSecondaryProteins(caseEvt);
        Assert.assertEquals(4, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(0, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(5, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());

        Assert.assertEquals(ProteinUtils.getUniprotXref(isoformSecondary3).getPrimaryId(), "P12345-1");

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create 4 proteins for cdc42_human (1 primary, 3 secondary)
     * Create a protein for each isoform  of cdc42_human (2 primary, 3 secondary). Create another isoform which is not in uniprot
     * but does have 'no-uniprot-update'
     * Create a protein for a feature chain
     * Collect all these proteins and update the secondary ac : the isoform not in uniprot but with no-uniprot-update
     * is not updated and is ignored. It will be updated later
     *
     */
    public void collect_cdc42_human_addTranscript_notInUniprot_and_noUniProtUpdate(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));
        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.getBioSource().setTaxId("9606");
        Protein secondary1 = getMockBuilder().createProtein("P21181", "secondary1");
        secondary1.getBioSource().setTaxId("9606");
        Protein secondary2 = getMockBuilder().createProtein("P25763", "secondary2");
        secondary2.getBioSource().setTaxId("9606");
        Protein secondary3 = getMockBuilder().createProtein("Q7L8R5", "secondary3");
        secondary3.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary, secondary1, secondary2, secondary3);

        Protein isoformPrimary1 = getMockBuilder().createProteinSpliceVariant(primary, "P60953-1", "isoformPrimary1");
        isoformPrimary1.getBioSource().setTaxId("9606");
        Protein isoformSecondary1 = getMockBuilder().createProteinSpliceVariant(primary, "P21181-1", "isoformSecondary1");
        isoformSecondary1.getBioSource().setTaxId("9606");
        Protein isoformPrimary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P60953-2", "isoformPrimary2");
        isoformPrimary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P21181-4", "isoformSecondary2");
        isoformSecondary2.getBioSource().setTaxId("9606");
        Protein isoformSecondary2_2 = getMockBuilder().createProteinSpliceVariant(secondary1, "P00012-2", "isoformSecondary2_2");
        isoformSecondary2_2.getBioSource().setTaxId("9606");

        Protein isoformSecondary3 = getMockBuilder().createProteinSpliceVariant(secondary3, "P12345-1", "isoformSecondary3");
        isoformSecondary3.getBioSource().setTaxId("9606");
        Annotation noUniprotUpdate = getMockBuilder().createAnnotation(null, getMockBuilder().createCvObject(CvTopic.class, null, CvTopic.NON_UNIPROT));
        isoformSecondary3.addAnnotation(noUniprotUpdate);

        Protein chain1 = getMockBuilder().createProteinChain(secondary2, "PRO-1", "chain1");
        chain1.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoformPrimary1, isoformSecondary1, isoformPrimary2, isoformSecondary2, isoformSecondary2_2, isoformSecondary3, chain1);

        // collect
        ProteinEvent evt = new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary);
        evt.setUniprotIdentity("P60953");
        evt.setUniprotProtein(uniprot);
        UpdateCaseEvent caseEvt = updater.collectPrimaryAndSecondaryProteins(evt);

        Assert.assertEquals(caseEvt.getQuerySentToService(), "P60953");
        Assert.assertEquals(1, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(3, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(2, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(3, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());
        Assert.assertEquals(10, caseEvt.getProteins().size());

        // update
        // update
        updater.updateAllSecondaryProteins(caseEvt);
        Assert.assertEquals(4, caseEvt.getPrimaryProteins().size());
        Assert.assertEquals(0, caseEvt.getSecondaryProteins().size());
        Assert.assertEquals(5, caseEvt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, caseEvt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, caseEvt.getPrimaryFeatureChains().size());

        Assert.assertEquals(ProteinUtils.getUniprotXref(isoformSecondary3).getPrimaryId(), "P12345-1");

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }
}
