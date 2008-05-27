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

import java.io.File;
import java.util.Collection;
import java.util.Date;

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

        UpdateReportHandler reportHandler = new FileReportHandler(new File("target"));

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig(reportHandler);

        getMockBuilder().createInstitution(CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);

        Protein dupe1 = getMockBuilder().createDeterministicProtein("P12345", "dupe1");
        dupe1.setBioSource(getMockBuilder().createBioSource(9986, "Oryctolagus cuniculus"));

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
        // xrefs: identity and intact-secondary
        Collection<InteractorXref> dupe2Xrefs = dupe2FromDb.getXrefs();
        Assert.assertEquals(2, dupe2Xrefs.size());
        Assert.assertEquals(3, dupe2FromDb.getActiveInstances().size());

        for (InteractorXref xref : dupe2Xrefs) {
            if ("intact-secondary".equals(xref.getCvXrefQualifier().getShortLabel())) {
                Assert.assertEquals(dupe1.getAc(), xref.getPrimaryId());
            }
        }
    }
}
