package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.After;
import org.junit.Assert;
import org.junit.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdateException;
import uk.ac.ebi.intact.model.*;

import java.io.File;
import java.sql.SQLException;

/**
 * Tester of global cv update runner
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25/01/12</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beanscv*.spring.xml"})
public class GlobalCvUpdateRunnerTest extends IntactBasicTestCase{

    @Autowired
    GlobalCvUpdateRunner globalCvUpdateRunner;

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
    public void test_update_allTerms() throws CvUpdateException {
        // create terms to update, parents to create, obsolete terms and obsolete terms remapped to other ontology
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvInteraction.class, "MI:0091", "chromatography technology");
        CvDagObject cv = getMockBuilder().createCvObject(CvInteraction.class, "MI:0004", "affinity chromatography technology");
        CvDagObject cvObsoleteSameOntology = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        CvDagObject cvObsoleteDifferentOntology = getMockBuilder().createCvObject(CvFeatureType.class, "MI:0123", "n2-acetyl");

        getCorePersister().saveOrUpdate(cv, parent, cvObsoleteSameOntology, cvObsoleteDifferentOntology);

        getDataContext().commitTransaction(status);

        Assert.assertNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0669"));

        globalCvUpdateRunner.updateAllTerms("MI");

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
        // the missing parent has been created
        Assert.assertEquals(2, term.getParents().size());

        CvObject term1 = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cvObsoleteSameOntology.getAc());
        Assert.assertEquals("MI:0933", term1.getIdentifier());

        // updated!!!!
        CvObject term2 = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cvObsoleteDifferentOntology.getAc());
        Assert.assertEquals("MOD:00359", term2.getIdentifier());
        Assert.assertEquals("acarg", term2.getShortLabel());
        Assert.assertEquals(1, term2.getAnnotations().size());

        // no new term have been created (comment should not have been created)
        Assert.assertNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0669"));

        getDataContext().commitTransaction(status2);
    }

    @Test
    @DirtiesContext
    public void test_update_and_create_allTerms() throws CvUpdateException {
        // create terms to update, parents to create, obsolete terms and obsolete terms remapped to other ontology
        TransactionStatus status = getDataContext().beginTransaction();

        CvInteractionType existingCv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0933", "negative gen int");

        CvDagObject parent = getMockBuilder().createCvObject(CvInteraction.class, "MI:0091", "chromatography technology");
        CvDagObject cv = getMockBuilder().createCvObject(CvInteraction.class, "MI:0004", "affinity chromatography technology");
        CvInteractionType cvObsoleteSameOntology = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        CvDagObject cvObsoleteDifferentOntology = getMockBuilder().createCvObject(CvFeatureType.class, "MI:0123", "n2-acetyl");

        Interaction interaction = getMockBuilder().createDeterministicInteraction();
        interaction.setCvInteractionType(cvObsoleteSameOntology);

        getCorePersister().saveOrUpdate(cv, parent, cvObsoleteSameOntology, cvObsoleteDifferentOntology, interaction, existingCv);

        getDataContext().commitTransaction(status);

        globalCvUpdateRunner.updateAndCreateAllTerms("MI");

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
        // the missing parent has been created
        Assert.assertEquals(2, term.getParents().size());

        Assert.assertNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cvObsoleteSameOntology.getAc()));
        CvObject term1 = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());
        Assert.assertEquals("MI:0933", term1.getIdentifier());

        // updated!!!!
        CvObject term2 = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cvObsoleteDifferentOntology.getAc());
        Assert.assertEquals("MOD:00359", term2.getIdentifier());
        Assert.assertEquals("acarg", term2.getShortLabel());
        Assert.assertEquals(1, term2.getAnnotations().size());

        // no new term have been created (comment should not have been created)
        Assert.assertNotNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByIdentifier("MI:0669"));

        // obsolete term has been remapped and interaction updated
        Interaction interDb = getDaoFactory().getInteractionDao().getByAc(interaction.getAc());

        Assert.assertEquals(existingCv.getAc(), interDb.getCvInteractionType().getAc());

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
