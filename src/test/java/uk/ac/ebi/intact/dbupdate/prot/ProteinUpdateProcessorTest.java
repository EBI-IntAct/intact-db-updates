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
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Arrays;
import java.util.Collection;
import java.util.Date;
import java.util.List;

/**
 * ProteinUpdateProcessor Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinUpdateProcessorTest extends IntactBasicTestCase {

    @Before
    public void before() throws Exception{

        DataContext context = getDataContext();

        TransactionStatus status = context.beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(context.getDaoFactory());
        primer.createCVs();

        context.commitTransaction(status);
    }

    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_delete_masterNoInteractions_spliceVars_yes() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);
        configUpdate.setProcessProteinNotFoundInUniprot(false);

        DataContext context = getDataContext();

        TransactionStatus status = context.beginTransaction();

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
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        getCorePersister().saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();

        TransactionStatus status2 = context2.beginTransaction();

        // splice var 'sv11' is deleted anyway, as P12345 does not contain such a splice var according to uniprot
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel("aatm_rabit")); // renamed master prot
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(randomProt.getShortLabel()));

        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getDbInfoDao().getAll().size());

        context2.commitTransaction(status2);
    }

    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     * Delete splice vars without interactions too
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_delete_masterNoInteractions_spliceVars_yes_deleteSpliceVars() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);
        configUpdate.setProcessProteinNotFoundInUniprot(false);

        DataContext context = getDataContext();

        TransactionStatus status = context.beginTransaction();
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
        randomProt.getXrefs().iterator().next().setPrimaryId("Q13948");

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        getCorePersister().saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel("aatm_rabit")); // renamed master prot
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(randomProt.getShortLabel()));

        context2.commitTransaction(status2);
    }

    /**
     * Delete: master prot does not have interactions, neither its splice variants
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_delete_masterNoInteractions_spliceVars_no() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );
        configUpdate.setProcessProteinNotFoundInUniprot(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

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

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        // the only 2 remaining protein should be the 2 random one as the master and its 2 splice variants do not have interactions attached.
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(masterProt1.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));

        context2.commitTransaction(status2);
    }

    /**
     * Duplicates: fix duplicates
     */
    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void duplicates_found() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        DataContext context = IntactContext.getCurrentInstance().getDataContext();

        TransactionStatus status = context.beginTransaction();

        CvXrefQualifier intactSecondary = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");
        CvDatabase intact = context.getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);

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

        dupe1.addXref(getMockBuilder().createXref(dupe1, "EBI-xxxx", intactSecondary, intact));
        getCorePersister().saveOrUpdate(dupe1);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByCrcAndTaxId(dupe1.getCrc64(), dupe1.getBioSource().getTaxId()).size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe1.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        Assert.assertNotNull(dupe2FromDb);
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void duplicates_found_isoforms() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);

        DataContext context = getDataContext();

        TransactionStatus status = context.beginTransaction();

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12346", "dupe1");
        dupe1.getBioSource().setTaxId("10116");

        getCorePersister().saveOrUpdate(dupe1);

        Protein dupe1_1 = getMockBuilder().createProteinSpliceVariant(dupe1, "P12346-1", "p12346-1");
        dupe1_1.getBioSource().setTaxId("10116");
        dupe1_1.setSequence("MRFAVGALLACAALGLCLAVPDKTVKWCAVSEHENTKCISFRDHMKTVLPADGPRLACVK" +
                "KTSYQDCIKAISGGEADAITLDGGWVYDAGLTPNNLKPVAAEFYGSLEHPQTHYLAVAVV" +
                "KKGTDFQLNQLQGKKSCHTGLGRSAGWIIPIGLLFCNLPEPRKPLEKAVASFFSGSCVPC" +
                "ADPVAFPQLCQLCPGCGCSPTQPFFGYVGAFKCLRDGGGDVAFVKHTTIFEVLPQKADRD" +
                "QYELLCLDNTRKPVDQYEDCYLARIPSHAVVARNGDGKEDLIWEILKVAQEHFGKGKSKD" +
                "FQLFGSPLGKDLLFKDSAFGLLRVPPRMDYRLYLGHSYVTAIRNQREGVCPEGSIDSAPV" +
                "KWCALSHQERAKCDEWSVSSNGQIECESAESTEDCIDKIVNGEADAMSLDGGHAYIAGQC" +
                "GLVPVMAENYDISSCTNPQSDVFPKGYYAVAVVKASDSSINWNNLKGKKSCHTGVDRTAG" +
                "WNIPMGLLFSRINHCKFDEFFSQGCAPGYKKNSTLCDLCIGPAKCAPNNREGYNGYTGAF" +
                "QCLVEKGDVAFVKHQTVLENTNGKNTAAWAKDLKQEDFQLLCPDGTKKPVTEFATCHLAQ" +
                "APNHVVVSRKEKAARVSTVLTAQKDLFWKGDKDCTGNFCLFRSSTKDLLFRDDTKCLTKL" +
                "PEGTTYEEYLGAEYLQAVGNIRKCSTSRLLEACTFHKS");

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe1_2 = cloner.clone(dupe1_1);
        dupe1_2.setBioSource(dupe1_1.getBioSource());
        dupe1_2.setCrc64(dupe1_1.getCrc64());
        ProteinUtils.getIdentityXrefs(dupe1_2).iterator().next().setPrimaryId("P12346-2");

        dupe1_2.setCreated(new Date(1)); // dupe2 is older

        Protein prot1 = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1_1, prot1);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe1_2, prot2);
        Interaction interaction3 = getMockBuilder().createInteraction(dupe1_1, prot3);

        getCorePersister().saveOrUpdate(dupe1, dupe1_1, dupe1_2, interaction1, interaction2, interaction3);

        Assert.assertEquals(6, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12346-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByCrcAndTaxId(dupe1_1.getCrc64(), dupe1_1.getBioSource().getTaxId()).size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12346-1").size());

        context.commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe1_1.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());
        Assert.assertNotNull(dupe2FromDb);
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());
        Assert.assertEquals(3, dupe2FromDb.getXrefs().size());

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    @Ignore
    public void duplicates_found_isoforms_differentSequence_oneBadRange() throws Exception {
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        CvFeatureType type = getMockBuilder().createCvObject(CvFeatureType.class, CvFeatureType.EXPERIMENTAL_FEATURE_MI_REF, CvFeatureType.EXPERIMENTAL_FEATURE);
        getCorePersister().saveOrUpdate(type);

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions(true);
        configUpdate.setGlobalProteinUpdate(true);
        configUpdate.setProcessProteinNotFoundInUniprot(false);

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12346", "dupe1");
        dupe1.getBioSource().setTaxId("10116");

        getCorePersister().saveOrUpdate(dupe1);

        Protein dupe1_1 = getMockBuilder().createProteinSpliceVariant(dupe1, "P12346-1", "p12346-1");
        dupe1_1.getBioSource().setTaxId("10116");
        dupe1_1.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        dupe1_1.setCreated(new Date(System.currentTimeMillis()));
        getCorePersister().saveOrUpdate(dupe1_1);

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe1_2 = cloner.clone(dupe1_1);
        dupe1_2.setBioSource(dupe1_1.getBioSource());
        dupe1_2.setCrc64(dupe1_1.getCrc64());
        dupe1_2.setSequence("MRFAVGALLACAALGLCLAVPDKTVKWCAVSEHENTKCISFRDHMKTVLPADGPRLACVK" +
                "KTSYQDCIKAISGGEADAITLDGGWVYDAGLTPNNLKPVAAEFYGSLEHPQTHYLAVAVV" +
                "KKGTDFQLNQLQGKKSCHTGLGRSAGWIIPIGLLFCNLPEPRKPLEKAVASFFSGSCVPC" +
                "ADPVAFPQLCQLCPGCGCSPTQPFFGYVGAFKCLRDGGGDVAFVKHTTIFEVLPQKADRD" +
                "QYELLCLDNTRKPVDQYEDCYLARIPSHAVVARNGDGKEDLIWEILKVAQEHFGKGKSKD" +
                "FQLFGSPLGKDLLFKDSAFGLLRVPPRMDYRLYLGHSYVTAIRNQREGVCPEGSIDSAPV" +
                "KWCALSHQERAKCDEWSVSSNGQIECESAESTEDCIDKIVNGEADAMSLDGGHAYIAGQC" +
                "GLVPVMAENYDISSCTNPQSDVFPKGYYAVAVVKASDSSINWNNLKGKKSCHTGVDRTAG" +
                "WNIPMGLLFSRINHCKFDEFFSQGCAPGYKKNSTLCDLCIGPAKCAPNNREGYNGYTGAF" +
                "QCLVEKGDVAFVKHQTVLENTNGKNTAAWAKDLKQEDFQLLCPDGTKKPVTEFATCHLAQ" +
                "APNHVVVSRKEKAARVSTVLTAQKDLFWKGDKDCTGNFCLFRSSTKDLLFRDDTKCLTKL" +
                "PEGTTYEEYLGAEYLQAVGNIRKCSTSRLLEACTFHKS");
        ProteinUtils.getIdentityXrefs(dupe1_2).iterator().next().setPrimaryId("P12346-2");

        dupe1_2.setCreated(new Date(1)); // dupe2 is older

        Protein prot1 = getMockBuilder().createProteinRandom();
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        getCorePersister().saveOrUpdate(dupe1_2, prot1, prot2, prot3);

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1_1, prot1);
        Collection<Component> components = interaction1.getComponents();

        Range r = getMockBuilder().createRange(2, 2, 8, 8);
        Component c = null;
        for (Component comp : components){
           if (dupe1_1.equals(comp.getInteractor())){
               c = comp;
           }
        }

        c.getBindingDomains().clear();
        Feature f = getMockBuilder().createFeatureRandom();
        f.getRanges().clear();
        f.addRange(r);
        f.setComponent(c);
        c.addBindingDomain(f);
        getCorePersister().saveOrUpdate(interaction1, dupe1_1, prot1);

        Interaction interaction2 = getMockBuilder().createInteraction(dupe1_2, prot2);
        Collection<Component> components2 = interaction2.getComponents();

        Range r2 = getMockBuilder().createRange(2, 2, 8, 8);
        Component c2 = null;
        for (Component comp : components2){
           if (dupe1_2.equals(comp.getInteractor())){
               c2 = comp;
           }
        }

        c2.getBindingDomains().clear();
        Feature f2 = getMockBuilder().createFeatureRandom();
        f2.getRanges().clear();
        f2.addRange(r2);
        f2.setComponent(c2);
        c2.addBindingDomain(f2);
        getCorePersister().saveOrUpdate(interaction2, dupe1_2, prot2);

        Interaction interaction3 = getMockBuilder().createInteraction(dupe1_1, prot3);
        Collection<Component> components3 = interaction3.getComponents();

        Range r3 = getMockBuilder().createRange(2, 2, 8, 8);
        Component c3 = null;
        for (Component comp : components3){
           if (dupe1_1.equals(comp.getInteractor())){
               c3 = comp;
           }
        }

        c3.getBindingDomains().clear();
        Feature f3 = getMockBuilder().createFeatureRandom();
        f3.getRanges().clear();
        f3.addRange(r3);
        f3.setComponent(c3);
        c3.addBindingDomain(f3);
        getCorePersister().saveOrUpdate(interaction3, dupe1_1, prot3);

        Assert.assertEquals(6, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12346-1");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByCrcAndTaxId(dupe1_1.getCrc64(), dupe1_1.getBioSource().getTaxId()).size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12346-1").size());

        boolean hasCautionBefore = false;

        for (Annotation a : dupe1_1.getAnnotations()){
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

        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(dupe1_1.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe1_2.getAc());

        ProteinImpl dupe1FromDb = getDaoFactory().getProteinDao().getByAc(dupe1_1.getAc());

        // if dupe 2 before, dupe 2 will be kept as original protein and will have only one interaction because the other interactions have range conflicts.
        // dupe 1 is now deprecated and contains two interactions
        if (dupe2FromDb.getCreated().before(dupe1FromDb.getCreated())){
            // keep same number of proteins
            Assert.assertEquals(6, getDaoFactory().getProteinDao().countAll());

            Assert.assertNotNull(dupe2FromDb);
            Assert.assertNotNull(dupe1FromDb);

            Assert.assertEquals(1, dupe2FromDb.getActiveInstances().size());
            Assert.assertEquals(2, dupe1FromDb.getActiveInstances().size());
        }
        // if dupe 1 before, dupe 1 will be kept as original an a new protein will be deprecated and have 2 interactions od dupe1.
        // dupe 1 will have one interaction (the one of dupe 2)
        // dupe 2 is deleted
        else {
            Assert.assertEquals(6, getDaoFactory().getProteinDao().countAll());

            Assert.assertNull(dupe2FromDb);
            Assert.assertNotNull(dupe1FromDb);

            Assert.assertEquals(1, dupe1FromDb.getActiveInstances().size());
        }

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

    // seems like the global protein update bring in new splice variant if they were not in the db yet.
    // But given that they don't have interaction we don't need them be added in the first place.
}
