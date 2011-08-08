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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persister.CorePersister;
import uk.ac.ebi.intact.core.persister.CorePersisterImpl;
import uk.ac.ebi.intact.core.persister.finder.DefaultFinder;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.text.DateFormat;
import java.text.SimpleDateFormat;
import java.util.*;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class ProteinUpdateProcessor4Test extends IntactBasicTestCase {

    @Before
    public void before() throws Exception {

        DataContext context = getDataContext();

        TransactionStatus status = context.beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(context.getDaoFactory());
        primer.createCVs();

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void duplicates_found_isoforms_different_parents() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.getBioSource().setTaxId("9986"); // rabit

        Protein dupe2 = getMockBuilder().createDeterministicProtein("P12346", "dupe2");
        dupe1.getBioSource().setTaxId("9986"); // rabit

        getCorePersister().saveOrUpdate(dupe1, dupe2);

        Protein dupe1_1 = getMockBuilder().createProteinSpliceVariant(dupe1, "P12345-1", "p12345-1");
        dupe1_1.getBioSource().setTaxId("9986"); // rabit

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe1_2 = cloner.clone(dupe1_1);
        dupe1_2.setBioSource(dupe1_1.getBioSource()); // rabit
        dupe1_2.setCrc64(dupe1_1.getCrc64());
        ProteinUtils.getIdentityXrefs(dupe1_2).iterator().next().setPrimaryId("P12345-2");
        ProteinUtils.extractIsoformParentCrossReferencesFrom(dupe1_2).iterator().next().setPrimaryId("P12346");

        dupe1_2.setCreated(new Date(1)); // dupe2 is older

        Protein prot1 = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1_1, prot1);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe1_2, prot2);
        Interaction interaction3 = getMockBuilder().createInteraction(dupe1_1, prot3);
        Interaction interaction4 = getMockBuilder().createInteraction(dupe2, prot3);

        getCorePersister().saveOrUpdate(dupe1, dupe1_1, dupe1_2, interaction1, interaction2, interaction3, interaction4);

        Assert.assertEquals(7, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(4, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(8, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByCrcAndTaxId(dupe1_1.getCrc64(), dupe1_1.getBioSource().getTaxId()).size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345-1").size());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(7, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(4, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(8, getDaoFactory().getComponentDao().countAll());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(dupe1_1.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());
        Assert.assertNotNull(dupe2FromDb);
        Assert.assertEquals(1, dupe2FromDb.getActiveInstances().size());

        context2.commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateProteinWithNullBiosource() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein prot = getMockBuilder().createProtein("P42898", "riboflavin");
        prot.setBioSource(null);
        prot.setCrc64(null);
        prot.setSequence(null);

        final Interaction interaction = getMockBuilder().createInteraction(prot);

        getCorePersister().saveOrUpdate(interaction);

        Assert.assertNotNull(prot.getAc());

        context.commitTransaction(status);

        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateByACs(Arrays.asList(prot.getAc()));

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        final ProteinImpl refreshedProt = getDaoFactory().getProteinDao().getByAc(prot.getAc());

        Assert.assertNotNull(refreshedProt.getCrc64());
        Assert.assertNotNull(refreshedProt.getSequence());
        Assert.assertNotNull(refreshedProt.getBioSource());

        context2.commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_updateProteinChains_chainWithoutInteraction() throws Exception {
        DataContext context = getDataContext();
        // master protein with interaction, linked chain with no interaction ...
        TransactionStatus status = context.beginTransaction();

        Protein master = getMockBuilder().createProtein("P03362", "pol_htl1a");
        final Interaction interaction = getMockBuilder().createInteraction( master );
        getCorePersister().saveOrUpdate(interaction);
        Assert.assertNotNull(master.getAc());

        // now create a protein chain
        CvDatabase intact = getMockBuilder().createCvObject( CvDatabase.class, CvDatabase.INTACT_MI_REF, "intact");
        CvXrefQualifier chainParent = getMockBuilder().createCvObject( CvXrefQualifier.class, "MI:0951", "chain-parent");

        Protein chain = getMockBuilder().createProtein("P03362-PRO_0000038873", "Reverse transcriptase/ribonuclease H");

        chain.addXref( new InteractorXref( chain.getOwner(), intact, master.getAc(), chainParent ) );

        getCorePersister().saveOrUpdate(chain);

        context.commitTransaction(status);

        // Run update on both the master and the chain
        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor( config );

        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        final ProteinImpl refreshedMaster = getDaoFactory().getProteinDao().getByAc(master.getAc());
        Assert.assertNotNull( refreshedMaster );
        Assert.assertNotNull(refreshedMaster.getCrc64());
        Assert.assertNotNull(refreshedMaster.getSequence());
        Assert.assertNotNull(refreshedMaster.getBioSource());

        final ProteinImpl refreshedChain = getDaoFactory().getProteinDao().getByAc(chain.getAc());
        Assert.assertNull( refreshedChain );

        context2.commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_updateProteinChains_chainWithInteraction() throws Exception {
        DataContext context = getDataContext();
        // master protein with interaction, linked chain with no interaction ...
        TransactionStatus status = context.beginTransaction();

        Protein master = getMockBuilder().createProtein("P03362", "pol_htl1a");
        final Interaction interaction = getMockBuilder().createInteraction( master );
        getCorePersister().saveOrUpdate(interaction);
        Assert.assertNotNull(master.getAc());

        // now create a protein chain
        CvDatabase intact = getMockBuilder().createCvObject( CvDatabase.class, CvDatabase.INTACT_MI_REF, "intact");
        CvXrefQualifier chainParent = getMockBuilder().createCvObject( CvXrefQualifier.class, "MI:0951", "chain-parent");

        Protein chain = getMockBuilder().createProtein("P03362-PRO_0000038873", "Reverse transcriptase/ribonuclease H");

        chain.addXref( new InteractorXref( chain.getOwner(), intact, master.getAc(), chainParent ) );

        final Interaction interaction2 = getMockBuilder().createInteraction( chain );
        getCorePersister().saveOrUpdate(interaction2);

        context.commitTransaction(status);

        // Run update on both the master and the chain
        ProteinUpdateProcessorConfig config = new ProteinUpdateProcessorConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(config);
        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        final ProteinImpl refreshedMaster = getDaoFactory().getProteinDao().getByAc(master.getAc());
        Assert.assertNotNull( refreshedMaster );
        Assert.assertNotNull(refreshedMaster.getCrc64());
        Assert.assertNotNull(refreshedMaster.getSequence());
        Assert.assertNotNull(refreshedMaster.getBioSource());

        final ProteinImpl refreshedChain = getDaoFactory().getProteinDao().getByAc(chain.getAc());
        Assert.assertNotNull( refreshedChain );
        Assert.assertNotNull(refreshedChain.getCrc64());
        Assert.assertNotNull(refreshedChain.getSequence());
        Assert.assertNotNull(refreshedChain.getBioSource());

        context2.commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_updateProteinChains_masterWithoutInteraction() throws Exception {

        DataContext context = getDataContext();
        // master protein with interaction, linked chain with no interaction ... both proteins should still be there post update
        TransactionStatus status = context.beginTransaction();

        Protein master = getMockBuilder().createProtein("P03362", "pol_htl1a");
        master.getBioSource().setTaxId("11926");
//        final Interaction interaction = getMockBuilder().createInteraction( master );
        getCorePersister().saveOrUpdate(master);
        Assert.assertNotNull(master.getAc());

        // now create a protein chain
        CvDatabase intact = getMockBuilder().createCvObject( CvDatabase.class, CvDatabase.INTACT_MI_REF, "intact");
        CvXrefQualifier chainParent = getMockBuilder().createCvObject( CvXrefQualifier.class, "MI:0951", "chain-parent");

        Protein chain = getMockBuilder().createProtein("P03362-PRO_0000038873", "Reverse transcriptase/ribonuclease H");
        chain.setBioSource(master.getBioSource());

        chain.addXref( new InteractorXref( chain.getOwner(), intact, master.getAc(), chainParent ) );

        final Interaction interaction = getMockBuilder().createInteraction( chain );
        getCorePersister().saveOrUpdate(interaction);

        context.commitTransaction(status);

        // Run update on both the master and the chain
        ProteinUpdateProcessorConfig config = new ProteinUpdateProcessorConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        config.setProcessProteinNotFoundInUniprot(false);
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(config);

        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        // the master protein should not be deleted as we have a chain

        final ProteinImpl refreshedMaster = getDaoFactory().getProteinDao().getByAc(master.getAc());
        Assert.assertNotNull( refreshedMaster );
        Assert.assertNotNull(refreshedMaster.getCrc64());
        Assert.assertNotNull(refreshedMaster.getSequence());
        Assert.assertNotNull(refreshedMaster.getBioSource());

        final ProteinImpl refreshedChain = getDaoFactory().getProteinDao().getByAc(chain.getAc());
        Assert.assertNotNull( refreshedChain );
        Assert.assertNotNull(refreshedChain.getCrc64());
        Assert.assertNotNull(refreshedChain.getSequence());
        Assert.assertNotNull(refreshedChain.getBioSource());
        Assert.assertEquals(2, refreshedChain.getAnnotations().size());

        boolean hasStart = false;
        boolean hasEnd = false;
        for (Annotation a : refreshedChain.getAnnotations()){
            if (CvTopic.CHAIN_SEQ_START.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                hasStart = true;
                System.out.println("start : " + a.getAnnotationText());
            }
            else if (CvTopic.CHAIN_SEQ_END.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                hasEnd = true;
                System.out.println("end : " + a.getAnnotationText());
            }
        }

        Assert.assertTrue(hasStart);
        Assert.assertTrue(hasEnd);

        context2.commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_duplicatedIsoform_isoformParent() throws Exception {

        // we have M1-SV1 and M2-SV2, M1 and M2 are duplicated, when merging, both splice variants should have
        // they isoform-parent Xref pointing to the remaining master. Add secondary-ac ??
        DataContext context = getDataContext();
        final TransactionStatus transactionStatus = context.beginTransaction();

        Protein[] proteins = createDuplicatedSpliceVariants();
        Assert.assertEquals( 3, proteins.length );

        final ProteinDao proteinDao = getDaoFactory().getProteinDao();

        Protein master1 = proteins[0];
        Protein isoform1 = proteins[1];
        Protein isoform2 = proteins[2];

        Assert.assertEquals(3, proteinDao.countAll());
        Assert.assertEquals(2, proteinDao.countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, proteinDao.getSpliceVariants(master1).size());
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );
        assertHasXref( isoform2, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );

        // note that master1.created < master2.created so that master will be retained as part of the merge procedure.
        context.commitTransaction(transactionStatus);

        // try the updater
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        // reload all proteins from scratch
        master1 = proteinDao.getByAc( master1.getAc() );
        Assert.assertNotNull( master1 );

        isoform1 = proteinDao.getByAc( isoform1.getAc() );
        Assert.assertNotNull( isoform1 );

        // master2 should have been merged into master1
        final String master2ac = master1.getAc();
        master1 = proteinDao.getByAc( master1.getAc() );
        Assert.assertNotNull( master1 );

        // isoform2 should have been merged into isoform1
        final String isoform2ac = isoform2.getAc();
        isoform2 = proteinDao.getByAc( isoform2ac );
        Assert.assertNull( isoform2 );

        // isoform-parent Xref should have been updated to reflect the parent merge
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );
//        assertHasXref( isoform2, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );

        // master/isoform 1 should have an xref pointing to the former master/isoform 2 AC
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, "intact-secondary", isoform2ac );

        Assert.assertEquals(2, proteinDao.countAll());
        Assert.assertEquals(1, proteinDao.countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());

        // interactions should have been moved onto the remaining isoform
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        Assert.assertEquals( 0, daoFactory.getInteractionDao().getInteractionsByInteractorAc( master1.getAc() ).size() );
        Assert.assertEquals( 2, daoFactory.getInteractionDao().getInteractionsByInteractorAc( isoform1.getAc() ).size() );

        context2.commitTransaction(status2);
    }

    private void assertHasXref( AnnotatedObject ao, String db, String qualifier, String primaryId ) {

        Assert.assertNotNull( ao );

        for ( Iterator iterator = ao.getXrefs().iterator(); iterator.hasNext(); ) {
            Xref xref = (Xref) iterator.next();

            if( (xref.getCvDatabase().getIdentifier().equals(db) || xref.getCvDatabase().getShortLabel().equals(db) ) &&
                    (xref.getCvXrefQualifier().getIdentifier().equals(qualifier) || xref.getCvXrefQualifier().getShortLabel().equals(qualifier) ) &&
                    xref.getPrimaryId().equals( primaryId ) ) {
                // found it
                return;
            }
        }

        Assert.fail( "Could not find an Xref with db='"+db+"' qualifier='"+qualifier+"' and primaryId='"+primaryId+"'." );
    }

    /**
     * created 2 identical master and isoform (both of which do have interactions).
     *
     * @return
     */
    private Protein[] createDuplicatedSpliceVariants() throws Exception {

        // http://www.uniprot.org/uniprot/P18459

        // we use our own persister to make sure we can duplicate master and isoform.
        CorePersister persister = new CorePersisterImpl( IntactContext.getCurrentInstance(), new DefaultFinder( ){
            @Override
            protected <T extends InteractorImpl> String findAcForInteractor( T interactor ) {
                // do not reuse interactors, always create new one
                return null;
            }
        });

        // make sure the owner has an Xref(psi-mi, intact_mi, identity)
        getMockBuilder().createInstitution( CvDatabase.INTACT_MI_REF, "intact" );

        DateFormat formatter = new SimpleDateFormat("yyyy/MM/dd");

        // interaction: no
        Protein master1 = getMockBuilder().createProtein("P18459", "master1");
        master1.getBioSource().setTaxId("7227");
        master1.setCreated( formatter.parse( "2010/06/23" ) );

        persister.saveOrUpdate(master1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein isoform1 = getMockBuilder().createProteinSpliceVariant(master1, "P18459-1", "isoform1");
        isoform1.getBioSource().setTaxId("7227");
        isoform1.setCreated( formatter.parse( "2010/06/23" ) );
        Interaction interaction = getMockBuilder().createInteraction( isoform1 );

        persister.saveOrUpdate(isoform1, interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getSpliceVariants(master1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());


        // interaction: yes
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(master1, "P18459-2", "isoform2");
        isoform2.getBioSource().setTaxId("7227");
        isoform2.setCreated( formatter.parse( "2010/06/26" ) ); // note: 3 days later than isoform 1
        isoform2.setSequence(isoform1.getSequence());
        Interaction interaction2 = getMockBuilder().createInteraction( isoform2 );

        persister.saveOrUpdate(isoform2, interaction2);

        InteractorXref identity = ProteinUtils.getUniprotXref(isoform2);
        identity.setPrimaryId("P18459-1");

        persister.saveOrUpdate(isoform2);

        return new Protein[]{ master1, isoform1, isoform2 };
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void spliceVariantGetGeneName() throws Exception {

        // check that splice variants do get gene names like the masters do.

        // http://www.uniprot.org/uniprot/P18459

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        // interaction: no
        Protein master1 = getMockBuilder().createProtein("P18459", "master1");
        master1.getBioSource().setTaxId( "7227" );
        master1.getBioSource().setShortLabel( "drome" );
        master1.getAliases().clear();

        getCorePersister().saveOrUpdate(master1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein isoform1 = getMockBuilder().createProteinSpliceVariant(master1, "P18459-1", "isoform1");
        isoform1.getBioSource().setTaxId( "7227" );
        isoform1.getBioSource().setShortLabel( "drome" );
        isoform1.getAliases().clear();

        Interaction interaction = getMockBuilder().createInteraction( isoform1 );

        getCorePersister().saveOrUpdate(isoform1, interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getSpliceVariants(master1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Protein reloadedMaster = getDaoFactory().getProteinDao().getByAc( master1.getAc() );

        assertHasAlias( reloadedMaster, CvAliasType.GENE_NAME_MI_REF, "ple" );
        assertHasAlias( reloadedMaster, CvAliasType.GENE_NAME_SYNONYM_MI_REF, "TH", "Tyrosine 3-hydroxylase", "Protein Pale" );
        assertHasAlias( reloadedMaster, CvAliasType.ORF_NAME_MI_REF, "CG10118" );

        Protein reloadedIsoform = getDaoFactory().getProteinDao().getByAc( isoform1.getAc() );

        assertHasAlias( reloadedIsoform, CvAliasType.GENE_NAME_MI_REF, "ple" );
        assertHasAlias( reloadedIsoform, CvAliasType.GENE_NAME_SYNONYM_MI_REF, "TH", "Tyrosine 3-hydroxylase", "Protein Pale" );
        assertHasAlias( reloadedIsoform, CvAliasType.ORF_NAME_MI_REF, "CG10118" );

        context2.commitTransaction(status2);
    }


    private void assertHasAlias( AnnotatedObject ao, String aliasLabelOrMi, String... expectedAliasNames ) {
        final Collection<Alias> aliases = AnnotatedObjectUtils.getAliasByType( ao, aliasLabelOrMi );
        Assert.assertEquals( expectedAliasNames.length, aliases.size() );

        List<String> expectedList = Arrays.asList( expectedAliasNames );
        for ( Alias alias : aliases ) {
            Assert.assertTrue( "Expected aliases: " + expectedList + ". Found: " + alias.getName(),
                    expectedList.contains( alias.getName() ) );
        }
    }

}
