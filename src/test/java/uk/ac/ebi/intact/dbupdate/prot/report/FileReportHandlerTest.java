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
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.io.*;
import java.util.Collection;
import java.util.Date;
import java.util.Iterator;

/**
 * FileReportHandler Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml", "/META-INF/standalone/update-jpa.spring.xml"} )
public class FileReportHandlerTest extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

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
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void simulation() throws Exception {

        TransactionStatus status = getDataContext().beginTransaction();

        final File dir = new File("target/simulation");
        UpdateReportHandler reportHandler = new FileReportHandler(dir);
        ProteinUpdateContext.getInstance().getConfig().setReportHandler( reportHandler );
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate(true);

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

        // one protein for all interactions
        Protein simple = getMockBuilder().createProtein("P12347", "simple_protein");
        simple.getBioSource().setTaxId("35845");
        getCorePersister().saveOrUpdate(simple);

        // one protein no uniprot update with one interaction
        Protein noUniProtUpdate = getMockBuilder().createProtein("P12346", "no_uniprot_update");
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
        Protein noUniProtIdentity = getMockBuilder().createProtein("P12346", "no_uniprot_identity");
        InteractorXref identity = ProteinUtils.getUniprotXref(noUniProtIdentity);
        noUniProtIdentity.removeXref(identity);
        getCorePersister().saveOrUpdate(noUniProtIdentity);

        Interaction interaction_2 = getMockBuilder().createInteraction(simple, noUniProtIdentity);
        for (Component c : interaction_2.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_2);

        // one protein with several uniprot identities and with an interaction
        Protein severalUniProtIdentity = getMockBuilder().createProtein("P12346", "several_uniprot_identities");
        InteractorXref identity2 = getMockBuilder().createXref(severalUniProtIdentity, "P12345",
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

        // one secondary protein
        Protein secondary = getMockBuilder().createProtein("O34373", "secondary_protein");
        secondary.getBioSource().setTaxId("1423");
        getCorePersister().saveOrUpdate(secondary);

        Interaction interaction_5 = getMockBuilder().createInteraction(simple, secondary);
        for (Component c : interaction_5.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_5);

        // one protein with bad organism
        Protein bad_organism = getMockBuilder().createProtein("P12349", "bad_organism");
        bad_organism.getBioSource().setTaxId("9606");    // is 7244 in uniprot
        getCorePersister().saveOrUpdate(bad_organism);

        Interaction interaction_6 = getMockBuilder().createInteraction(simple, bad_organism);
        for (Component c : interaction_6.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_6);

        // one protein with a splice variant which doesn't exist in uniprot but contains one interaction
        Protein parent = getMockBuilder().createProtein("P12350", "not_existing_splice_variant");
        parent.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Protein isoform_no_uniprot = getMockBuilder().createProteinSpliceVariant(parent, "P12350-2", "not_existing_splice_variant");
        isoform_no_uniprot.getBioSource().setTaxId("5938");
        getCorePersister().saveOrUpdate(parent);

        Interaction interaction_7 = getMockBuilder().createInteraction(simple, isoform_no_uniprot);
        for (Component c : interaction_7.getComponents()){
            c.getBindingDomains().clear();
        }
        getCorePersister().saveOrUpdate(interaction_7);

        // duplicates
        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));
        dupe1.setSequence("LALASSWWAHVEMGPPDPILGVTEAYKRDTNSKK"); // real sequence, insertion of "LALA" upstream

        CvDatabase dip = getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.DIP_MI_REF, CvDatabase.DIP);
        dupe1.addXref(getMockBuilder().createXref(dupe1, "DIP:00001", null, dip));

        IntactCloner cloner = new IntactCloner(true);
        Protein dupe2 = cloner.clone(dupe1);

        for (Iterator<InteractorXref> xrefIter = dupe2.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12351");
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xrefIter.remove();
            }
        }

        dupe2.setCreated(new Date(1)); // dupe2 is older
        // no need to update the sequence
        dupe2.setSequence("SSWWAHVEMGPPDPILGVTEAYKRDTNSKK");

        Protein dupe3 = cloner.clone(dupe2);

        for (Iterator<InteractorXref> xrefIter = dupe3.getXrefs().iterator(); xrefIter.hasNext();) {
            InteractorXref xref =  xrefIter.next();
            if (CvDatabase.UNIPROT_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xref.setPrimaryId("P12352");
            }
        }

        // no need to update the sequence
        dupe3.setSequence("SSWWAHVEMGPPDPILGVTEAYKRDTNSKK");

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, simple);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe2, simple);
        Interaction interaction3 = getMockBuilder().createInteraction(dupe1, simple);
        Interaction interaction4 = getMockBuilder().createInteraction(dupe3, simple);

        // add a range in the protein with up-to-date sequence (dupe2)
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        Range range = getMockBuilder().createRange(5, 5, 7, 7);
        feature.addRange(range);
        dupe2.getActiveInstances().iterator().next().addBindingDomain(feature);

        // add a bad range in the protein with sequence to update (dupe2)
        Feature feature2 = getMockBuilder().createFeatureRandom();
        feature2.getRanges().clear();
        Range range2 = getMockBuilder().createRange(1, 1, 4, 4);
        feature2.addRange(range2);
        dupe1.getActiveInstances().iterator().next().addBindingDomain(feature2);

        // persist the interactions
        getCorePersister().saveOrUpdate(dupe1);
        getCorePersister().saveOrUpdate(dupe2);
        getCorePersister().saveOrUpdate(dupe3);
        getCorePersister().saveOrUpdate(interaction1, interaction2, interaction3, interaction4);

        Assert.assertEquals(13, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(11, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(22, getDaoFactory().getComponentDao().countAll());

        Assert.assertEquals(2, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        Protein dupe2Refreshed = getDaoFactory().getProteinDao().getByAc(dupe2.getAc());
        InteractorXref uniprotXref = ProteinUtils.getIdentityXrefs(dupe2Refreshed).iterator().next();
        uniprotXref.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());

        Protein dupe3Refreshed = getDaoFactory().getProteinDao().getByAc(dupe3.getAc());
        InteractorXref uniprotXref2 = ProteinUtils.getIdentityXrefs(dupe3Refreshed).iterator().next();
        uniprotXref2.setPrimaryId("P12345");
        getDaoFactory().getXrefDao(InteractorXref.class).update(uniprotXref2);

        Assert.assertEquals(4, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());
        getDataContext().commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor();
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();
        Assert.assertEquals(3, getDaoFactory().getProteinDao().getByUniprotId("P12345").size());
        Assert.assertEquals(11, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(11, getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(22, getDaoFactory().getComponentDao().countAll());
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
        final File invalidRangeFile = new File(dir, "invalid_range.csv");
        final File deadProteinFile = new File(dir, "dead_proteins.csv");
        final File outOfDateProteinFile = new File(dir, "out_of_date_participants.csv");
        final File erroFile = new File(dir, "process_errors.csv");
        final File secondaryProteinsFile = new File(dir, "secondary_proteins.csv");

        Assert.assertTrue(duplicatesFile.exists());
        Assert.assertTrue(deletedFile.exists());
        Assert.assertTrue(createdFile.exists());
        Assert.assertTrue(nonUniprotFile.exists());
        Assert.assertTrue(updateCasesFile.exists());
        Assert.assertTrue(sequenceChangedFile.exists());
        Assert.assertTrue(rangeChangedFile.exists());
        Assert.assertTrue(invalidRangeFile.exists());
        Assert.assertTrue(deadProteinFile.exists());
        Assert.assertTrue(outOfDateProteinFile.exists());
        Assert.assertTrue(erroFile.exists());
        Assert.assertTrue(secondaryProteinsFile.exists());

        Assert.assertEquals(2, countLinesInFile(duplicatesFile));
        Assert.assertEquals(3, countLinesInFile(deletedFile));
        Assert.assertEquals(0, countLinesInFile(createdFile));
        Assert.assertEquals(4, countLinesInFile(nonUniprotFile));
        Assert.assertEquals(6, countLinesInFile(updateCasesFile));
        //Assert.assertEquals(5, countLinesInFile(sequenceChangedFile));
        Assert.assertEquals(0, countLinesInFile(rangeChangedFile));
        Assert.assertEquals(0, countLinesInFile(invalidRangeFile)); // TODO can be changed later
        Assert.assertEquals(3, countLinesInFile(deadProteinFile));
        Assert.assertEquals(2, countLinesInFile(outOfDateProteinFile));
        Assert.assertEquals(7, countLinesInFile(erroFile));
        Assert.assertEquals(2, countLinesInFile(secondaryProteinsFile));

        getDataContext().commitTransaction(status2);
    }

    private static int countLinesInFile(File file) {
        int count = 0;
        try {
            BufferedReader in = new BufferedReader(new FileReader(file));
            String str;
            while ((str = in.readLine()) != null) {
                count++;
            }
            in.close();
        } catch (IOException e) {
            e.printStackTrace();
        }
        return count;
    }
}
