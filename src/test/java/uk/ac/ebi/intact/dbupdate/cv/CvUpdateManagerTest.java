package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdateException;
import uk.ac.ebi.intact.model.*;

import java.io.File;
import java.sql.SQLException;
import java.util.Arrays;

/**
 * Unit tester of CvUpdateManager
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beanscv*.spring.xml"})
public class CvUpdateManagerTest extends IntactBasicTestCase{

    @Autowired
    CvUpdateManager cvManager;

    @After
    public void removeUnnecessaryFolders() throws SQLException {
        File reports = new File("reports");

        if (reports.exists()){
            File[] files = reports.listFiles();

            for (File f : files){
                f.delete();
            }

            reports.delete();
        }
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_update_cv() throws CvUpdateException {
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvInteraction.class, "MI:0091", "chromatography technology");
        CvDagObject cv = getMockBuilder().createCvObject(CvInteraction.class, "MI:0004", "affinity chromatography technology");

        getCorePersister().saveOrUpdate(cv, parent);

        getDataContext().commitTransaction(status);

        cvManager.updateCv(cv.getAc(), "MI");

        TransactionStatus status2 = getDataContext().beginTransaction();
        
        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        Assert.assertEquals(2, term.getXrefs().size());

        for (CvObjectXref ref : term.getXrefs()){
            if (ref.getPrimaryId().equals("7708014")){
                Assert.assertEquals(CvDatabase.PUBMED_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.PRIMARY_REFERENCE_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else if (ref.getPrimaryId().equals("MI:0004")){
                Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertEquals(1, term.getAnnotations().size());
        Annotation def = term.getAnnotations().iterator().next();
        Assert.assertEquals(def.getCvTopic().getShortLabel(), CvTopic.DEFINITION);
        Assert.assertEquals(def.getAnnotationText(), "This class of approaches is characterised by the use of affinity resins as tools to purify molecule of interest (baits) and their binding partners. The baits can be captured by a variety of high affinity ligands linked to a resin - for example, antibodies specific for the bait itself, antibodies for specific tags engineered to be expressed as part of the bait or other high affinity binders such as glutathione resins for GST fusion proteins, metal resins for histidine-tagged proteins.");

        Assert.assertEquals(1, term.getAliases().size());
        Assert.assertEquals("Affinity purification", term.getAliases().iterator().next().getName());
        Assert.assertEquals(1, term.getParents().size());

        for (CvDagObject p : term.getParents()){
            Assert.assertEquals(parent.getAc(), p.getAc());
        }

        getDataContext().commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_import_cv_no_children() throws CvUpdateException {
        String termAc = "MI:0091"; // chromatography technology
        
        CvDagObject term = cvManager.importCvTerm(termAc, "MI", false);
        
        Assert.assertNotNull(term);
        Assert.assertNotNull(term.getAc());
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0001"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0045"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0401"));
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_import_cv_children() throws CvUpdateException {
        String termAc = "MI:0091"; // chromatography technology

        CvDagObject term = cvManager.importCvTerm(termAc, "MI", true);

        Assert.assertNotNull(term);
        Assert.assertNotNull(term.getAc());
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0001"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0045"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0401"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0004"));
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0400"));
    }

    @Test
    @DirtiesContext
    public void test_import_cv_parentFromOtherOntology() throws CvUpdateException {
        String termAc = "MOD:00003"; // mod term

        CvDagObject term = cvManager.importCvTerm(termAc, "MOD", false);

        Assert.assertNotNull(term);
        Assert.assertNotNull(term.getAc());

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject mod = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MOD:00032");
        Assert.assertNotNull(mod);
        Assert.assertEquals(1, mod.getChildren().size());
        Assert.assertEquals(1, mod.getParents().size());
        CvDagObject bioFeature = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0252");
        Assert.assertNotNull(bioFeature);
        Assert.assertEquals(1, bioFeature.getChildren().size());
        
        Assert.assertEquals(bioFeature.getChildren().iterator().next(), mod.getParents().iterator().next());

        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0116"));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_unHide_cv() throws CvUpdateException {
        String termAc = "MI:0091"; // chromatography technology

        cvManager.importCvTerm(termAc, "MI", false);

        TransactionStatus status = getDataContext().beginTransaction();
        CvObject cvHidden = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0045");
        
        Assert.assertTrue(isTermHidden(cvHidden));

        getDataContext().commitTransaction(status);

        cvManager.removeHiddenFrom(Arrays.asList(cvHidden));

        TransactionStatus status2 = getDataContext().beginTransaction();
        CvObject cv = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0045");

        Assert.assertFalse(isTermHidden(cv));

        getDataContext().commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_hide_cv() throws CvUpdateException {
        String termAc = "MI:0091"; // chromatography technology

        cvManager.importCvTerm(termAc, "MI", false);

        TransactionStatus status = getDataContext().beginTransaction();
        CvObject cvHidden = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier(termAc);

        Assert.assertFalse(isTermHidden(cvHidden));

        getDataContext().commitTransaction(status);

        cvManager.hideTerms(Arrays.asList(cvHidden), "obsolete");

        TransactionStatus status2 = getDataContext().beginTransaction();
        CvObject cv = getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier(termAc);

        Assert.assertTrue(isTermHidden(cv));

        getDataContext().commitTransaction(status2);
    }

    private boolean isTermHidden(CvObject term){

        for (Annotation annot : term.getAnnotations()){
            if (CvTopic.HIDDEN.equals(annot.getCvTopic().getShortLabel())){
                return true;
            }
        }

        return false;
    }
}
