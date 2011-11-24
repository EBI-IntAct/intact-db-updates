package uk.ac.ebi.intact.dbupdate.cv;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.tools.ontology_manager.impl.local.OntologyLoaderException;
import uk.ac.ebi.intact.bridges.ontology_manager.interfaces.IntactOntologyAccess;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;

import java.io.IOException;

/**
 * Unit tester of the CvUpdater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>16/11/11</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/intact.spring.xml",
        "classpath*:/META-INF/standalone/*-standalone.spring.xml"})
public class CvUpdaterTest extends IntactBasicTestCase{

    private CvUpdateManager cvManager;

    @Before
    public void clear() throws IOException, OntologyLoaderException {
        cvManager = new CvUpdateManager(CvUpdaterTest.class.getResource("/ontologies.xml"), "targets/reports");
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void test_update_wrongIdentifier_xrefsAdded_annotationsAdded_parentsMissing(){
        TransactionStatus status = getDataContext().beginTransaction();

        CvDagObject cv = getMockBuilder().createCvObject(CvTopic.class, CvTopic.COMMENT_MI_REF, CvTopic.COMMENT);
        getCorePersister().saveOrUpdate(cv);

        cv.setIdentifier("IA:xxxxx");
        getCorePersister().saveOrUpdate(cv);

        getDataContext().commitTransaction(status);

        IntactOntologyAccess access = cvManager.getIntactOntologyManager().getOntologyAccess("MI");

        TransactionStatus status2 = getDataContext().beginTransaction();

        CvUpdateContext context = new CvUpdateContext(this.cvManager);
        context.setOntologyAccess(access);
        context.setOntologyTerm(access.getTermForAccession(CvTopic.COMMENT_MI_REF));
        context.setIdentifier(CvTopic.COMMENT_MI_REF);
        context.setCvTerm(cv);
        context.setIdentityXref(cv.getXrefs().iterator().next());

        cvManager.getCvUpdater().updateTerm(context);

        getDataContext().commitTransaction(status2);

        Assert.assertEquals(CvTopic.COMMENT_MI_REF, cv.getIdentifier());
        Assert.assertEquals(2, cv.getXrefs().size());

        for (CvObjectXref ref : cv.getXrefs()){
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

        Assert.assertEquals(1, cv.getAnnotations().size());
        Annotation def = cv.getAnnotations().iterator().next();
        Assert.assertEquals(def.getCvTopic().getShortLabel(), CvTopic.DEFINITION);
        Assert.assertEquals(def.getAnnotationText(), "Comment for public view. This attribute can be associated to interaction, experiment, CV term, an organism and any participant.");

        Assert.assertTrue(cv.getAliases().isEmpty());

        Assert.assertEquals(6, cvManager.getCvUpdater().getMissingParents().size());

        for (String term : cvManager.getCvUpdater().getMissingParents().keySet()){
             if (!"MI:0664".equals(term) && !"MI:0665".equals(term) && !"MI:0666".equals(term) && !"MI:0667".equals(term)
                     && !"MI:0668".equals(term) && !"MI:0669".equals(term)){
                  Assert.assertTrue(false);
             }
        }
    }

}
