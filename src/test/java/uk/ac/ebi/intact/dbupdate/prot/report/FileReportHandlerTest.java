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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.core.persister.PersisterHelper;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.core.util.SchemaUtils;
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
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class FileReportHandlerTest extends IntactBasicTestCase {

    @Before
    public void before_schema() throws Exception {
        SchemaUtils.createSchema();
    }

    @Test
    public void simulation() throws Exception {
        beginTransaction();
        new ComprehensiveCvPrimer(getDaoFactory()).createCVs();
        commitTransaction();

        final File dir = new File("target/simulation");
        UpdateReportHandler reportHandler = new FileReportHandler(dir);

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig(reportHandler);

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

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
                xref.setPrimaryId("P12346");
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                xrefIter.remove();
            }
        }

        dupe2.setCreated(new Date(1)); // dupe2 is older

        Assert.assertEquals(2, dupe1.getXrefs().size());
        Assert.assertEquals(1, dupe2.getXrefs().size());

        Protein prot1 = getMockBuilder().createProtein("P54999", "ruxf_yeast",
                                                       getMockBuilder().createBioSource(4932, "yeast"));
        prot1.setSequence("MSESSDISAMQPVNPKPFLKGLVNHRVGVKLKFNSTEYRGTLVSTDNYFNLQLNEAEEFVAGVSHGTLGEIFIRCNNVLYIRELPN");
        Protein prot2 = getMockBuilder().createProteinRandom();
        Protein prot3 = getMockBuilder().createProteinRandom();

        Interaction interaction1 = getMockBuilder().createInteraction(dupe1, prot1);
        Interaction interaction2 = getMockBuilder().createInteraction(dupe2, prot2);
        Interaction interaction3 = getMockBuilder().createInteraction(dupe1, prot3);

        // add a range in the protein with updated sequence (dupe2)
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        Range range = getMockBuilder().createRange(5, 5, 7, 7);
        feature.addRange(range);
        dupe2.getActiveInstances().iterator().next().addBindingDomain(feature);

        // persist the interactions
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
        // xrefs: identity, intact-secondary and dip
        Collection<InteractorXref> dupe2Xrefs = dupe2FromDb.getXrefs();

        boolean intactSecondaryFound = false;
        boolean dipFound = false;

        for (InteractorXref xref : dupe2Xrefs) {
            if (xref.getCvXrefQualifier() != null && "intact-secondary".equals(xref.getCvXrefQualifier().getShortLabel())) {
                Assert.assertEquals(dupe1.getAc(), xref.getPrimaryId());
                intactSecondaryFound = true;
            } else if (CvDatabase.DIP_MI_REF.equals(xref.getCvDatabase().getMiIdentifier())) {
                dipFound = true;
            }
        }

        Assert.assertTrue("An intact-secondary xref is expected in dupe2", intactSecondaryFound);
        Assert.assertTrue("An dip xref (copied from dupe1) is expected in dupe2", dipFound);

        Assert.assertEquals(8, dupe2Xrefs.size());
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());

        final File preProcessedFile = new File(dir, "pre_processed.csv");
        final File processedFile = new File(dir, "processed.csv");
        final File duplicatesFile = new File(dir, "duplicates.csv");
        final File deletedFile = new File(dir, "deleted.csv");
        final File createdFile = new File(dir, "created.csv");
        final File nonUniprotFile = new File(dir, "non_uniprot.csv");
        final File updateCasesFile = new File(dir, "update_cases.csv");
        final File sequenceChangedFile = new File(dir, "sequence_changed.fasta");
        final File rangeChangedFile = new File(dir, "range_changed.csv");

        Assert.assertTrue(preProcessedFile.exists());
        Assert.assertTrue(processedFile.exists());
        Assert.assertTrue(duplicatesFile.exists());
        Assert.assertTrue(deletedFile.exists());
        Assert.assertTrue(createdFile.exists());
        Assert.assertTrue(nonUniprotFile.exists());
        Assert.assertTrue(updateCasesFile.exists());
        Assert.assertTrue(sequenceChangedFile.exists());
        Assert.assertTrue(rangeChangedFile.exists());
        
        Assert.assertEquals(5, countLinesInFile(preProcessedFile));
        Assert.assertEquals(5, countLinesInFile(processedFile));
        Assert.assertEquals(2, countLinesInFile(duplicatesFile));
        Assert.assertEquals(2, countLinesInFile(deletedFile));
        Assert.assertEquals(0, countLinesInFile(createdFile));
        Assert.assertEquals(0, countLinesInFile(nonUniprotFile));
        Assert.assertEquals(3, countLinesInFile(updateCasesFile));
        Assert.assertEquals(4, countLinesInFile(sequenceChangedFile));
        Assert.assertEquals(2, countLinesInFile(rangeChangedFile));


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
