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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.UniprotProteinUpdater;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotXref;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.*;

/**
 * Tester of UniprotProteinUpdater
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
public class UniprotProteinUpdaterTest extends IntactBasicTestCase {

    private UniprotProteinUpdater updater;

    @Before
    public void before() throws Exception {
        updater = new UniprotProteinUpdater();
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
    public void create_master_protein() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, new ArrayList<Protein>(),
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST);
        evt.setUniprotServiceResult(new UniprotServiceResult(uniprot.getPrimaryAc()));

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

        Assert.assertEquals(12, createdProtein.getXrefs().size());

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
}
