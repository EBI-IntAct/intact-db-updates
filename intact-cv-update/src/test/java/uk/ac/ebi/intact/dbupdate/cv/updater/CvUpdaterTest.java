package uk.ac.ebi.intact.dbupdate.cv.updater;

import org.junit.After;
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
import uk.ac.ebi.intact.model.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;

/**
 * Unit tester of the CvUpdaterImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beanscv*.spring.xml"})
public class CvUpdaterTest extends IntactBasicTestCase{

    private CvUpdateManager cvManager;

    @Autowired
    private CvUpdater cvUpdater;

    @Before
    public void clear() throws IOException, OntologyLoaderException {
        cvManager = new CvUpdateManager(CvUpdaterTest.class.getResource("/ontologies.xml"), "target/reports");
    }

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
    public void test_update_wrongIdentifier_xrefsAdded_annotationsAdded_parentsMissing() throws CvUpdateException {
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
        getCorePersister().saveOrUpdate(cv);

        cv.setIdentifier("IA:xxxxx");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(CvTopic.COMMENT_MI_REF));
        context.setIdentifier(CvTopic.COMMENT_MI_REF);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        cvUpdater.updateTerm(context);

        getDataContext().commitTransaction(status2);

        Assert.assertEquals(CvTopic.COMMENT_MI_REF, term.getIdentifier());
        Assert.assertEquals(2, term.getXrefs().size());

        for (CvObjectXref ref : term.getXrefs()){
            if (ref.getPrimaryId().equals("14755292")){
                Assert.assertEquals(CvDatabase.PUBMED_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.PRIMARY_REFERENCE_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else if (ref.getPrimaryId().equals(CvTopic.COMMENT_MI_REF)){
                Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertEquals(2, term.getAnnotations().size());
        Iterator<Annotation> itAnnot = term.getAnnotations().iterator();
        Annotation def = itAnnot.next();
        Assert.assertEquals(def.getCvTopic().getShortLabel(), CvTopic.DEFINITION);
        Assert.assertEquals(def.getAnnotationText(), "Comment for public view. This attribute can be associated to interaction, experiment, CV term, an organism and any participant.");
        Annotation usedInClass = itAnnot.next();
        Assert.assertEquals(usedInClass.getCvTopic().getShortLabel(), CvTopic.USED_IN_CLASS);

        Assert.assertTrue(term.getAliases().isEmpty());
        Assert.assertEquals(0, term.getParents().size());
        Assert.assertEquals(6, cvUpdater.getMissingParents().size());

        for (String t : cvUpdater.getMissingParents().keySet()){
            if (!"MI:0664".equals(t) && !"MI:0665".equals(t) && !"MI:0666".equals(t) && !"MI:0667".equals(t)
                    && !"MI:0668".equals(t) && !"MI:0669".equals(t)){
                Assert.assertTrue(false);
            }
        }
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_update_noIdentityXref_xrefsRemoved_aliasRemoved_annotationsRemoved_existingParent_removeParent() throws CvUpdateException {
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvTopic.class, "MI:0664", "interaction attribute name");
        CvDagObject parent2 = getMockBuilder().createCvObject(CvTopic.class, "IA:12345", "other attribute name");
        parent2.getXrefs().iterator().next().getCvDatabase().setIdentifier(CvDatabase.INTACT_MI_REF);
        CvDagObject parent3 = getMockBuilder().createCvObject(CvTopic.class, CvTopic.AUTHOR_CONFIDENCE_MI_REF, CvTopic.AUTHOR_CONFIDENCE);

        CvDagObject cv = getMockBuilder().createCvObject(CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
        cv.getXrefs().clear();

        cv.getAliases().add(getMockBuilder().createAlias(cv, "test", null, "go-synonyms"));
        cv.getXrefs().add(getMockBuilder().createXref(cv, "12345",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.PRIMARY_REFERENCE_MI_REF, CvXrefQualifier.PRIMARY_REFERENCE),
                getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.PUBMED_MI_REF, CvDatabase.PUBMED)));
        cv.getAnnotations().add(getMockBuilder().createAnnotation("bla bla", CvTopic.COMMENT_MI_REF, CvTopic.COMMENT));
        cv.getAnnotations().add(getMockBuilder().createAnnotation("uk.ac.ebi.intact.model.InteractionImpl", getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.USED_IN_CLASS)));

        cv.addParent(parent);
        cv.addParent(parent2);
        cv.addParent(parent3);
        getCorePersister().saveOrUpdate(parent, cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(CvTopic.COMMENT_MI_REF));
        context.setIdentifier(CvTopic.COMMENT_MI_REF);
        context.setCvTerm(term);

        cvUpdater.updateTerm(context);

        getDataContext().commitTransaction(status2);

        Assert.assertEquals(2, term.getXrefs().size());

        for (CvObjectXref ref : term.getXrefs()){
            if (ref.getPrimaryId().equals("14755292")){
                Assert.assertEquals(CvDatabase.PUBMED_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.PRIMARY_REFERENCE_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else if (ref.getPrimaryId().equals(CvTopic.COMMENT_MI_REF)){
                Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, ref.getCvDatabase().getIdentifier());
                Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, ref.getCvXrefQualifier().getIdentifier());
            }
            else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertEquals(2, term.getAnnotations().size());

        for (Annotation ann : term.getAnnotations()){
            if (ann.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.DEFINITION)){
                Assert.assertEquals(ann.getAnnotationText(), "Comment for public view. This attribute can be associated to interaction, experiment, CV term, an organism and any participant.");
            }
            else if (ann.getCvTopic().getShortLabel().equalsIgnoreCase(CvTopic.USED_IN_CLASS)){
                Assert.assertNotSame(ann.getAnnotationText(), "uk.ac.ebi.intact.model.InteractionImpl");
            }
            else {
                Assert.assertTrue(false);
            }
        }

        Assert.assertTrue(term.getAliases().isEmpty());

        Assert.assertEquals(5, cvUpdater.getMissingParents().size());

        for (String t : cvUpdater.getMissingParents().keySet()){
            if (!"MI:0665".equals(t) && !"MI:0666".equals(t) && !"MI:0667".equals(t)
                    && !"MI:0668".equals(t) && !"MI:0669".equals(t)){
                Assert.assertTrue(false);
            }
        }

        Assert.assertEquals(2, term.getParents().size());

        Assert.assertEquals(0, getDaoFactory().getAliasDao(CvObjectAlias.class).getAll().size());
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_update_wrongIdentifier_aliasAdded_createdParent() throws CvUpdateException {
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvTopic.class, "MI:0091", "chromatography technology");
        CvDagObject cv = getMockBuilder().createCvObject(CvTopic.class, "MI:0004", "affinity chromatography technology");

        getCorePersister().saveOrUpdate(cv, parent);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0004"));
        context.setIdentifier("MI:0004");
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        cvUpdater.updateTerm(context);

        getDataContext().commitTransaction(status2);

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
        Assert.assertEquals(1, cvUpdater.getMissingParents().size());

        for (String t : cvUpdater.getMissingParents().keySet()){
            if (!"MI:0400".equals(t)){
                Assert.assertTrue(false);
            }
        }
    }
}
