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
import uk.ac.ebi.intact.commons.util.Crc64;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.OutOfDateParticipantFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Second tester of UniprotProteinUpdaterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinUpdater2Test extends IntactBasicTestCase {

    private UniprotProteinUpdaterImpl updater;

    @Before
    public void before() throws Exception {
        updater = new UniprotProteinUpdaterImpl(new OutOfDateParticipantFixerImpl(new RangeFixerImpl()));
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
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges cannot be shifted properly : one protein transcript in uniprot has the same sequence and one intact secondary protein
     * is matching this splice variant with a sequence up to date: it will NOT be updated (will be updated when processing the transcripts) but the interactions will be attached to this transcript
     */
    public void update_master_interaction_range_impossible_to_shift_existing_secondary_isoform() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence());
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        uniprot.setSequence(protein.getSequence().substring(4));
        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getSequence().equals(protein.getSequence())){
                variant = v;
            }
        }

        Assert.assertNotNull(variant);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(protein, variant.getPrimaryAc(), "isoform");
        isoform.setSequence(variant.getSequence());
        isoform.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
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

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);
        Collection<ProteinTranscript> secondaryIsoforms = new ArrayList<ProteinTranscript>();
        secondaryIsoforms.add(new ProteinTranscript(isoform, variant));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, secondaryIsoforms, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(0, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(0, updatedProtein.getActiveInstances().size());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        ProteinTranscript transcript = evt.getSecondaryIsoforms().iterator().next();
        Assert.assertEquals(isoform.getAc(), transcript.getProtein().getAc());
        Assert.assertEquals(variant, transcript.getUniprotVariant());
        Assert.assertEquals(1, isoform.getActiveInstances().size());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges cannot be shifted properly : one protein transcript in uniprot has the same sequence and no intact primary protein
     * is matching this feature chain: it will be created and updated and the interactions will be attached to this transcript
     */
    public void update_master_interaction_range_impossible_to_shift_create_chain() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence());
        uniprot.getFeatureChains().add(chain1);

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence());
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        uniprot.setSequence(protein.getSequence().substring(4));

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getSequence().equals(protein.getSequence())){
                v.setSequence(uniprot.getSequence());
            }
        }

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

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(0, updatedProtein.getActiveInstances().size());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        Protein transcript = (Protein) componentWithFeatureConflicts.getInteractor();
        Assert.assertTrue(hasXRef(transcript, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT));
        Assert.assertNotNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(transcript.getAc()));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges cannot be shifted properly : one protein transcript in uniprot has the same sequence and one intact primary protein
     * is matching this feature chain: it will be updated and the interactions will be attached to this transcript
     */
    public void update_master_interaction_range_impossible_to_shift_existing_primary_chain() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence());
        uniprot.getFeatureChains().add(chain1);

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence());
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        uniprot.setSequence(protein.getSequence().substring(4));

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getSequence().equals(protein.getSequence())){
                v.setSequence(uniprot.getSequence());
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein chain = getMockBuilder().createProteinChain(protein, chain1.getPrimaryAc(), "chain");
        chain.setSequence(chain1.getSequence());
        chain.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
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

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, evt.getSecondaryIsoforms().size());
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(0, updatedProtein.getActiveInstances().size());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        ProteinTranscript transcript = evt.getPrimaryFeatureChains().iterator().next();
        Assert.assertEquals(chain.getAc(), transcript.getProtein().getAc());
        Assert.assertEquals(chain1, transcript.getUniprotVariant());
        Assert.assertEquals(1, chain.getActiveInstances().size());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a splice variant existing in intact with the uniprot protein
     */
    public void update_isoform() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variant = v;
            }
        }

        Assert.assertNotNull(variant);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(master, variant.getPrimaryAc(), "isoform");
        isoform.setSequence(sequence);
        isoform.getBioSource().setTaxId("9606");InteractorAlias alias = getMockBuilder().createAlias(isoform, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        isoform.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(isoform, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        isoform.addXref(ref);


        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variant));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateIsoform(evt, master);
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());

        // test that isoform P60953-1 has been properly updated
        ProteinTranscript transcript = evt.getPrimaryIsoforms().iterator().next();

        Protein updatedProtein = transcript.getProtein();
        Assert.assertEquals(variant, transcript.getUniprotVariant());

        Assert.assertEquals(isoform.getAc(), updatedProtein.getAc());

        Assert.assertEquals(master.getBioSource().getTaxId(), updatedProtein.getBioSource().getTaxId());
        Assert.assertEquals(variant.getPrimaryAc().toLowerCase(), updatedProtein.getShortLabel());
        Assert.assertEquals(master.getFullName(), updatedProtein.getFullName());
        Assert.assertEquals(variant.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(Crc64.getCrc64(variant.getSequence()), updatedProtein.getCrc64());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(updatedProtein).getPrimaryId());

        for (String secAc : variant.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(updatedProtein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

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

        for ( String syn2 : variant.getSynomyms() ) {
            Assert.assertTrue(hasAlias(updatedProtein, CvAliasType.ISOFORM_SYNONYM, syn2));
        }

        Assert.assertEquals(3, updatedProtein.getXrefs().size());
        Assert.assertFalse(hasAlias(updatedProtein, CvAliasType.ORF_NAME, "name"));
        Assert.assertFalse(hasXRef(updatedProtein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a splice variant existing in intact with the uniprot protein
     * the organism is null and will be updated with the uniprot transcript in uniprot
     */
    public void update_isoform_biosource_null() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variant = v;
            }
        }

        Assert.assertNotNull(variant);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(master, variant.getPrimaryAc(), "isoform");
        isoform.setSequence(sequence);
        isoform.setBioSource(null);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variant));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateIsoform(evt, master);
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        // test that isoform P60953-1 has been properly updated
        ProteinTranscript transcript = evt.getPrimaryIsoforms().iterator().next();

        Protein updatedProtein = transcript.getProtein();
        Assert.assertEquals(variant, transcript.getUniprotVariant());
        Assert.assertEquals(isoform.getAc(), updatedProtein.getAc());

        Assert.assertEquals(master.getBioSource().getTaxId(), updatedProtein.getBioSource().getTaxId());

        Assert.assertEquals(3, updatedProtein.getXrefs().size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a splice variant existing in intact with the uniprot protein
     * The organism is different from the one in uniprot : cannot update this splice variant
     */
    public void update_isoform_organism_different() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        master.setFullName(uniprot.getDescription());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getPrimaryAc().equals("P60953-1")){
                variant = v;
            }
        }

        Assert.assertNotNull(variant);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(master, variant.getPrimaryAc(), "isoform");
        isoform.setSequence(sequence);
        isoform.getBioSource().setTaxId("-1");
        InteractorAlias alias = getMockBuilder().createAlias(isoform, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        isoform.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(isoform, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        isoform.addXref(ref);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variant));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateIsoform(evt, master);
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(0, evt.getXrefUpdaterReports().size());

        Assert.assertNotSame(master.getBioSource().getTaxId(), isoform.getBioSource().getTaxId());
        Assert.assertNotSame(variant.getPrimaryAc().toLowerCase(), isoform.getShortLabel());
        Assert.assertNotSame(master.getFullName(), isoform.getFullName());
        Assert.assertNotSame(variant.getSequence(), isoform.getSequence());
        Assert.assertNotSame(Crc64.getCrc64(variant.getSequence()), isoform.getCrc64());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(isoform).getPrimaryId());

        Assert.assertEquals(3, isoform.getXrefs().size());
        Assert.assertTrue(hasAlias(isoform, CvAliasType.ORF_NAME, "name"));
        Assert.assertTrue(hasXRef(isoform, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a feature chain existing in intact with the uniprot protein
     */
    public void update_chain() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence().substring(2, 8));
        chain1.setStart(2);
        chain1.setEnd(7);
        uniprot.getFeatureChains().add(chain1);

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein chain = getMockBuilder().createProteinChain(master, chain1.getPrimaryAc(), "chain");
        chain.setSequence(sequence);
        chain.getBioSource().setTaxId("9606");InteractorAlias alias = getMockBuilder().createAlias(chain, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        chain.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(chain, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        chain.addXref(ref);


        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateFeatureChain(evt, master);
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        // test that chain P60953-1 has been properly updated
        ProteinTranscript transcript = evt.getPrimaryFeatureChains().iterator().next();

        Protein updatedProtein = transcript.getProtein();
        Assert.assertEquals(chain1, transcript.getUniprotVariant());

        Assert.assertEquals(chain.getAc(), updatedProtein.getAc());

        Assert.assertEquals(master.getBioSource().getTaxId(), updatedProtein.getBioSource().getTaxId());
        Assert.assertEquals(chain1.getPrimaryAc().toLowerCase(), updatedProtein.getShortLabel());
        Assert.assertEquals(chain1.getDescription(), updatedProtein.getFullName());
        Assert.assertEquals(chain1.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(Crc64.getCrc64(chain1.getSequence()), updatedProtein.getCrc64());
        Assert.assertEquals(chain1.getPrimaryAc(), ProteinUtils.getUniprotXref(updatedProtein).getPrimaryId());

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

        Assert.assertEquals(2, updatedProtein.getXrefs().size());
        Assert.assertFalse(hasAlias(updatedProtein, CvAliasType.ORF_NAME, "name"));
        Assert.assertFalse(hasXRef(updatedProtein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a feature chain existing in intact with the uniprot protein
     * The organism is null and will be updated with the organism in uniprot
     */
    public void update_chain_biosource_null() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence().substring(2, 8));
        chain1.setStart(2);
        chain1.setEnd(7);
        uniprot.getFeatureChains().add(chain1);

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein chain = getMockBuilder().createProteinChain(master, chain1.getPrimaryAc(), "chain");
        chain.setSequence(sequence);
        chain.setBioSource(null);
        InteractorAlias alias = getMockBuilder().createAlias(chain, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        chain.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(chain, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        chain.addXref(ref);


        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateFeatureChain(evt, master);
        Assert.assertEquals(1, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        // test that chain P60953-1 has been properly updated
        ProteinTranscript transcript = evt.getPrimaryFeatureChains().iterator().next();

        Protein updatedProtein = transcript.getProtein();
        Assert.assertEquals(chain1, transcript.getUniprotVariant());

        Assert.assertEquals(chain.getAc(), updatedProtein.getAc());

        Assert.assertEquals(master.getBioSource().getTaxId(), updatedProtein.getBioSource().getTaxId());

        Assert.assertEquals(2, updatedProtein.getXrefs().size());
        Assert.assertFalse(hasAlias(updatedProtein, CvAliasType.ORF_NAME, "name"));
        Assert.assertFalse(hasXRef(updatedProtein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a feature chain existing in intact with the uniprot protein
     * The organism is different from the organism in uniprot : cannot be updated
     */
    public void update_chain_organism_different() throws Exception{
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence().substring(2, 8));
        chain1.setStart(2);
        chain1.setEnd(7);
        chain1.setDescription("description");
        uniprot.getFeatureChains().add(chain1);

        String sequence = "AAAIILKY";

        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        Protein chain = getMockBuilder().createProteinChain(master, chain1.getPrimaryAc(), "chain");
        chain.setSequence(sequence);
        chain.getBioSource().setTaxId("-1");
        InteractorAlias alias = getMockBuilder().createAlias(chain, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        chain.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(chain, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        chain.addXref(ref);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(chain);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(master);
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(chain, chain1));

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChains, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateFeatureChain(evt, master);
        Assert.assertEquals(0, evt.getPrimaryFeatureChains().size());
        Assert.assertEquals(0, evt.getXrefUpdaterReports().size());

        Assert.assertNotSame(master.getBioSource().getTaxId(), chain.getBioSource().getTaxId());
        Assert.assertNotSame(chain1.getPrimaryAc().toLowerCase(), chain.getShortLabel());
        Assert.assertNotSame(chain1.getDescription(), chain.getFullName());
        Assert.assertNotSame(chain1.getSequence(), chain.getSequence());
        Assert.assertNotSame(Crc64.getCrc64(chain1.getSequence()), chain.getCrc64());
        Assert.assertEquals(chain1.getPrimaryAc(), ProteinUtils.getUniprotXref(chain).getPrimaryId());

        Assert.assertEquals(3, chain.getXrefs().size());
        Assert.assertTrue(hasAlias(chain, CvAliasType.ORF_NAME, "name"));
        Assert.assertTrue(hasXRef(chain, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);
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
