/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
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
 * ProteinUpdateProcessor Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class ProteinUpdateProcessorTest extends IntactBasicTestCase {

    @Before
    public void before_schema() throws Exception {
        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();
    }

    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     */
    @Test
    @DirtiesContext
    public void updateAll_delete_masterNoInteractions_spliceVars_yes() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        getCorePersister().saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        // splice var 'sv11' is deleted anyway, as P12345 does not contain such a splice var according to uniprot
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel("aatm_rabit")); // renamed master prot
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(randomProt.getShortLabel()));

        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getDbInfoDao().getAll().size());

    }

    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     * Delete splice vars without interactions too
     */
    @Test
    @DirtiesContext
    public void updateAll_delete_masterNoInteractions_spliceVars_yes_deleteSpliceVars() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabit


        getCorePersister().saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        getCorePersister().saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel("aatm_rabit")); // renamed master prot
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(randomProt.getShortLabel()));
    }

    /**
     * Delete: master prot does not have interactions, neither its splice variants
     */
    @Test
    @DirtiesContext
    public void updateAll_delete_masterNoInteractions_spliceVars_no() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabbit

        getCorePersister().saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: no
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        // this will generate 2 random proteins
        Interaction interaction = getMockBuilder().createInteractionRandomBinary();

        getCorePersister().saveOrUpdate(spliceVar11, spliceVar12, interaction);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        // the only 2 remaining protein should be the 2 random one as the master and its 2 splice variants do not have interactions attached.
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(masterProt1.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
    }

    /**
     * Duplicates: fix duplicates
     */
    @Test
    @DirtiesContext
    public void duplicates_found() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.getBioSource().setTaxId("9986"); // rabit

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe2 = cloner.clone(dupe1);
        ProteinUtils.getIdentityXrefs(dupe2).iterator().next().setPrimaryId("P12346");

        dupe2.setCreated(new Date(1)); // dupe2 is older

        Protein prot1 = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, prot1);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe2, prot2);
        Interaction interaction3 = getMockBuilder().createInteraction(dupe1, prot3);

        getCorePersister().saveOrUpdate(dupe1, dupe2, interaction1, interaction2, interaction3);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByCrcAndTaxId(dupe1.getCrc64(), dupe1.getBioSource().getTaxId()).size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe1.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        Assert.assertNotNull(dupe2FromDb);
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());
    }

    @Test
    @DirtiesContext
    public void updateProteinWithNullBiosource() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();

        Protein prot = getMockBuilder().createProtein("P42898", "riboflavin");
        prot.setBioSource(null);
        prot.setCrc64(null);
        prot.setSequence(null);
        
        final Interaction interaction = getMockBuilder().createInteraction(prot);

        getCorePersister().saveOrUpdate(interaction);

        Assert.assertNotNull(prot.getAc());

        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateByACs(Arrays.asList(prot.getAc()));

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        final ProteinImpl refreshedProt = getDaoFactory().getProteinDao().getByAc(prot.getAc());

        Assert.assertNotNull(refreshedProt.getCrc64());
        Assert.assertNotNull(refreshedProt.getSequence());
        Assert.assertNotNull(refreshedProt.getBioSource());
    }

    @Test
    @DirtiesContext
    public void updateAll_updateProteinChains_chainWithoutInteraction() throws Exception {

        // master protein with interaction, linked chain with no interaction ...

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

        // Run update on both the master and the chain
        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor( config );

        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        final ProteinImpl refreshedMaster = getDaoFactory().getProteinDao().getByAc(master.getAc());
        Assert.assertNotNull( refreshedMaster );
        Assert.assertNotNull(refreshedMaster.getCrc64());
        Assert.assertNotNull(refreshedMaster.getSequence());
        Assert.assertNotNull(refreshedMaster.getBioSource());

        final ProteinImpl refreshedChain = getDaoFactory().getProteinDao().getByAc(chain.getAc());
        Assert.assertNull( refreshedChain );
    }

    @Test
    @DirtiesContext
    public void updateAll_updateProteinChains_chainWithInteraction() throws Exception {

        // master protein with interaction, linked chain with no interaction ...


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

        // Run update on both the master and the chain
        ProteinUpdateProcessorConfig config = new ProteinUpdateProcessorConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(config);
        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

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
    }
    
    @Test
    @DirtiesContext
    public void updateAll_updateProteinChains_masterWithoutInteraction() throws Exception {

        // master protein with interaction, linked chain with no interaction ... both proteins should still be there post update

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

        // Run update on both the master and the chain
        ProteinUpdateProcessorConfig config = new ProteinUpdateProcessorConfig();
        config.setDeleteProteinTranscriptWithoutInteractions( true );
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(config);

        protUpdateProcessor.updateByACs( Arrays.asList( master.getAc(), chain.getAc() ) );

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

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
    }

    @Test
    @DirtiesContext
    public void updateAll_duplicatedIsoform_isoformParent() throws Exception {

        // we have M1-SV1 and M2-SV2, M1 and M2 are duplicated, when merging, both splice variants should have
        // they isoform-parent Xref pointing to the remaining master. Add secondary-ac ??

        final TransactionStatus transactionStatus = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein[] proteins = createDuplicatedSpliceVariants();
        Assert.assertEquals( 4, proteins.length );

        final ProteinDao proteinDao = getDaoFactory().getProteinDao();

        Protein master1 = proteins[0];
        Protein isoform1 = proteins[1];
        Protein master2 = proteins[2];
        Protein isoform2 = proteins[3];

        Assert.assertEquals(4, proteinDao.countAll());
        Assert.assertEquals(2, proteinDao.countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, proteinDao.getSpliceVariants(master2).size());
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );
        assertHasXref( isoform2, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master2.getAc() );

        // note that master1.created < master2.created so that master will be retained as part of the merge procedure.

        // try the updater
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDataContext().commitTransaction( transactionStatus );

        // reload all proteins from scratch
        master1 = proteinDao.getByAc( master1.getAc() );
        Assert.assertNotNull( master1 );

        isoform1 = proteinDao.getByAc( isoform1.getAc() );
        Assert.assertNotNull( isoform1 );

        // master2 should have been merged into master1
        final String master2ac = master2.getAc();
        master2 = proteinDao.getByAc( master2.getAc() );
        Assert.assertNull( master2 );

        // isoform2 should have been merged into isoform1
        final String isoform2ac = isoform2.getAc();
        isoform2 = proteinDao.getByAc( isoform2ac );
        Assert.assertNull( isoform2 );

        // isoform-parent Xref should have been updated to reflect the parent merge
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );
//        assertHasXref( isoform2, CvDatabase.INTACT_MI_REF, CvXrefQualifier.ISOFORM_PARENT_MI_REF, master1.getAc() );

        // master/isoform 1 should have an xref pointing to the former master/isoform 2 AC
        assertHasXref( master1, CvDatabase.INTACT_MI_REF, "intact-secondary", master2ac );
        assertHasXref( isoform1, CvDatabase.INTACT_MI_REF, "intact-secondary", isoform2ac );

        Assert.assertEquals(2, proteinDao.countAll());
        Assert.assertEquals(1, proteinDao.countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());

        // interactions should have been moved onto the remaining isoform
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        Assert.assertEquals( 0, daoFactory.getInteractionDao().getInteractionsByInteractorAc( master1.getAc() ).size() );
        Assert.assertEquals( 2, daoFactory.getInteractionDao().getInteractionsByInteractorAc( isoform1.getAc() ).size() );
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
        master1.setCreated( formatter.parse( "2010/06/23" ) );

        persister.saveOrUpdate(master1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein isoform1 = getMockBuilder().createProteinSpliceVariant(master1, "P18459-1", "isoform1");
        isoform1.setCreated( formatter.parse( "2010/06/23" ) );
        Interaction interaction = getMockBuilder().createInteraction( isoform1 );

        persister.saveOrUpdate(isoform1, interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getProteinDao().getSpliceVariants(master1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // interaction: no
        Protein master2 = getMockBuilder().createProtein("P18459", "master2");
        master2.setCreated( formatter.parse( "2010/06/26" ) ); // note: 3 days later than master 1

        persister.saveOrUpdate(master2);

        // interaction: yes
        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(master2, "P18459-1", "isoform2");
        isoform2.setCreated( formatter.parse( "2010/06/26" ) ); // note: 3 days later than isoform 1
        Interaction interaction2 = getMockBuilder().createInteraction( isoform2 );

        persister.saveOrUpdate(isoform2, interaction2);

        return new Protein[]{ master1, isoform1, master2, isoform2 };
    }

    @Test
    @DirtiesContext
    public void spliceVariantGetGeneName() throws Exception {

        // check that splice variants do get gene names like the masters do.

        // http://www.uniprot.org/uniprot/P18459

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );
        
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

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

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
    }

    @Test
    @DirtiesContext
    public void updateAll_updateRange() throws Exception {
         // TODO
    }

    // TODO test effect of config on deletion of molecules with interactions (proteins, chains, isoforms)

    private void assertHasAlias( AnnotatedObject ao, String aliasLabelOrMi, String... expectedAliasNames ) {
        final Collection<Alias> aliases = AnnotatedObjectUtils.getAliasByType( ao, aliasLabelOrMi );
        Assert.assertEquals( expectedAliasNames.length, aliases.size() );

        List<String> expectedList = Arrays.asList( expectedAliasNames );
        for ( Alias alias : aliases ) {
            Assert.assertTrue( "Expected aliases: " + expectedList + ". Found: " + alias.getName(),
                               expectedList.contains( alias.getName() ) );
        }
    }

    // seems like the global protein update bring in new splice variant if they were not in the db yet.
    // But given that they don't have interaction we don't need them be added in the first place.
}
