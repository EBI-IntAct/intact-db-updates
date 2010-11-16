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

import org.junit.Assert;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Protein;

/**
 * Tester of UniprotProteinUpdater
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class UniprotProteinUpdaterTest extends IntactBasicTestCase {

    private class DummyProcessor extends ProteinProcessor {
        protected void registerListeners() { }
    }

    @Test
    @DirtiesContext
    public void onPreProcess_uniprot() throws Exception{
        Protein prot = getMockBuilder().createProteinRandom();
        
        ProteinProcessor processor = new DummyProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);

        UniprotProteinUpdater listener = new UniprotProteinUpdater();
        //listener.onPreProcess(evt);

        //Assert.assertFalse(processor.isFinalizationRequested());
    }
    
    @Test
    @DirtiesContext
    public void onPreProcess_nonUniprot() throws Exception{
        Protein prot = getMockBuilder().createProteinRandom();
        prot.getXrefs().clear();
        prot.addAnnotation(getMockBuilder().createAnnotation("nonUniprot", null, CvTopic.NON_UNIPROT));

        ProteinProcessor processor = new DummyProcessor();

        ProteinEvent evt = new ProteinEvent(processor, null, prot);

        UniprotProteinUpdater listener = new UniprotProteinUpdater();
        //listener.onPreProcess(evt);

        //Assert.assertTrue(processor.isFinalizationRequested());
    }
}
