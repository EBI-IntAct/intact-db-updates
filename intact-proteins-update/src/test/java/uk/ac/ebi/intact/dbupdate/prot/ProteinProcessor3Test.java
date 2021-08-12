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
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.model.UniprotXref;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.Collection;
import java.util.List;
import java.util.stream.Collectors;

import static uk.ac.ebi.intact.util.protein.utils.TestsUtils.hasAlias;
import static uk.ac.ebi.intact.util.protein.utils.TestsUtils.hasXRef;

/**
 * Third Tester of ProteinProcessor
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id: ProteinProcessorTest.java 15394 2010-11-19 15:30:14Z marine.dumousseau@wanadoo.fr $
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinProcessor3Test extends IntactBasicTestCase {

    ProteinUpdateProcessor processor;

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
    public void delete_protein_without_interaction() {
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
    public void update_proteins_with_xrefs() {
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProtsWithoutInteractions(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_AHK4_ARATH();

        TransactionStatus status = getDataContext().beginTransaction();

        Protein protein = getMockBuilder().createProtein("Q9C5U0",
                "ahk4_arath", getMockBuilder().createBioSource(3702, "arath"));

        // now create a protein xrefs to check if they get properly updated
        CvDatabase go = getMockBuilder().createCvObject(CvDatabase.class, null, "go");
        protein.addXref(getMockBuilder().createXref(protein, "GO:0005783", null, go));
        protein.addXref(getMockBuilder().createXref(protein, "GO:0005789", null, go));
        protein.addXref(getMockBuilder().createXref(protein, "GO:0016021", null, go));
        protein.addXref(getMockBuilder().createXref(protein, "GO:0009736", null, go));

        CvDatabase ensemblPlants = getMockBuilder().createCvObject( CvDatabase.class, null,"ensemblplants");
        protein.addXref(getMockBuilder().createXref(protein, "AT2G01830.2", null, ensemblPlants));
        protein.addXref(getMockBuilder().createXref(protein, "AT2G01830", null, ensemblPlants));

        Protein isoform1 = getMockBuilder().createProtein("Q9C5U0-1",
                "q9c5u0-1", getMockBuilder().createBioSource(3702,"arath"));
        Protein isoform2 = getMockBuilder().createProtein("Q9C5U0-2",
                "q9c5u0-2", getMockBuilder().createBioSource(3702,"arath"));
        getCorePersister().saveOrUpdate(protein);
        getCorePersister().saveOrUpdate(isoform1);
        getCorePersister().saveOrUpdate(isoform2);

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(protein.getAc()));
        Assert.assertTrue(protein.getActiveInstances().isEmpty());
        Assert.assertEquals(7, protein.getXrefs().size());

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform1.getAc()));
        Assert.assertTrue(isoform1.getActiveInstances().isEmpty());
        Assert.assertEquals(1, isoform1.getXrefs().size());

        Assert.assertNotNull(getDaoFactory().getProteinDao().getByAc(isoform2.getAc()));
        Assert.assertTrue(isoform2.getActiveInstances().isEmpty());
        Assert.assertEquals(1, isoform2.getXrefs().size());

        context.commitTransaction(status);

        List<Protein> intactProteins = processor.retrieveAndUpdateProteinFromUniprot("Q9C5U0");

        status = context.beginTransaction();

        Assert.assertEquals(3, intactProteins.size());
        Protein updatedProtein = getDaoFactory().getProteinDao().getByAc(protein.getAc());

        Assert.assertNotNull(updatedProtein);

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(updatedProtein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), updatedProtein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), updatedProtein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(updatedProtein).getPrimaryId());

        // 7 uniprot + 13 other xrefs
        Assert.assertEquals(20, updatedProtein.getXrefs().size());

        Assert.assertEquals(6, uniprot.getSecondaryAcs().size());
        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(updatedProtein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        List<UniprotXref> ensemblXrefs = uniprot.getCrossReferences().stream()
                .filter(uniprotXref -> uniprotXref.getDatabase().equalsIgnoreCase("ensemblPlants"))
                .collect(Collectors.toList());
        Assert.assertEquals(3, ensemblXrefs.size());
        for (UniprotXref uniprotXref : ensemblXrefs){
            Assert.assertTrue(hasXRef(updatedProtein, uniprotXref.getAccession(), uniprotXref.getDatabase(), uniprotXref.getQualifier()));
        }

        List<UniprotXref> goXrefs = uniprot.getCrossReferences().stream()
                .filter(uniprotXref -> uniprotXref.getDatabase().equalsIgnoreCase("go"))
                .collect(Collectors.toList());
        Assert.assertEquals(4, goXrefs.size());
        for (UniprotXref uniprotXref : goXrefs){
            Assert.assertTrue(hasXRef(updatedProtein, uniprotXref.getAccession(), uniprotXref.getDatabase(),uniprotXref.getDescription(), uniprotXref.getQualifier()));
        }

        Assert.assertEquals(6, updatedProtein.getAliases().size());
        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertNotNull(updatedProtein);

        Collection<UniprotSpliceVariant> isoforms = uniprot.getSpliceVariants();
        Assert.assertEquals(2, isoforms.size());
        for (UniprotSpliceVariant isoform : isoforms) {
            switch (isoform.getPrimaryAc()){
                case "Q9C5U0-1":
                    updatedProtein = getDaoFactory().getProteinDao().getByAc(isoform1.getAc());

                    // 1 intact  + 1 uniprot + 3 other xrefs
                    Assert.assertEquals(5, updatedProtein.getXrefs().size());

                    ensemblXrefs = isoform.getCrossReferences().stream()
                            .filter(uniprotXref -> uniprotXref.getDatabase().equalsIgnoreCase("ensemblPlants"))
                            .collect(Collectors.toList());
                    Assert.assertEquals(3, ensemblXrefs.size());
                    for (UniprotXref uniprotXref : ensemblXrefs){
                        Assert.assertTrue(hasXRef(updatedProtein, uniprotXref.getAccession(), uniprotXref.getDatabase(), uniprotXref.getQualifier()));
                    }

                    break;
                case "Q9C5U0-2":
                    updatedProtein = getDaoFactory().getProteinDao().getByAc(isoform2.getAc());

                    // 1 intact  + 1 uniprot + 9 other xrefs
                    Assert.assertEquals(11, updatedProtein.getXrefs().size());

                    ensemblXrefs = isoform.getCrossReferences().stream()
                            .filter(uniprotXref -> uniprotXref.getDatabase().equalsIgnoreCase("ensemblPlants"))
                            .collect(Collectors.toList());
                    Assert.assertEquals(9, ensemblXrefs.size());
                    for (UniprotXref uniprotXref : ensemblXrefs){
                        Assert.assertTrue(hasXRef(updatedProtein, uniprotXref.getAccession(), uniprotXref.getDatabase(), uniprotXref.getQualifier()));
                    }
                    break;
                default:
                    throw new IllegalStateException("Unexpected value: " + isoform.getPrimaryAc());
            }

            // common to both isoforms
            Assert.assertNotNull(updatedProtein);

            Assert.assertEquals(isoform.getOrganism().getTaxid(), Integer.parseInt(updatedProtein.getBioSource().getTaxId()));
            Assert.assertEquals(isoform.getId().toLowerCase(), updatedProtein.getShortLabel());
            Assert.assertEquals(isoform.getSequence(), updatedProtein.getSequence());
            Assert.assertEquals(isoform.getPrimaryAc(), ProteinUtils.getUniprotXref(updatedProtein).getPrimaryId());
            Assert.assertEquals(0, isoform.getSecondaryAcs().size());

            Assert.assertEquals(7, updatedProtein.getAliases().size());
            for ( String geneName : uniprot.getGenes() ) {
                Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.GENE_NAME, geneName));
            }

            for ( String syn : uniprot.getSynomyms() ) {
                Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.GENE_NAME_SYNONYM, syn));
            }

            for ( String orf : uniprot.getOrfs() ) {
                Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.ORF_NAME, orf));
            }

            for ( String locus : uniprot.getLocuses() ) {
                Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.LOCUS_NAME, locus));
            }

            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.ISOFORM_SYNONYM, isoform.getSynomyms().iterator().next()));

        }

        context.commitTransaction(status);
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    /*
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
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

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