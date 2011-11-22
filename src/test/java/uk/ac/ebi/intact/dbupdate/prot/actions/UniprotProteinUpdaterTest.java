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
 * Tester of UniprotProteinUpdaterImpl
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinUpdaterTest extends IntactBasicTestCase {

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
     * No Intact protein is matching the uniprot entry. The intact protein will be created
     * and updated with the uniprot entry
     */
    public void create_master_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        // the update event without any proteins in intact
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, new ArrayList<Protein>(),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create and update the protein in intact
        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());

        Protein createdProtein = evt.getPrimaryProteins().iterator().next();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(createdProtein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), createdProtein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), createdProtein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), createdProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), createdProtein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(createdProtein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(createdProtein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.LOCUS_NAME, locus));
        }

        // 4 uniprot + 10 other xrefs
        Assert.assertEquals(14, createdProtein.getXrefs().size());
        Assert.assertNotNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(createdProtein.getAc()));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * It is not a global protein so the transcript can be created.
     * No splice variant in Intact is matching any of the splice variants in uniprot.
     * They will be created and updated. They will be attached to the existing master protein in IntAct
     */
    public void create_isoform_protein_yes() throws Exception{
        // not a global update and we allow isoforms without interactions
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        // the master protein in Intact
        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create all splice variants
        updater.createOrUpdateIsoform(evt, master);
        Assert.assertEquals(2, evt.getPrimaryIsoforms().size());

        // test that isoform P60953-1 has been properly updated
        ProteinTranscript transcript = null;
        for (ProteinTranscript t : evt.getPrimaryIsoforms()){
            if (t.getUniprotVariant().getPrimaryAc().equals("P60953-1")){
                transcript = t;
                break;
            }
        }
        Assert.assertNotNull(transcript);
        Protein createdProtein = transcript.getProtein();
        UniprotSpliceVariant variant = (UniprotSpliceVariant) transcript.getUniprotVariant();

        Assert.assertEquals(master.getBioSource().getTaxId(), createdProtein.getBioSource().getTaxId());
        Assert.assertEquals(variant.getPrimaryAc().toLowerCase(), createdProtein.getShortLabel());
        Assert.assertEquals(master.getFullName(), createdProtein.getFullName());
        Assert.assertEquals(variant.getSequence(), createdProtein.getSequence());
        Assert.assertEquals(Crc64.getCrc64(variant.getSequence()), createdProtein.getCrc64());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(createdProtein).getPrimaryId());
        Assert.assertTrue(hasXRef(createdProtein, master.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        for (String secAc : variant.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(createdProtein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.LOCUS_NAME, locus));
        }

        for ( String syn2 : variant.getSynomyms() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.ISOFORM_SYNONYM, syn2));
        }

        Assert.assertEquals(3, createdProtein.getXrefs().size());
        Assert.assertTrue(hasAnnotation(createdProtein, variant.getNote(), CvTopic.ISOFORM_COMMENT));
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * It is not a global protein so the transcript can be created.
     * No feature chain in Intact is matching any of the feature chains in uniprot.
     * They will be created and updated. They will be attached to the existing master protein in IntAct
     */
    public void create_chain_protein_yes() throws Exception{
        // not a global update and we allow isoforms without interactions
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(false);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence().substring(2, 8));
        chain1.setStart(2);
        chain1.setEnd(7);
        UniprotFeatureChain chain2 = new UniprotFeatureChain("PRO-2", uniprot.getOrganism(), null);
        uniprot.getFeatureChains().add(chain1);
        uniprot.getFeatureChains().add(chain2);

        // the master protein in intact
        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        // the update case event
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), uniprot.getPrimaryAc());

        // create all feature chains
        updater.createOrUpdateFeatureChain(evt, master);
        Assert.assertEquals(2, evt.getPrimaryFeatureChains().size());

        // test that chain PRO-1 has been properly updated (has start and end)
        ProteinTranscript transcript = null;
        for (ProteinTranscript t : evt.getPrimaryFeatureChains()){
            if (t.getUniprotVariant().getPrimaryAc().equals("PRO-1")){
                transcript = t;
                break;
            }
        }
        Assert.assertNotNull(transcript);
        Protein createdProtein = transcript.getProtein();
        UniprotFeatureChain variant = (UniprotFeatureChain) transcript.getUniprotVariant();

        Assert.assertEquals(master.getBioSource().getTaxId(), createdProtein.getBioSource().getTaxId());
        Assert.assertEquals(variant.getPrimaryAc().toLowerCase(), createdProtein.getShortLabel());
        Assert.assertEquals(variant.getDescription(), createdProtein.getFullName());
        Assert.assertEquals(variant.getSequence(), createdProtein.getSequence());
        Assert.assertEquals(Crc64.getCrc64(variant.getSequence()), createdProtein.getCrc64());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(createdProtein).getPrimaryId());
        Assert.assertTrue(hasXRef(createdProtein, master.getAc(), CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT));

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(createdProtein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(2, createdProtein.getXrefs().size());
        Assert.assertTrue(hasAnnotation(createdProtein, Integer.toString(variant.getStart()), CvTopic.CHAIN_SEQ_START));
        Assert.assertTrue(hasAnnotation(createdProtein, Integer.toString(variant.getEnd()), CvTopic.CHAIN_SEQ_END));

        // test that chain PRO-2 has been properly updated (has start and end undetermined)
        ProteinTranscript transcript2 = null;
        for (ProteinTranscript t : evt.getPrimaryFeatureChains()){
            if (t.getUniprotVariant().getPrimaryAc().equals("PRO-2")){
                transcript2 = t;
                break;
            }
        }
        Assert.assertNotNull(transcript2);
        Protein createdProtein2 = transcript2.getProtein();

        Assert.assertTrue(hasAnnotation(createdProtein2, "?", CvTopic.CHAIN_SEQ_START));
        Assert.assertTrue(hasAnnotation(createdProtein2, "?", CvTopic.CHAIN_SEQ_END));

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * It is a global protein so the transcript cannot be created.
     * No splice variant in Intact is matching any of the splice variants in uniprot.
     * They will not be created because of the configuration of the update.
     */
    public void create_isoform_protein_no() throws Exception{
        // global update , we don't create splice variants
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        // the master protein in intact
        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        // the update case event
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        // create all splice variants : check that no protein transcript has been created in intact
        updater.createOrUpdateIsoform(evt, master);
        Assert.assertEquals(0, evt.getPrimaryIsoforms().size());

        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * It is a global protein so the transcript cannot be created.
     * No feature chain in Intact is matching any of the feature chains in uniprot.
     * They will not be created because of the configuration of the update.
     */
    public void create_chain_protein_no() throws Exception{
        // global update, we don't create feature chains
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setGlobalProteinUpdate(true);
        config.setDeleteProteinTranscriptWithoutInteractions(false);

        TransactionStatus status = getDataContext().beginTransaction();
        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        UniprotFeatureChain chain1 = new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), uniprot.getSequence().substring(2, 8));
        chain1.setStart(2);
        chain1.setEnd(7);
        UniprotFeatureChain chain2 = new UniprotFeatureChain("PRO-2", uniprot.getOrganism(), null);
        uniprot.getFeatureChains().add(chain1);
        uniprot.getFeatureChains().add(chain2);

        // the master protein in Intact
        Protein master = getMockBuilder().createProtein("P60953", "master");
        master.getBioSource().setTaxId("9606");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(master);

        // the update case event
        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, Collections.EMPTY_LIST,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), uniprot.getPrimaryAc());

        // create all feature chains : check that no feature chain has been created in Intact
        updater.createOrUpdateFeatureChain(evt, master);
        Assert.assertEquals(0, evt.getPrimaryFeatureChains().size());

        Assert.assertEquals(1, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein existing in intact with the uniprot protein
     */
    public void update_master_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";
        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(sequence);
        protein.getAnnotations().clear();
        protein.getAliases().clear();
        protein.addAlias(getMockBuilder().createAliasGeneName(protein, "CDC42"));

        InteractorAlias alias = getMockBuilder().createAlias(protein, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        protein.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(protein, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        protein.addXref(ref);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(updatedProtein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), updatedProtein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), updatedProtein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(updatedProtein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
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

        Assert.assertEquals(14, updatedProtein.getXrefs().size());
        Assert.assertFalse(hasAlias(updatedProtein, CvAliasType.ORF_NAME, "name"));
        Assert.assertFalse(hasXRef(updatedProtein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

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
