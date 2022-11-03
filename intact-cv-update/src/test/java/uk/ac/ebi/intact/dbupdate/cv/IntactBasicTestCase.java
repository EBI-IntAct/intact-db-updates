/*
 * Copyright 2001-2007 The European Bioinformatics Institute.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.After;
import org.junit.Before;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persister.CorePersister;
import uk.ac.ebi.intact.core.unit.IntactMockBuilder;

/**
 * Base for all intact-tests.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations = {
        "classpath*:/META-INF/cvupdate.spring.xml",
        "classpath*:/META-INF/beanscv*.spring.xml"
})
@Transactional("transactionManager")
@DirtiesContext
public abstract class IntactBasicTestCase {

    @Autowired
    private ApplicationContext applicationContext;

    private IntactMockBuilder mockBuilder;

    @Before
    public void prepareBasicTest() {
        mockBuilder = new IntactMockBuilder(getIntactContext().getConfig().getDefaultInstitution());
    }

    @After
    public void afterBasicTest() {
        mockBuilder = null;
    }

    protected IntactContext getIntactContext() {
        return (IntactContext) applicationContext.getBean("intactContext");
    }

    protected DataContext getDataContext() {
        return getIntactContext().getDataContext();
    }

    protected DaoFactory getDaoFactory() {
        return getDataContext().getDaoFactory();
    }

    protected IntactMockBuilder getMockBuilder() {
        return mockBuilder;
    }

    public CorePersister getCorePersister() {
        return getIntactContext().getCorePersister();
    }
}