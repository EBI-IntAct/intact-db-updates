package uk.ac.ebi.intact.dbupdate.prot;

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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.listener.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;

import java.util.Collection;
import java.util.Date;
import java.util.Set;

/**
 * Second Tester of ProteinProcessor
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )

public class ProteinProcessor2Test extends IntactBasicTestCase {

    ProteinProcessor processor;

    @Before
    public void before() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setUniprotService(new MockUniprotService());

        processor = new ProteinUpdateProcessor();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        processor = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One Intact protein is 'no-uniprot' and the other is from uniprot.
     * The protein from uniprot is updated and the protein 'no-uniptoy-update' should be ignored
     */
    public void update_protein_and_ignore_protein_no_uniprot() throws Exception{

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(false);
        config.setGlobalProteinUpdate(false);

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();

        Protein protein = getMockBuilder().createProtein("P60953", "protein");
        protein.getBioSource().setTaxId("9606");
        getCorePersister().saveOrUpdate(protein);

        Protein secondary = getMockBuilder().createProtein("P21181", "secondary");
        Annotation no_uniprot = getMockBuilder().createAnnotation(null, null, CvTopic.NON_UNIPROT);
        secondary.addAnnotation(no_uniprot);
        getCorePersister().saveOrUpdate(secondary);

        Protein random = getMockBuilder().createProteinRandom();
        getCorePersister().saveOrUpdate(random);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        getCorePersister().saveOrUpdate(interaction);
        Interaction interaction2 = getMockBuilder().createInteraction(secondary, random);
        getCorePersister().saveOrUpdate(interaction2);

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());

        Set<String> updatedProteins = processor.update(protein, context);

        Assert.assertEquals(4, updatedProteins.size());
        Assert.assertEquals(5, getDaoFactory().getProteinDao().countAll());
        Assert.assertTrue(hasAnnotation(secondary, null, CvTopic.NON_UNIPROT));

        // the uniprot protein
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertEquals(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertEquals(uniprot.getDescription(), protein.getFullName());
        Assert.assertEquals(uniprot.getSequence(), protein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        for (String secAc : uniprot.getSecondaryAcs()){
            Assert.assertTrue(hasXRef(protein, secAc, CvDatabase.UNIPROT, CvXrefQualifier.SECONDARY_AC));
        }

        for ( String geneName : uniprot.getGenes() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME, geneName));
        }

        for ( String syn : uniprot.getSynomyms() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.GENE_NAME_SYNONYM, syn));
        }

        for ( String orf : uniprot.getOrfs() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, orf));
        }

        for ( String locus : uniprot.getLocuses() ) {
            Assert.assertTrue(hasAlias(protein, CvAliasType.LOCUS_NAME, locus));
        }

        Assert.assertEquals(12, protein.getXrefs().size());

        // reset
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setGlobalProteinUpdate(false);

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

    private boolean hasAnnotation( Protein p, String text, String cvTopic) {
        final Collection<Annotation> annotations = p.getAnnotations();
        boolean hasAnnotation = false;

        for ( Annotation a : annotations ) {
            if (cvTopic.equalsIgnoreCase(a.getCvTopic().getShortLabel())){
                if (text == null){
                    hasAnnotation = true;
                }
                else if (text != null && a.getAnnotationText() != null){
                    if (text.equalsIgnoreCase(a.getAnnotationText())){
                        hasAnnotation = true;
                    }
                }
            }
        }

        return hasAnnotation;
    }
}
