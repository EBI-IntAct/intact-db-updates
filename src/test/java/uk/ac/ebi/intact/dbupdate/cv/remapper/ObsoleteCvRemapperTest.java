package uk.ac.ebi.intact.dbupdate.cv.remapper;

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
import uk.ac.ebi.intact.dbupdate.cv.updater.CvUpdaterTest;
import uk.ac.ebi.intact.model.*;

import java.io.File;
import java.io.IOException;
import java.sql.SQLException;
import java.util.Iterator;
import java.util.Map;
import java.util.Set;

/**
 * Cv remapper test
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml",
        "classpath*:/META-INF/beanscv*.spring.xml"})
public class ObsoleteCvRemapperTest extends IntactBasicTestCase {

    private CvUpdateManager cvManager;

    @Autowired
    private ObsoleteCvRemapper obsoleteRemapper;

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
    public void test_obsolete_no_obvious_remapping(){
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvInteraction.class, "MI:0021", "coloc fluoresc probe");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0021"));
        context.setIdentifier("MI:0021");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        Assert.assertEquals("MI:0021", term.getIdentifier());
        Assert.assertEquals(1, term.getXrefs().size());
        Assert.assertEquals("MI:0021", term.getXrefs().iterator().next().getPrimaryId());
        Assert.assertEquals("MI:0021", term.getXrefs().iterator().next().getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, term.getXrefs().iterator().next().getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, term.getXrefs().iterator().next().getCvXrefQualifier().getIdentifier());
        Assert.assertEquals(0, obsoleteRemapper.getRemappedCvToUpdate().size());
        
        Assert.assertTrue(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_non_existing_term_same_ontology(){
        String remappedId = "MI:0933";
        
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0802"));
        context.setTermObsolete(true);
        context.setIdentifier("MI:0802");
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        Assert.assertEquals(2, term.getXrefs().size());
        Iterator<CvObjectXref> refs = term.getXrefs().iterator();
        CvObjectXref secondary = refs.next();
        CvObjectXref psi = refs.next();

        Assert.assertEquals(remappedId, psi.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, psi.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, psi.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals("MI:0802", secondary.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, secondary.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, secondary.getCvXrefQualifier().getIdentifier());
        
        Assert.assertEquals("MI:0933", context.getIdentifier());
        Assert.assertEquals(access.getTermForAccession("MI:0933"), context.getOntologyTerm());
        Assert.assertEquals(psi, context.getIdentityXref());
        
        Assert.assertEquals("MI:0933", term.getIdentifier());

        Assert.assertEquals(0, obsoleteRemapper.getRemappedCvToUpdate().size());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_non_existing_term_different_ontology(){
        String remappedId = "MOD:00359";

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvFeatureType.class, "MI:0123", "n2-acetyl-arginine");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0123"));
        context.setIdentifier("MI:0123");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        Assert.assertEquals(2, term.getXrefs().size());
        Iterator<CvObjectXref> refs = term.getXrefs().iterator();
        CvObjectXref secondary = refs.next();
        CvObjectXref psi = refs.next();

        Assert.assertEquals(remappedId, psi.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MOD_MI_REF, psi.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, psi.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals("MI:0123", secondary.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, secondary.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, secondary.getCvXrefQualifier().getIdentifier());

        Assert.assertEquals("MOD:00359", context.getIdentifier());
        Assert.assertNull(context.getOntologyTerm());
        Assert.assertEquals(psi, context.getIdentityXref());

        Assert.assertEquals("MOD:00359", term.getIdentifier());

        Assert.assertEquals(1, obsoleteRemapper.getRemappedCvToUpdate().size());
        Map.Entry<String, Set<CvDagObject>> entry = obsoleteRemapper.getRemappedCvToUpdate().entrySet().iterator().next();

        Assert.assertEquals("MOD", entry.getKey());
        Assert.assertEquals(1, entry.getValue().size());
        Assert.assertEquals(term, entry.getValue().iterator().next());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_existing_term_same_ontology(){

        String remappedId = "MI:0933";

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject existingCv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0933", "negative gen int");
        CvDagObject cv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        getCorePersister().saveOrUpdate(cv, existingCv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0802"));
        context.setIdentifier("MI:0802");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();
        
        Assert.assertNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc()));
        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());

        Assert.assertEquals(3, term.getXrefs().size());
        Iterator<CvObjectXref> refs = term.getXrefs().iterator();
        CvObjectXref secondary = refs.next();
        CvObjectXref secondaryIntact = refs.next();
        CvObjectXref psi = refs.next();

        Assert.assertEquals(remappedId, psi.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, psi.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, psi.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals("MI:0802", secondary.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, secondary.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, secondary.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals(cv.getAc(), secondaryIntact.getPrimaryId());
        Assert.assertEquals(CvDatabase.INTACT_MI_REF, secondaryIntact.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, secondaryIntact.getCvXrefQualifier().getIdentifier());

        Assert.assertEquals("MI:0933", context.getIdentifier());
        Assert.assertEquals(access.getTermForAccession("MI:0933"), context.getOntologyTerm());
        Assert.assertEquals(psi, context.getIdentityXref());

        Assert.assertEquals("MI:0933", term.getIdentifier());

        Assert.assertEquals(0, obsoleteRemapper.getRemappedCvToUpdate().size());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_existing_term_different_ontology(){
        String remappedId = "MOD:00359";
        
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject existingCv = getMockBuilder().createCvObject(CvFeatureType.class, "MOD:00359", "n2-acetyl");
        CvObjectXref modXref = existingCv.getXrefs().iterator().next();
        modXref.setCvDatabase(getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.PSI_MOD_MI_REF, CvDatabase.PSI_MOD));

        CvDagObject cv = getMockBuilder().createCvObject(CvFeatureType.class, "MI:0123", "n2-acetyl-arginine");
        getCorePersister().saveOrUpdate(cv, existingCv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0123"));
        context.setIdentifier("MI:0123");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        Assert.assertNull(getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc()));
        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());

        Assert.assertEquals(3, term.getXrefs().size());
        Iterator<CvObjectXref> refs = term.getXrefs().iterator();
        CvObjectXref secondary = refs.next();
        CvObjectXref intactSecondary = refs.next();
        CvObjectXref psi = refs.next();

        Assert.assertEquals(remappedId, psi.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MOD_MI_REF, psi.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.IDENTITY_MI_REF, psi.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals("MI:0123", secondary.getPrimaryId());
        Assert.assertEquals(CvDatabase.PSI_MI_MI_REF, secondary.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, secondary.getCvXrefQualifier().getIdentifier());
        Assert.assertEquals(cv.getAc(), intactSecondary.getPrimaryId());
        Assert.assertEquals(CvDatabase.INTACT_MI_REF, intactSecondary.getCvDatabase().getIdentifier());
        Assert.assertEquals(CvXrefQualifier.SECONDARY_AC_MI_REF, intactSecondary.getCvXrefQualifier().getIdentifier());

        Assert.assertEquals("MOD:00359", context.getIdentifier());
        Assert.assertNull(context.getOntologyTerm());
        Assert.assertEquals(psi, context.getIdentityXref());

        Assert.assertEquals("MOD:00359", term.getIdentifier());

        Assert.assertEquals(1, obsoleteRemapper.getRemappedCvToUpdate().size());
        Map.Entry<String, Set<CvDagObject>> entry = obsoleteRemapper.getRemappedCvToUpdate().entrySet().iterator().next();

        Assert.assertEquals("MOD", entry.getKey());
        Assert.assertEquals(1, entry.getValue().size());
        Assert.assertEquals(term, entry.getValue().iterator().next());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_existing_term_existing_parent_child(){

        String parentId = "MI:0208";

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvInteractionType.class, parentId, "genetic interaction");
        CvDagObject child = getMockBuilder().createCvObject(CvInteractionType.class, "IA:xxxxx", "genetic interaction child");
        CvDagObject existingCv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0933", "negative gen int");
        existingCv.addChild(child);
        existingCv.addParent(parent);
        
        CvDagObject cv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        cv.addChild(child);
        cv.addParent(parent);

        getCorePersister().saveOrUpdate(cv, existingCv, parent, child);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0802"));
        context.setIdentifier("MI:0802");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());

        Assert.assertEquals(1, term.getParents().size());
        Assert.assertEquals(1, term.getChildren().size());

        CvDagObject parentDb = term.getParents().iterator().next();
        CvDagObject childDb = term.getChildren().iterator().next();
        Assert.assertEquals(parentDb.getAc(), term.getParents().iterator().next().getAc());
        Assert.assertEquals(childDb.getAc(), term.getChildren().iterator().next().getAc());
        Assert.assertEquals(1, parentDb.getChildren().size());
        Assert.assertEquals(1, childDb.getParents().size());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_existing_term_update_parent_child(){

        String parentId = "MI:0208";

        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject parent = getMockBuilder().createCvObject(CvInteractionType.class, parentId, "genetic interaction");
        CvDagObject child = getMockBuilder().createCvObject(CvInteractionType.class, "IA:xxxxx", "genetic interaction child");
        CvDagObject existingCv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0933", "negative gen int");
        existingCv.addChild(child);
        existingCv.addParent(parent);

        CvDagObject cv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");

        getCorePersister().saveOrUpdate(cv, existingCv, parent, child);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0802"));
        context.setIdentifier("MI:0802");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());

        Assert.assertEquals(1, term.getParents().size());
        Assert.assertEquals(1, term.getChildren().size());

        CvDagObject parentDb = term.getParents().iterator().next();
        CvDagObject childDb = term.getChildren().iterator().next();
        Assert.assertEquals(parentDb.getAc(), term.getParents().iterator().next().getAc());
        Assert.assertEquals(childDb.getAc(), term.getChildren().iterator().next().getAc());
        Assert.assertEquals(1, parentDb.getChildren().size());
        Assert.assertEquals(1, childDb.getParents().size());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_obsolete_remap_to_existing_term_remap_cvReferences(){

        TransactionStatus status = getDataContext().beginTransaction();

        CvInteractionType existingCv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0933", "negative gen int");
        CvInteractionType cv = getMockBuilder().createCvObject(CvInteractionType.class, "MI:0802", "enhancement interaction");
        
        Interaction interaction = getMockBuilder().createDeterministicInteraction();
        interaction.setCvInteractionType(cv);
        
        getCorePersister().saveOrUpdate(cv, existingCv, interaction);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvDagObject term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(cv.getAc());

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession("MI:0802"));
        context.setIdentifier("MI:0802");
        context.setTermObsolete(true);
        context.setCvTerm(term);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        obsoleteRemapper.remapObsoleteCvTerm(context);

        getDataContext().commitTransaction(status2);

        TransactionStatus status3 = getDataContext().beginTransaction();

        term = getDaoFactory().getCvObjectDao(CvDagObject.class).getByAc(existingCv.getAc());
        Interaction interDb = getDaoFactory().getInteractionDao().getByAc(interaction.getAc());

        Assert.assertEquals(term, interDb.getCvInteractionType());

        Assert.assertFalse(context.isTermObsolete());

        getDataContext().commitTransaction(status3);
    }
}
