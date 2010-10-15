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
package uk.ac.ebi.intact.dbupdate.prot.referencefilter;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.uniprot.service.referenceFilter.CrossReferenceFilter;

/**
 * IntactCrossReferenceSelector Tester.
 *
 * @author <Authors name>
 * @version 1.0
 * @since <pre>10/19/2006</pre>
 */
public class IntactCrossReferenceFilterTest {

    private CrossReferenceFilter filter;

    @Before
    public void before() {
       filter = new IntactCrossReferenceFilter();
    }

    @After
    public void after() {
        filter = null;
    }

    @Test
    public void isSelected() {
        Assert.assertTrue( filter.isSelected( "GO" ) );
        Assert.assertTrue( filter.isSelected( "go" ) );
        Assert.assertTrue( filter.isSelected( "Go" ) );
        Assert.assertTrue( filter.isSelected( " gO " ) );
        Assert.assertTrue( filter.isSelected( "interpro" ) );
        Assert.assertTrue( filter.isSelected( "SGD" ) );
        Assert.assertTrue( filter.isSelected( "FlyBase" ) );
        Assert.assertTrue( filter.isSelected( "Refseq" ) );
        Assert.assertTrue( filter.isSelected( "refseq" ) );
        Assert.assertTrue( filter.isSelected( "reactome" ) );
        Assert.assertTrue( filter.isSelected( "Reactome" ) );

        Assert.assertFalse( filter.isSelected( "GOGO" ) );
        Assert.assertFalse( filter.isSelected( "gogo" ) );
        Assert.assertFalse( filter.isSelected( "foobar" ) );
    }
    
    @Test (expected = IllegalArgumentException.class)
    public void isSelected_null() throws Exception {
        filter.isSelected( null );
    }

    @Test (expected = IllegalArgumentException.class)
    public void isSelected_empty() throws Exception {
        filter.isSelected( "" );
    }
}

