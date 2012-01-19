package uk.ac.ebi.intact.dbupdate.cv.importer;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateContext;
import uk.ac.ebi.intact.dbupdate.cv.CvUpdateManager;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdaterTest;
import uk.ac.ebi.intact.model.*;

import java.io.IOException;

/**
 * Unit tester of CvImporterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beans.spring.xml"})
public class CvImporterTest extends IntactBasicTestCase{

    private CvUpdateManager cvManager;

    @Autowired
    private CvImporter cvImporter;

    @Before
    public void clear() throws IOException, OntologyLoaderException {
        cvManager = new CvUpdateManager(CvUpdaterTest.class.getResource("/ontologies.xml"), "target/reports");
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_new_import_do_not_include_children() throws IllegalAccessException, InstantiationException {
        String termAc = "MI:0091"; // chromatography technology

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");
        
        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(termAc));
        context.setTermObsolete(false);
        context.setIdentifier(termAc);

        cvImporter.importCv(context, false);

        Assert.assertNotNull(context.getCvTerm());
    
        // object must not be hidden and should be an interaction detection method
        Assert.assertTrue(context.getCvTerm() instanceof CvInteraction);
        Assert.assertEquals(termAc, context.getCvTerm().getIdentifier());
        Assert.assertEquals("chromatography technology", context.getCvTerm().getFullName());
        Assert.assertEquals("chromatography", context.getCvTerm().getShortLabel());
        // one pubmed and one identity
        Assert.assertEquals(2, context.getCvTerm().getXrefs().size());
        // one alias = column chromatography
        Assert.assertEquals(1, context.getCvTerm().getAliases().size());
        // one definition
        Assert.assertEquals(1, context.getCvTerm().getAnnotations().size());
        // no children
        Assert.assertTrue(context.getCvTerm().getChildren().isEmpty());
        // one parent
        Assert.assertEquals(1, context.getCvTerm().getParents().size());
        Assert.assertFalse(isTermHidden(context.getCvTerm()));

        // all parents must be hidden and should be interaction detection method
        CvDagObject parent = context.getCvTerm().getParents().iterator().next();
        Assert.assertTrue(parent instanceof CvInteraction);
        Assert.assertEquals("MI:0401", parent.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent.getParents().size());
        Assert.assertTrue(isTermHidden(parent));
        
        CvDagObject parent2 = parent.getParents().iterator().next();
        Assert.assertTrue(parent2 instanceof CvInteraction);
        Assert.assertEquals("MI:0045", parent2.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent2.getParents().size());
        Assert.assertTrue(isTermHidden(parent2));

        CvDagObject parent3 = parent2.getParents().iterator().next();
        Assert.assertTrue(parent3 instanceof CvInteraction);
        Assert.assertEquals("MI:0001", parent3.getIdentifier());
        // no parents because the parent is the root term
        Assert.assertEquals(0, parent3.getParents().size());
        Assert.assertTrue(isTermHidden(parent3));
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_new_import_include_children() throws IllegalAccessException, InstantiationException {
        String termAc = "MI:0091"; // chromatography technology

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(termAc));
        context.setTermObsolete(false);
        context.setIdentifier(termAc);

        cvImporter.importCv(context, true);

        Assert.assertNotNull(context.getCvTerm());

        // object must not be hidden and should be an interaction detection method
        Assert.assertTrue(context.getCvTerm() instanceof CvInteraction);
        Assert.assertEquals(termAc, context.getCvTerm().getIdentifier());
        Assert.assertEquals("chromatography technology", context.getCvTerm().getFullName());
        Assert.assertEquals("chromatography", context.getCvTerm().getShortLabel());
        // one pubmed and one identity
        Assert.assertEquals(2, context.getCvTerm().getXrefs().size());
        // one alias = column chromatography
        Assert.assertEquals(1, context.getCvTerm().getAliases().size());
        // one definition
        Assert.assertEquals(1, context.getCvTerm().getAnnotations().size());
        // 1 child
        Assert.assertEquals(1, context.getCvTerm().getChildren().size());
        // one parent
        Assert.assertEquals(1, context.getCvTerm().getParents().size());
        Assert.assertFalse(isTermHidden(context.getCvTerm()));

        // all children cannot be hidden and should be interaction detection method
        CvDagObject child = context.getCvTerm().getChildren().iterator().next();
        Assert.assertTrue(child instanceof CvInteraction);
        Assert.assertEquals("MI:0004", child.getIdentifier());
        // no child
        Assert.assertTrue(child.getChildren().isEmpty());
        // two parents
        Assert.assertEquals(2, child.getParents().size());
        Assert.assertFalse(isTermHidden(child));

        // all parents must be hidden and should be interaction detection method
        CvDagObject parent = context.getCvTerm().getParents().iterator().next();
        Assert.assertTrue(parent instanceof CvInteraction);
        Assert.assertEquals("MI:0401", parent.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent.getParents().size());
        Assert.assertTrue(isTermHidden(parent));

        CvDagObject parent2 = parent.getParents().iterator().next();
        Assert.assertTrue(parent2 instanceof CvInteraction);
        Assert.assertEquals("MI:0045", parent2.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent2.getParents().size());
        Assert.assertTrue(isTermHidden(parent2));

        CvDagObject parent3 = parent2.getParents().iterator().next();
        Assert.assertTrue(parent3 instanceof CvInteraction);
        Assert.assertEquals("MI:0001", parent3.getIdentifier());
        // no parents because the parent is the root term
        Assert.assertEquals(0, parent3.getParents().size());
        Assert.assertTrue(isTermHidden(parent3));
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_import_existing_parent() throws IllegalAccessException, InstantiationException {
        String termAc = "MI:0091"; // chromatography technology

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvInteraction.class, "MI:0401", "biochemical");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);
        
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0401"));
        
        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(termAc));
        context.setTermObsolete(false);
        context.setIdentifier(termAc);

        cvImporter.importCv(context, false);

        TransactionStatus status2 = getDataContext().beginTransaction();

        Assert.assertNotNull(context.getCvTerm());

        CvDagObject reloadedTerm = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(context.getCvTerm().getAc());

        // object must not be hidden and should be an interaction detection method
        Assert.assertTrue(reloadedTerm instanceof CvInteraction);
        Assert.assertEquals(termAc, reloadedTerm.getIdentifier());
        Assert.assertEquals("chromatography technology", reloadedTerm.getFullName());
        Assert.assertEquals("chromatography", reloadedTerm.getShortLabel());
        // one pubmed and one identity
        Assert.assertEquals(2, reloadedTerm.getXrefs().size());
        // one alias = column chromatography
        Assert.assertEquals(1, reloadedTerm.getAliases().size());
        // one definition
        Assert.assertEquals(1, reloadedTerm.getAnnotations().size());
        // no children
        Assert.assertTrue(reloadedTerm.getChildren().isEmpty());
        // one parent
        Assert.assertEquals(1, reloadedTerm.getParents().size());
        Assert.assertFalse(isTermHidden(reloadedTerm));

        // all parents must be hidden excepted for biochemical which already existed and should be interaction detection method
        CvDagObject parent = reloadedTerm.getParents().iterator().next();
        Assert.assertTrue(parent instanceof CvInteraction);
        // biochemical already exists
        Assert.assertEquals(cv.getAc(), parent.getAc());
        Assert.assertEquals("MI:0401", parent.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent.getParents().size());
        Assert.assertFalse(isTermHidden(parent));

        CvDagObject parent2 = parent.getParents().iterator().next();
        Assert.assertTrue(parent2 instanceof CvInteraction);
        Assert.assertEquals("MI:0045", parent2.getIdentifier());
        // one parent
        Assert.assertEquals(1, parent2.getParents().size());
        Assert.assertTrue(isTermHidden(parent2));

        CvDagObject parent3 = parent2.getParents().iterator().next();
        Assert.assertTrue(parent3 instanceof CvInteraction);
        Assert.assertEquals("MI:0001", parent3.getIdentifier());
        // no parents because the parent is the root term
        Assert.assertEquals(0, parent3.getParents().size());
        Assert.assertTrue(isTermHidden(parent3));

        getDataContext().commitTransaction(status2);
    }

    private boolean isTermHidden(CvDagObject term){
       
        for (Annotation annot : term.getAnnotations()){
            if (CvTopic.HIDDEN.equals(annot.getCvTopic().getShortLabel())){
                return true;
            }
        }
        
        return false;
    }
}
