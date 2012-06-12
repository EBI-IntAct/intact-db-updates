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
package uk.ac.ebi.intact.dbupdate.prot.report;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.clone.IntactCloner;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.io.BufferedReader;
import java.io.File;
import java.io.FileReader;
import java.io.IOException;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * FileReportHandler Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class FileReportHandlerTest extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();
        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setBlastEnabled(false);
        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        processor = new ProteinUpdateProcessor();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        processor = null;
    }

    @Test
    @Transactional(propagation = Propagation.NEVER)
    public void simulation() throws Exception {

        TransactionStatus status = getDataContext().beginTransaction();

        final File dir = new File("target/simulation");
        UpdateReportHandler reportHandler = new FileReportHandler(dir);
        ProteinUpdateContext.getInstance().getConfig().setReportHandler( reportHandler );
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate(true);

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

        // one protein for all interactions. This protein has the same sequence as one isoform which is not the canonical sequence
        Protein simple = getMockBuilder().createProtein("P36872", "simple_protein");
        simple.getBioSource().setTaxId("7227");
        simple.setSequence("MAGNGEASWCFSQIKGALDDDVTDADIISCVEFNHDGELLATGDKGGRVVIFQRDPASKA" +
                "ANPRRGEYNVYSTFQSHEPEFDYLKSLEIEEKINKIRWLQQKNPVHFLLSTNDKTVKLWK" +
                "VSERDKSFGGYNTKEENGLIRDPQNVTALRVPSVKQIPLLVEASPRRTFANAHTYHINSI" +
                "SVNSDQETFLSADDLRINLWHLEVVNQSYNIVDIKPTNMEELTEVITAAEFHPTECNVFV" +
                "YSSSKGTIRLCDMRSAALCDRHSKQFEEPENPTNRSFFSEIISSISDVKLSNSGRYMISR" +
                "DYLSIKVWDLHMETKPIETYPVHEYLRAKLCSLYENDCIFDKFECCWNGKDSSIMTGSYN" +
                "NFFRVFDRNSKKDVTLEASRDIIKPKTVLKPRKVCTGGKRKKDEISVDCLDFNKKILHTA" +
                "WHPEENIIAVAATNNLFIFQDKF") ;
        getCorePersister().saveOrUpdate(simple);

        // one protein no uniprot update with one interaction
        Protein noUniProtUpdate = getMockBuilder().createProtein("P12342", "no_uniprot_update");
        noUniProtUpdate.getXrefs().clear();
        Annotation noUniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        noUniProtUpdate.addAnnotation(noUniprot);
        getCorePersister().saveOrUpdate(noUniProtUpdate);

        Interaction interaction_1 = getMockBuilder().createInteraction(simple, noUniProtUpdate);
        for (Component c : interaction_1.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_1);

        // one protein without uniprot identity and with an interaction
        Protein noUniProtIdentity = getMockBuilder().createProtein("P12341", "no_uniprot_identity");
        InteractorXref identity = ProteinUtils.getUniprotXref(noUniProtIdentity);
        noUniProtIdentity.removeXref(identity);
        getCorePersister().saveOrUpdate(noUniProtIdentity);

        Interaction interaction_2 = getMockBuilder().createInteraction(simple, noUniProtIdentity);
        for (Component c : interaction_2.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_2);

        // one protein with several uniprot identities and with an interaction
        Protein severalUniProtIdentity = getMockBuilder().createProtein("P12343", "several_uniprot_identities");
        InteractorXref identity2 = getMockBuilder().createXref(severalUniProtIdentity, "P12344",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF, CvXrefQualifier.IDENTITY),
                getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT));
        severalUniProtIdentity.addXref(identity2);
        getCorePersister().saveOrUpdate(severalUniProtIdentity);

        Interaction interaction_3 = getMockBuilder().createInteraction(simple, severalUniProtIdentity);
        for (Component c : interaction_3.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_3);

        // one protein without any interactions
        Protein noInteractions = getMockBuilder().createProtein("P12348", "no_interaction");
        getCorePersister().saveOrUpdate(noInteractions);

        // one dead protein with an interaction
        Protein deadProtein = getMockBuilder().createProtein("Pxxxx", "dead_protein");
        getCorePersister().saveOrUpdate(deadProtein);

        Interaction interaction_4 = getMockBuilder().createInteraction(simple, deadProtein);
        for (Component c : interaction_4.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_4);

        // one intact transcript without parent with an interaction
        Protein transcriptWithoutParent = getMockBuilder().createProtein("P18459-2", "isoform_no_parent");
        transcriptWithoutParent.setBioSource(getMockBuilder().createBioSource(7227, "drome"));
        getCorePersister().saveOrUpdate(transcriptWithoutParent);

        Interaction interaction_5 = getMockBuilder().createInteraction(simple, transcriptWithoutParent);
        for (Component c : interaction_5.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_5);

        // one secondary protein
        Protein secondary = getMockBuilder().createProtein("O34373", "secondary_protein");
        secondary.getBioSource().setTaxId("224308");
        getCorePersister().saveOrUpdate(secondary);

        Interaction interaction_6 = getMockBuilder().createInteraction(simple, secondary);
        for (Component c : interaction_6.getComponents()){
            c.getBindingDomains().clear();
        }

        // add a range which is invalid
        Feature feature4 = getMockBuilder().createFeatureRandom();
        feature4.getRanges().clear();
        Range range4 = getMockBuilder().createRange(11, 11, 8, 8);
        feature4.addRange(range4);
        secondary.getActiveInstances().iterator().next().addBindingDomain(feature4);

        getCorePersister().saveOrUpdate(secondary, interaction_6);

        // one protein with bad organism
        Protein bad_organism = getMockBuilder().createProtein("P12349", "bad_organism");
        bad_organism.getBioSource().setTaxId("9606");    // is 7244 in uniprot
        getCorePersister().saveOrUpdate(bad_organism);

        Interaction interaction_7 = getMockBuilder().createInteraction(simple, bad_organism);
        for (Component c : interaction_7.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_7);

        // one protein with a splice variant which doesn't exist in uniprot but contains one interaction
        Protein parent = getMockBuilder().createProtein("P12350", "not_existing_splice_variant");
        parent.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Protein isoform_no_uniprot = getMockBuilder().createProteinSpliceVariant(parent, "P12350-2", "not_existing_splice_variant");
        isoform_no_uniprot.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Interaction interaction_8 = getMockBuilder().createInteraction(simple, isoform_no_uniprot);
        for (Component c : interaction_8.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_8);

        // duplicates

        // dupe1 has an invalid range and will be deprecated
        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));
        dupe1.setSequence("LALASSWWAHVEMGPPDPILGVTEAYKRDTNSKK"); // real sequence, insertion of "LALA" upstream

        CvDatabase dip = getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.DIP_MI_REF, CvDatabase.DIP);
        dupe1.addXref(getMockBuilder().createXref(dupe1, "DIP:00001", null, dip));

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe2 = cloner.clone(dupe1);
        dupe2.setShortLabel("dupe2");
        dupe2.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));

        for (Iterator<InteractorXref> xrefIter = dupe2.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12351");
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xrefIter.remove();
            }
        }

        // dupe2 will be kept as original but has the same interaction as dupe3
        dupe2.setCreated(new Date(1)); // dupe2 is older
        // no need to update the sequence
        dupe2.setSequence("SSWWAHVEMGPPDPILGVTEAYKRDTNSKK");
        dupe2.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));

        // dupe 3 has a range which will be shifted before the merge and has an interaction which will be deleted because duplicated participant
        Protein dupe3 = cloner.clone(dupe2);
        dupe3.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));
        dupe3.setShortLabel("dupe3");
        dupe3.setCreated(dupe1.getCreated());

        for (Iterator<InteractorXref> xrefIter = dupe3.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12352");
            }
        }

        // no need to update the sequence
        dupe3.setSequence("SSWWAHVPPDPILGVTEAYKRDTNSKK");

        getCorePersister().saveOrUpdate(dupe1);
        getCorePersister().saveOrUpdate(dupe2);
        getCorePersister().saveOrUpdate(dupe3);

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, simple);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe2, simple);
        Interaction interaction4 = getMockBuilder().createInteraction(dupe3, simple);

        for (Component c : dupe2.getActiveInstances()){
            c.getBindingDomains().clear();
        }
        // add a range in the protein with up-to-date sequence (dupe2)
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        Range range = getMockBuilder().createRange(5, 5, 7, 7);
        feature.addRange(range);
        dupe2.getActiveInstances().iterator().next().addBindingDomain(feature);

        for (Component c : dupe3.getActiveInstances()){
            c.getBindingDomains().clear();
        }
        // add a range which will be shifted (dupe3)
        Feature feature3 = getMockBuilder().createFeatureRandom();
        feature3.getRanges().clear();
        Range range3 = getMockBuilder().createRange(8, 8, 11, 11);
        feature3.addRange(range3);
        dupe3.getActiveInstances().iterator().next().addBindingDomain(feature3);

        // add a bad range in the protein with sequence to update (dupe1). Add another component which will be a duplicated component
        Feature feature2 = getMockBuilder().createFeatureRandom();
        feature2.getRanges().clear();
        Range range2 = getMockBuilder().createRange(1, 1, 4, 4);
        feature2.addRange(range2);

        for (Component c : dupe1.getActiveInstances()){
            c.getBindingDomains().clear();

            if (dupe1.getAc().equals(c.getInteractor().getAc())){
                c.addFeature(feature2);
            }
        }

        // persist the interactions
        getCorePersister().saveOrUpdate(interaction1, interaction2, interaction4);

        // interaction with duplicated component
        Interaction interaction3 = getMockBuilder().createInteraction(dupe2, simple, dupe3);
        for (Component c : interaction3.getComponents()){
            c.getBindingDomains().clear();
        }

        // persist the interactions
        getCorePersister().saveOrUpdate(interaction3);

        Assert.assertEquals(14, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(12, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(25, getDaoFactory().getComponentDao().countAll());

        Assert.assertEquals(1, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        Protein dupe3Refreshed = getDaoFactory().getProteinDao().getByAc(dupe3.getAc());
        InteractorXref uniprotXref2 = ProteinUtils.getIdentityXrefs(dupe3Refreshed).iterator().next();
        uniprotXref2.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());
        getDataContext().commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();
        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());
        Assert.assertEquals(13, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(12, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(24, getDaoFactory().getComponentDao().countAll());
        Assert.assertNull(getDaoFactory().getProteinDao().getByAc(dupe3.getAc()));

        ProteinImpl dupe2FromDb = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        Assert.assertNotNull(dupe2FromDb);
        ProteinImpl dupe1FromDb = getDaoFactory().getProteinDao().getByAc(dupe1.getAc());
        Assert.assertNotNull(dupe1FromDb);
        // xrefs: identity, intact-secondary and dip
        Collection<InteractorXref> dupe2Xrefs = dupe2FromDb.getXrefs();

        boolean intactSecondaryFound = false;
        boolean dipFound = false;

        for (InteractorXref xref : dupe2Xrefs) {
            if (xref.getCvXrefQualifier() != null && "intact-secondary".equals(xref.getCvXrefQualifier().getShortLabel())) {
                Assert.assertEquals(dupe3.getAc(), xref.getPrimaryId());
                intactSecondaryFound = true;
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                dipFound = true;
            }
        }

        Assert.assertTrue("An intact-secondary xref is expected in dupe2", intactSecondaryFound);
        Assert.assertFalse("An dip xref (copied from dupe1) is not expected in dupe2 because not present in uniprot", dipFound);

        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());

        final File duplicatesFile = new File(dir, "duplicates.csv");
        final File deletedFile = new File(dir, "deleted.csv");
        final File createdFile = new File(dir, "created.csv");
        final File nonUniprotFile = new File(dir, "non_uniprot.csv");
        final File updateCasesFile = new File(dir, "update_cases.csv");
        final File sequenceChangedFile = new File(dir, "sequence_changed.fasta");
        final File rangeChangedFile = new File(dir, "range_changed.csv");
        final File featureChangedFile = new File(dir, "feature_changed.csv");
        final File invalidRangeFile = new File(dir, "invalid_range.csv");
        final File outOfDateRangeFile = new File(dir, "out_of_date_range.csv");
        final File deadProteinFile = new File(dir, "dead_proteins.csv");
        final File outOfDateProteinFile = new File(dir, "out_of_date_participants.csv");
        final File erroFile = new File(dir, "process_errors.csv");
        final File secondaryProteinsFile = new File(dir, "secondary_proteins.csv");
        final File transcriptWithSameSequenceFile = new File(dir, "transcript_same_sequence.csv");
        final File invalidIntactParentFile = new File(dir, "updated_intact_parents.csv");
        final File proteinMappingFile = new File(dir, "protein_mapping.csv");
        final File sequenceChangedCautionFile = new File(dir, "sequence_changed_caution.csv");
        final File deletedComponentFile = new File(dir, "deleted_component.csv");

        Assert.assertTrue(duplicatesFile.exists());
        Assert.assertTrue(deletedFile.exists());
        Assert.assertTrue(createdFile.exists());
        Assert.assertTrue(nonUniprotFile.exists());
        Assert.assertTrue(updateCasesFile.exists());
        Assert.assertTrue(sequenceChangedFile.exists());
        Assert.assertTrue(rangeChangedFile.exists());
        Assert.assertTrue(featureChangedFile.exists());
        Assert.assertTrue(invalidRangeFile.exists());
        Assert.assertTrue(outOfDateRangeFile.exists());
        Assert.assertTrue(deadProteinFile.exists());
        Assert.assertTrue(outOfDateProteinFile.exists());
        Assert.assertTrue(erroFile.exists());
        Assert.assertTrue(secondaryProteinsFile.exists());
        Assert.assertTrue(transcriptWithSameSequenceFile.exists());
        Assert.assertTrue(invalidIntactParentFile.exists());
        Assert.assertTrue(proteinMappingFile.exists());
        Assert.assertTrue(sequenceChangedCautionFile.exists());
        Assert.assertTrue(deletedComponentFile.exists());

        // 2 : header plus one duplicate case
        Assert.assertEquals(2, countLinesInFile(duplicatesFile));
        // 3 : header plus one deleted because duplicate and one deleted because no interactions
        Assert.assertEquals(3, countLinesInFile(deletedFile));
        // 2 : header plus one master protein created because one transcript without parents
        Assert.assertEquals(2, countLinesInFile(createdFile));
        // 3 : header plus one 'non-uniprot' protein, plus one protein without uniprot
        Assert.assertEquals(3, countLinesInFile(nonUniprotFile));
        // 6 : header plus simple_protein, transcript without parent, secondary protein, one protein with a splice variant which doesn't exist in uniprot and duplicated prot
        Assert.assertEquals(6, countLinesInFile(updateCasesFile));
        // 2 : header plus one range updated with dupe3
        Assert.assertEquals(2, countLinesInFile(rangeChangedFile));
        // 2 : header plus one invalid range attached to secondary proteins (the out of date range attached to a duplicated protein has not been updated because we kept the duplicate protein as a deprecated protein)
        Assert.assertEquals(2, countLinesInFile(featureChangedFile));
        // 2 : header plus invalid range attached to secondary protein
        Assert.assertEquals(2, countLinesInFile(invalidRangeFile));
        // 2 : header plus out of date range attached to one of the duplicated proteins
        Assert.assertEquals(2, countLinesInFile(outOfDateRangeFile));
        // 3 : header plus dead master protein and one non existing splice variant
        Assert.assertEquals(3, countLinesInFile(deadProteinFile));
        // 3 : header plus secondary protein with invalid range and one of the duplicated protein having an out of date range
        Assert.assertEquals(3, countLinesInFile(outOfDateProteinFile));
        // 5 : header plus one protein with several uniprot identities
        Assert.assertEquals(5, countLinesInFile(erroFile));
        // 2 : header plus one secondary protein updated
        Assert.assertEquals(2, countLinesInFile(secondaryProteinsFile));
        // 2 : header plus one simple protein haing the same sequence as one of its isoforms
        Assert.assertEquals(2, countLinesInFile(transcriptWithSameSequenceFile));
        // 2 : header plus protein transcript without intact parent
        Assert.assertEquals(2, countLinesInFile(invalidIntactParentFile));
        // 2 : header plus dupe3 which is now a duplicated participant
        Assert.assertEquals(2, countLinesInFile(deletedComponentFile));

        getDataContext().commitTransaction(status2);
    }

    private static int countLinesInFile(File file) {
        int count = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            try{
                String str;
                while ((str = in.readLine()) != null) {
                    count++;
                }
            }
            finally{
                in.close();
            }
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
}
