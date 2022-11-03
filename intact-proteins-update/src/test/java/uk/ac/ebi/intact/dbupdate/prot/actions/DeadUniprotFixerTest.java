package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.IntactBasicTestCase;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.fixers.DeadUniprotProteinFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import static uk.ac.ebi.intact.util.protein.utils.TestsUtils.*;


/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>17-Nov-2010</pre>
 */
public class DeadUniprotFixerTest extends IntactBasicTestCase {

    private DeadUniprotProteinFixer deadUniprotFixer;
    @Before
    public void before() throws Exception {
        deadUniprotFixer = new DeadUniprotProteinFixer();
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        deadUniprotFixer = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Fix a protein dead in uniprot : change identity uniprot with 'uniprot-removed-ac'.
     * Add annotation 'no-uniprot-update' and a caution
     */
    public void fix_dead_protein_no_other_cross_references() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein deadProtein = getMockBuilder().createProtein("P12345", "dead");
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(deadProtein);

        // fix dead protein
        deadUniprotFixer.fixDeadProtein(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), deadProtein));

        Assert.assertTrue(hasXRef(deadProtein, "P12345", CvDatabase.UNIPROT, CvXrefQualifier.UNIPROT_REMOVED_AC));
        Assert.assertTrue(hasAnnotation(deadProtein, null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(deadProtein, null, CvTopic.CAUTION));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Fix a protein dead in uniprot : change identity uniprot with 'uniprot-removed-ac'.
     * Add annotation 'no-uniprot-update' and a caution
     *
     * Remove all other cross references of the protein
     */
    public void fix_dead_protein_with_other_cross_references() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();

        Protein deadProtein = getMockBuilder().createProtein("P12345", "dead");

        InteractorXref ref = getMockBuilder().createXref(deadProtein, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        deadProtein.addXref(ref);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(deadProtein);

        // fix dead protein
        deadUniprotFixer.fixDeadProtein(new ProteinEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), deadProtein));

        Assert.assertTrue(hasXRef(deadProtein, "P12345", CvDatabase.UNIPROT, CvXrefQualifier.UNIPROT_REMOVED_AC));
        Assert.assertTrue(hasAnnotation(deadProtein, null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(deadProtein, null, CvTopic.CAUTION));
        Assert.assertFalse(hasXRef(deadProtein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);
    }
}