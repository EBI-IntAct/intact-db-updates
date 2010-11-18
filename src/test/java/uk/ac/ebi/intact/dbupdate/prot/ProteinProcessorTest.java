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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.*;

/**
 * Tester of ProteinProcessor
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class ProteinProcessorTest extends IntactBasicTestCase {

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
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration allows to delete proteins without interactions.
     * Should be deleted
     */
    public void delete_protein_without_interaction() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(true);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
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

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(3, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getSpliceVariants(protein).size());

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
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

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
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

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is involved in one interaction but is 'no-uniprot-update'.
     * Should not be updated
     */
    public void ignore_protein_no_uniprot() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        Annotation no_uniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        protein.addAnnotation(no_uniprot);
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertTrue(hasAnnotation(protein, null, CvTopic.NON_UNIPROT));

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
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

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P12345", "protein");
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertTrue(hasAnnotation(protein, null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(protein, null, CvTopic.CAUTION));
        Assert.assertTrue(hasXRef(protein, "P12345", CvDatabase.UNIPROT, CvXrefQualifier.UNIPROT_REMOVED_AC));

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);
        config.setProcessProteinNotFoundInUniprot(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is involved in one interaction but is a dead protein in uniprot.
     * The configuration doesn't allow to fix dead proteins.
     * Should not be updated and the identity cross reference should not be changed
     */
    public void update_dead_protein_no() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);
        config.setProcessProteinNotFoundInUniprot(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P12345", "protein");
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertFalse(hasAnnotation(protein, null, CvTopic.NON_UNIPROT));
        Assert.assertFalse(hasAnnotation(protein, null, CvTopic.CAUTION));
        Assert.assertTrue(hasXRef(protein, "P12345", CvDatabase.UNIPROT, CvXrefQualifier.IDENTITY));

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);
        config.setProcessProteinNotFoundInUniprot(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One Intact protein is not involved in any interactions and the other is involved in one interaction.
     * The configuration allows to delete proteins without interactions.
     * The protein with the interaction is updated and the protein without interactions should be deleted
     */
    public void update_protein_and_delete_protein_without_interaction() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(true);

        TransactionStatus status = getDataContext().beginTransaction();

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

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(2, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One Intact protein is 'no-uniprot' and the other is from uniprot.
     * The protein from uniprot is updated and the protein 'no-uniptoy-update' should be ignored
     */
    public void update_protein_and_ignore_protein_no_uniprot() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        Annotation no_uniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        secondary.addAnnotation(no_uniprot);
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertTrue(hasAnnotation(secondary, null, CvTopic.NON_UNIPROT));

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should be merged because
     * the configuration allows to fix duplicates and only the original protein should be updated
     */
    public void update_protein_and_fix_duplicates() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(2, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(2, protein.getActiveInstances().size());

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(13, protein.getXrefs().size());
        Assert.assertTrue(hasXRef(protein, secondary.getAc(), CvDatabase.INTACT, "intact-secondary"));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * the configuration doesn't allow to fix duplicates and both proteins should be updated.
     * The isoforms and chains cannot be updated because we don't have a single parent in intact
     */
    public void update_protein_and_fix_duplicates_no() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(2, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());
        Assert.assertFalse(hasXRef(protein, secondary.getAc(), CvDatabase.INTACT, "intact-secondary"));

        // reset
        config.setFixDuplicates(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should be merged because
     * the configuration allows to fix duplicates and only the original isoform should be updated.
     */
    public void update_protein_and_fix_duplicates_isoform() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(protein, "P60953-1", "isoform1");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "isoform2");
        isoform2.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);
        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        getCorePersister().saveOrUpdate(interaction3);
        Interaction interaction4 = getMockBuilder().createInteraction(isoform2, random);
        getCorePersister().saveOrUpdate(interaction4);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform.getAc()));
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertEquals(2, protein.getActiveInstances().size());
        Assert.assertEquals(2, isoform.getActiveInstances().size());

        Assert.assertTrue(hasXRef(protein, secondary.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertTrue(hasXRef(isoform, isoform2.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertTrue(hasXRef(isoform, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertTrue(hasXRef(isoform2, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * the configuration doesn't allow to fix duplicates and both isoforms should be updated.
     */
    public void update_protein_and_fix_duplicates_isoforms_no() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(protein, "P60953-1", "isoform1");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "isoform2");
        isoform2.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);
        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        getCorePersister().saveOrUpdate(interaction3);
        Interaction interaction4 = getMockBuilder().createInteraction(isoform2, random);
        getCorePersister().saveOrUpdate(interaction4);

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());

        Assert.assertFalse(hasXRef(protein, secondary.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertFalse(hasXRef(isoform, isoform2.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertTrue(hasXRef(isoform, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertFalse(hasXRef(isoform2, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * there are feature conflicts and only the original protein should be updated.
     * The duplicated protein should be tagged as 'no-uniprot-update'
     */
    public void update_protein_and_fix_duplicates_conflicts_no_transcripts() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        Component componentWithFeatureConflicts = null;
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
                componentWithFeatureConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Assert.assertFalse(hasXRef(secondary, protein.getAc(), CvDatabase.INTACT, "intact-secondary"));

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        Protein noUniprotUpdate = (Protein) componentWithFeatureConflicts.getInteractor();
        Assert.assertFalse(ProteinUtils.isFromUniprot(noUniprotUpdate));
        Assert.assertEquals(protein.getAc(), noUniprotUpdate.getAc());

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same protein in IntAct. Should not be merged because
     * there are feature conflicts and only the original protein should be updated.
     * The duplicated protein is deleted because it was possible to remap the sequence to one isoform
     */
    public void update_protein_and_fix_duplicates_conflicts_transcripts_yes() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence("SYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHHH");
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(30, 30, 36, 36);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, protein.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(protein);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(1, secondary.getActiveInstances().size());

        Protein isoform2 = getDaoFactory().getProteinDao().getByUniprotId("P60953-2").iterator().next();
        Assert.assertEquals(1, isoform2.getActiveInstances().size());

        Assert.assertTrue(hasXRef(isoform2, protein.getAc(), CvDatabase.INTACT, "intact-secondary"));
        Assert.assertTrue(hasXRef(isoform2, secondary.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        Assert.assertEquals(30, range.getFromIntervalStart());
        Assert.assertEquals(30, range.getFromIntervalEnd());
        Assert.assertEquals(36, range.getToIntervalStart());
        Assert.assertEquals(36, range.getToIntervalEnd());

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * there are feature conflicts and only the original isoform should be updated.
     * The duplicated isoform should be tagged as 'no-uniprot-update'
     */
    public void update_protein_and_fix_isoforms_duplicates_conflicts_no_transcripts() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(secondary, "P60953-1", "isoformValid");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "duplicate");
        isoform2.getBioSource().setTaxId("9606");
        isoform2.setSequence("AAAAAAAAAAAAAAAAAAAAAAAAAAAAA");
        isoform2.getAnnotations().clear();
        isoform2.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(isoform2, random);
        Component componentWithFeatureConflicts = null;
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(isoform2.getAc())){
                c.addBindingDomain(feature);
                componentWithFeatureConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(isoform2);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());

        Assert.assertFalse(hasXRef(isoform, isoform2.getAc(), CvDatabase.INTACT, "intact-secondary"));

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        Protein noUniprotUpdate = (Protein) componentWithFeatureConflicts.getInteractor();
        Assert.assertFalse(ProteinUtils.isFromUniprot(noUniprotUpdate));
        Assert.assertEquals(isoform2.getAc(), noUniprotUpdate.getAc());

        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * There are two duplicates of the same isoform in IntAct. Should not be merged because
     * there are feature conflicts and only the original isoform should be updated.
     * The duplicated isoform is deleted because it was possible to remap the sequence to another isoform
     */
    public void update_protein_and_fix_isoform_duplicates_conflicts_transcripts_yes() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setFixDuplicates(true);
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        secondary.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(secondary);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(secondary, "P60953-1", "isoformValid");
        isoform.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(isoform);

        Protein isoform2 = getMockBuilder().createProteinSpliceVariant(secondary, "P21181-1", "duplicate");
        isoform2.getBioSource().setTaxId("9606");
        isoform2.setSequence("SYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHHH");
        isoform2.getAnnotations().clear();
        isoform2.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        InteractorXref ref = ProteinUtils.getUniprotXref(isoform2);
        ref.setPrimaryId("P60953-1");

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform2);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(30, 30, 36, 36);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(isoform2, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(isoform2.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        for (Component c : interaction2.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction2);

        Interaction interaction3 = getMockBuilder().createInteraction(isoform, random);
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());

        Set<String> updatedProteins = processor.update(isoform2);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(secondary.getAc()));
        Assert.assertEquals(0, isoform2.getActiveInstances().size());
        Assert.assertEquals(1, secondary.getActiveInstances().size());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        
        Protein isoformLoaded = getDaoFactory().getProteinDao().getByUniprotId("P60953-2").iterator().next();
        Assert.assertEquals(1, isoformLoaded.getActiveInstances().size());

        Assert.assertTrue(hasXRef(isoformLoaded, isoform2.getAc(), CvDatabase.INTACT, "intact-secondary"));

        Assert.assertEquals(30, range.getFromIntervalStart());
        Assert.assertEquals(30, range.getFromIntervalEnd());
        Assert.assertEquals(36, range.getToIntervalStart());
        Assert.assertEquals(36, range.getToIntervalEnd());

        Assert.assertEquals(4, getDaoFactory().getProteinDao().countAll());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void updateAll_oneElementToBeProcessedRemoved() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        final Protein protMain = getMockBuilder().createProtein("P12345", "main");
        protMain.setCreated(new Date(1));
        final Protein protRemoved = getMockBuilder().createProtein("Q01010", "removed");

        final Protein prot1 = getMockBuilder().createProteinRandom();
        final Protein prot2 = getMockBuilder().createProteinRandom();
        final Protein prot3 = getMockBuilder().createProteinRandom();
        final Protein prot4 = getMockBuilder().createProteinRandom();

        getCorePersister().saveOrUpdate(prot1, protMain, prot2, prot3, protRemoved, prot4);

        Assert.assertNotNull(protRemoved.getAc());

        getDataContext().commitTransaction(status);

        ProteinProcessor processor = new ProteinProcessor(3, 2) {
            protected void registerListeners() {
                addListener(new AbstractProteinUpdateProcessorListener() {

                    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
                        if ("main".equals(evt.getProtein().getShortLabel())) {
                            getDaoFactory().getProteinDao().deleteByAc(protRemoved.getAc());
                        }
                    }
                });
            }
        };
        processor.updateAll();
    }

    private boolean hasXRef( Protein p, String primaryAc, String databaseName, String qualifierName ) {
        final Collection<InteractorXref> refs = p.getXrefs();
        boolean hasXRef = false;

        for ( InteractorXref ref : refs ) {
            if (databaseName.equalsIgnoreCase(ref.getCvDatabase().getShortLabel())){
                if (qualifierName.equalsIgnoreCase(ref.getCvXrefQualifier().getShortLabel())){
                    if (primaryAc.equalsIgnoreCase(ref.getPrimaryId())){
                        hasXRef = true;
                    }
                }
            }
        }

        return hasXRef;
    }

    private boolean hasAlias( Protein p, String aliasLabel, String aliasName ) {
        final Collection<InteractorAlias> aliases = p.getAliases();

        boolean hasFoundAlias = false;

        for ( InteractorAlias alias : aliases ) {
            if (alias.getCvAliasType().getShortLabel().equals(aliasLabel)){
                if (aliasName.equals(alias.getName())){
                    hasFoundAlias = true;
                }
            }
        }

        return hasFoundAlias;
    }

    private boolean hasAnnotation( Protein p, String text, String cvTopic) {
        final Collection<Annotation> annotations = p.getAnnotations();
        boolean hasAnnotation = false;

        for ( Annotation a : annotations ) {
            if (cvTopic.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                if (text == null){
                    hasAnnotation = true;
                }
                else if (text != null && a.getAnnotationText() != null){
                    if (text.equalsIgnoreCase(a.getAnnotationText())){
                        hasAnnotation = true;
                    }
                }
            }
        }

        return hasAnnotation;
    }
}
