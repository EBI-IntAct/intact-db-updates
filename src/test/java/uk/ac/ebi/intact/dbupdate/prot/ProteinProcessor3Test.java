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

import org.junit.After;
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
import uk.ac.ebi.intact.model.Interaction;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.List;

/**
 * Third Tester of ProteinProcessor
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: ProteinProcessorTest.java 15394 2010-11-19 15:30:14Z marine.dumousseau@wanadoo.fr $
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinProcessor3Test extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setUniprotService(new MockUniprotService());

        processor = new ProteinUpdateProcessor();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        processor = null;
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration allows to delete proteins without interactions.
     * Should be deleted
     */
    public void delete_protein_without_interaction() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertTrue(protein.getActiveInstances().isEmpty());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        TransactionStatus status2 = context.beginTransaction();

        Assert.assertEquals(1, intactProteins.size());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        context.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration doesn't allow to delete
     * proteins without interactions. In addition, it is not a global update and we can have transcripts without interactions.
     * The protein should be updated and two splice variants without any interactions should be created
     */
    public void update_protein_without_interaction_transcripts_yes() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(3, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(protein).size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration doesn't allow to delete
     * proteins without interactions. In addition, it is a global update and we cannot have transcripts without interactions.
     * The protein should be updated and no splice variants should be created.
     */
    public void update_protein_without_interaction_no_transcripts() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(false);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(true);

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");
        
        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(1, intactProteins.size());
        Assert.assertEquals(protein.getAc(), intactProteins.iterator().next().getAc());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration doesn't allow to delete
     * proteins without interactions. In addition, it is not a global update and we cannot have transcripts without interactions.
     * The protein should be updated and no splice variants should be created.
     */
    public void update_protein_without_interaction_no_transcripts_2() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(false);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(1, intactProteins.size());
        Assert.assertEquals(protein.getAc(), intactProteins.iterator().next().getAc());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is involved in one interaction but is a dead protein in uniprot.
     * The configuration allows to fix dead proteins.
     * Should not be updated and the identity cross reference should be set as 'uniprot-removed-ac'
     */
    public void update_dead_protein_yes() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);
        config.setProcessProteinNotFoundInUniprot(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P12345", "protein");
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P12345");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context2.beginTransaction();

        Assert.assertEquals(0, intactProteins.size());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);
        config.setProcessProteinNotFoundInUniprot(true);

        context2.commitTransaction(status2);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One Intact protein is not involved in any interactions and the other is involved in one interaction.
     * The configuration allows to delete proteins without interactions.
     * The protein with the interaction is updated and the protein without interactions should be deleted
     */
    public void update_protein_and_delete_protein_without_interaction() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("P60953");

        DataContext context2 = getDataContext();
        TransactionStatus status2 = context.beginTransaction();

        Assert.assertEquals(1, intactProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        context2.commitTransaction(status2);
    }

}
