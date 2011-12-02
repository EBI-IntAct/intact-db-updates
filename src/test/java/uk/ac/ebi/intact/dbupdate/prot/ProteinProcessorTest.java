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
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.Collection;
import java.util.Set;

/**
 * Tester of ProteinProcessor
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
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
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is not involved in any interactions and the configuration allows to delete proteins without interactions.
     * Should be deleted
     */
    public void delete_protein_without_interaction() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(true);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein, context);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        context.commitTransaction(status);
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

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein, context);

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

        Assert.assertEquals(14, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context.commitTransaction(status);
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

        Set<String> updatedProteins = processor.update(protein, context);

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

        Assert.assertEquals(14, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context.commitTransaction(status);
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

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));

        Set<String> updatedProteins = processor.update(protein, context);

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

        Assert.assertEquals(14, protein.getXrefs().size());

        // reset
        config.setDeleteProtsWithoutInteractions(true);
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context.commitTransaction(status);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The Intact protein is involved in one interaction but is 'no-uniprot-update'.
     * Should not be updated
     */
    public void ignore_protein_no_uniprot() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        Annotation no_uniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        protein.addAnnotation(no_uniprot);
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein, context);

        Assert.assertEquals(1, updatedProteins.size());
        Assert.assertEquals(protein.getAc(), updatedProteins.iterator().next());
        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertTrue(hasAnnotation(protein, null, CvTopic.NON_UNIPROT));

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context.commitTransaction(status);
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

        Set<String> updatedProteins = processor.update(protein, context);

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

        context.commitTransaction(status);
    }

    @Test
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

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P12345", "protein");
        getCorePersister().saveOrUpdate(protein);

        Protein randomProtein = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(randomProtein);

        Interaction interaction = getMockBuilder().createInteraction(protein, randomProtein);
        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein, context);

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

        context.commitTransaction(status);
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

        Set<String> updatedProteins = processor.update(protein, context);

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

        Assert.assertEquals(14, protein.getXrefs().size());

        context.commitTransaction(status);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One Intact protein is 'no-uniprot' and the other is from uniprot.
     * The protein from uniprot is updated and the protein 'no-uniptoy-update' should be ignored
     */
    public void update_protein_and_ignore_protein_no_uniprot() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

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

        Set<String> updatedProteins = processor.update(protein, context);

        Assert.assertEquals(3, updatedProteins.size()); // the no-uniprot protein is considered as not processed excepted if we specifically choose to update it
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

        Assert.assertEquals(14, protein.getXrefs().size());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

        context.commitTransaction(status);
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
