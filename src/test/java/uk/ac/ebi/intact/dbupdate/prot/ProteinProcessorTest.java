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

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.core.persister.PersisterHelper;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.core.util.SchemaUtils;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;

import java.util.Date;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinProcessorTest extends IntactBasicTestCase {

    @Before
    public void before_schema() throws Exception {
        SchemaUtils.createSchema();
    }

    @Test
    public void updateAll_oneElementToBeProcessedRemoved() throws Exception {

         final Protein protMain = getMockBuilder().createProtein("P12345", "main");
         protMain.setCreated(new Date(1));
         final Protein protRemoved = getMockBuilder().createProtein("Q01010", "removed");

         final Protein prot1 = getMockBuilder().createProteinRandom();
         final Protein prot2 = getMockBuilder().createProteinRandom();
         final Protein prot3 = getMockBuilder().createProteinRandom();
         final Protein prot4 = getMockBuilder().createProteinRandom();

         PersisterHelper.saveOrUpdate(prot1, protMain, prot2, prot3, protRemoved, prot4);

         Assert.assertNotNull(protRemoved.getAc());

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

}
