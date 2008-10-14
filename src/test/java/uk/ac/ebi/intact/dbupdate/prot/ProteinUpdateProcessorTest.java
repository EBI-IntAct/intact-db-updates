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
import uk.ac.ebi.intact.core.persister.PersisterHelper;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.core.util.SchemaUtils;
import uk.ac.ebi.intact.model.Interaction;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Arrays;
import java.util.Date;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessorTest extends IntactBasicTestCase {

    @Before
    public void before_schema() throws Exception {
        SchemaUtils.createSchema();

        beginTransaction();
        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();
        commitTransaction();
    }

    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     */
    @Test
    public void updateAll_delete_masterNoInteractions_spliceVars_yes() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setProcessBatchSize(3);
        configUpdate.setProcessStepSize(2);

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabit

        PersisterHelper.saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        PersisterHelper.saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        // splice var 'sv11' is deleted anyway, as P12345 does not contain such a splice var according to uniprot
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());
        
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel("aatm_rabit")); // renamed master prot
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByShortLabel(randomProt.getShortLabel()));

    }


    /**
     * Delete: master prot does not have interactions, but has splice variants with interactions
     * Delete splice vars without interactions too
     */
    @Test
    public void updateAll_delete_masterNoInteractions_spliceVars_yes_deleteSpliceVars() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setProcessBatchSize(3);
        configUpdate.setProcessStepSize(2);
        configUpdate.setDeleteSpliceVariantsWithoutInteractions(true);

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabit


        PersisterHelper.saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        // interaction: yes
        Protein randomProt = getMockBuilder().createProteinRandom();

        Interaction interaction = getMockBuilder().createInteraction(spliceVar11, randomProt);

        PersisterHelper.saveOrUpdate(spliceVar12, interaction);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
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
    public void updateAll_delete_masterNoInteractions_spliceVars_no() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setProcessBatchSize(3);
        configUpdate.setProcessStepSize(2);

        // interaction: no
        Protein masterProt1 = getMockBuilder().createProtein("P12345", "master1");
        masterProt1.getBioSource().setTaxId("9986"); // rabit

        PersisterHelper.saveOrUpdate(masterProt1);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: no
        Protein spliceVar11 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-1", "sv11");

        // interaction: no
        Protein spliceVar12 = getMockBuilder().createProteinSpliceVariant(masterProt1, "P12345-2", "sv12");

        Interaction interaction = getMockBuilder().createInteractionRandomBinary();

        PersisterHelper.saveOrUpdate(spliceVar11, spliceVar12, interaction);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(masterProt1).size());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);

        protUpdateProcessor.updateAll();

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar12.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(masterProt1.getShortLabel()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByShortLabel(spliceVar11.getShortLabel()));
    }

    /**
     * Duplicates: fix duplicates
     */
    @Test
    public void duplicates_found() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setProcessBatchSize(3);
        configUpdate.setProcessStepSize(2);

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

        PersisterHelper.saveOrUpdate(dupe1, dupe2, interaction1, interaction2, interaction3);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(6, getDaoFactory().getComponentDao().countAll());

        beginTransaction();

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        commitTransaction();

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
    public void updateProteinWithNullBiosource() throws Exception {
        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setProcessBatchSize(3);
        configUpdate.setProcessStepSize(2);

        Protein prot = getMockBuilder().createProtein("P42898", "riboflavin");
        prot.setBioSource(null);
        prot.setCrc64(null);
        prot.setSequence(null);
        
        final Interaction interaction = getMockBuilder().createInteraction(prot);

        PersisterHelper.saveOrUpdate(interaction);

        Assert.assertNotNull(prot.getAc());

        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateByACs(Arrays.asList(prot.getAc()));

        final ProteinImpl refreshedProt = getDaoFactory().getProteinDao().getByAc(prot.getAc());

        Assert.assertNotNull(refreshedProt.getCrc64());
        Assert.assertNotNull(refreshedProt.getSequence());
        Assert.assertNotNull(refreshedProt.getBioSource());
    }
    
}
