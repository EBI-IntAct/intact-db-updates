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
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.IntactTranscriptParentUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.CvDatabase;
import uk.ac.ebi.intact.model.CvXrefQualifier;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;
import java.util.List;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>07-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class IntactParentUpdaterTest extends IntactBasicTestCase {

    private IntactTranscriptParentUpdaterImpl intactUpdater;
    @Before
    public void setUp(){
        intactUpdater = new IntactTranscriptParentUpdaterImpl();
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }
    @After
    public void after() throws Exception {
        intactUpdater = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void remap_parent_intact_secondary(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        String oldParent = "EBI-xxxx";

        CvDatabase intact = context.getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);
        CvXrefQualifier intactSecondary = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");
        CvXrefQualifier isoformParent = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        Protein parentToFind = getMockBuilder().createProtein("P12345", "protein parent");
        parentToFind.addXref(getMockBuilder().createXref(parentToFind, oldParent, intactSecondary, intact));

        getCorePersister().saveOrUpdate(parentToFind);

        Protein isoform = getMockBuilder().createProtein("P12345-1", "isoform");
        isoform.addXref(getMockBuilder().createXref(isoform, oldParent, isoformParent, intact));

        getCorePersister().saveOrUpdate(isoform);

        Assert.assertTrue(hasXRef(isoform, oldParent, CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertTrue(hasXRef(parentToFind, oldParent, CvDatabase.INTACT, "intact-secondary"));
        Assert.assertEquals(0, context.getDaoFactory().getProteinDao().getSpliceVariants(parentToFind).size());

        List<Protein> transcriptsToReview = new ArrayList<Protein>();
        intactUpdater.checkConsistencyProteinTranscript(new ProteinEvent(new ProteinUpdateProcessor(), context, isoform), transcriptsToReview);

        Assert.assertTrue(hasXRef(isoform, parentToFind.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertEquals(1, context.getDaoFactory().getProteinDao().getSpliceVariants(parentToFind).size());

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void remap_parent_invalid_parent(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        CvDatabase intact = context.getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);
        CvXrefQualifier intactSecondary = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");
        CvXrefQualifier isoformParent = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        Protein isoform = getMockBuilder().createProtein("P12345-1", "isoform");
        getCorePersister().saveOrUpdate(isoform);

        String oldParent = isoform.getAc();

        isoform.addXref(getMockBuilder().createXref(isoform, oldParent, isoformParent, intact));
        getCorePersister().saveOrUpdate(isoform);

        Assert.assertTrue(hasXRef(isoform, oldParent, CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));

        List<Protein> transcriptsToReview = new ArrayList<Protein>();
        boolean canUpdate = intactUpdater.checkConsistencyProteinTranscript(new ProteinEvent(new ProteinUpdateProcessor(), context, isoform), transcriptsToReview);

        Assert.assertTrue(canUpdate);
        Assert.assertEquals(1, transcriptsToReview.size());

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void remap_parent_intact_secondary_all_transcripts(){
        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        String oldParent = "EBI-xxxx";

        CvDatabase intact = context.getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.INTACT_MI_REF);
        CvXrefQualifier intactSecondary = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByShortLabel("intact-secondary");
        CvXrefQualifier isoformParent = context.getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        Protein parentToFind = getMockBuilder().createProtein("P12345", "protein parent");
        parentToFind.addXref(getMockBuilder().createXref(parentToFind, oldParent, intactSecondary, intact));

        getCorePersister().saveOrUpdate(parentToFind);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(parentToFind);

        Protein isoform = getMockBuilder().createProtein("P12345-1", "isoform");
        isoform.addXref(getMockBuilder().createXref(parentToFind, oldParent, isoformParent, intact));

        getCorePersister().saveOrUpdate(isoform);

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, null));

        Assert.assertTrue(hasXRef(isoform, oldParent, CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertTrue(hasXRef(parentToFind, oldParent, CvDatabase.INTACT, "intact-secondary"));
        Assert.assertEquals(0, context.getDaoFactory().getProteinDao().getSpliceVariants(parentToFind).size());

        intactUpdater.checkConsistencyOfAllTranscripts(new UpdateCaseEvent(new ProteinUpdateProcessor(), context, null, primaryProteins, Collections.EMPTY_LIST, primaryIsoforms, Collections.EMPTY_LIST, Collections.EMPTY_LIST, "P12345"));

        Assert.assertTrue(hasXRef(isoform, parentToFind.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertEquals(1, context.getDaoFactory().getProteinDao().getSpliceVariants(parentToFind).size());

        context.commitTransaction(status);
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
}
