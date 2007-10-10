package uk.ac.ebi.intact.util.protein;

import org.junit.After;
import static org.junit.Assert.*;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;
import uk.ac.ebi.intact.business.IntactTransactionException;
import uk.ac.ebi.intact.config.CvPrimer;
import uk.ac.ebi.intact.config.impl.SmallCvPrimer;
import uk.ac.ebi.intact.context.CvContext;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.core.persister.PersisterHelper;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.core.util.SchemaUtils;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.persistence.dao.*;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.mock.FlexibleMockUniprotService;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;
import uk.ac.ebi.intact.util.protein.mock.UniprotProteinXrefBuilder;
import uk.ac.ebi.intact.util.protein.utils.ProteinToDeleteManager;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.*;

/**
 * ProteinServiceImpl Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since TODO artifact version
 */
public class ProteinServiceImplTest extends IntactBasicTestCase {

    @Before
    public void before() throws Exception {
        SchemaUtils.resetSchema();

        beginTransaction();
        CvPrimer cvPrimer = new ComprehensiveCvPrimer(getDaoFactory());
        cvPrimer.createCVs();
        commitTransaction();

    }

    @After
    public void after() throws Exception {
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
        IntactContext.getCurrentInstance().close();
    }

    private class ComprehensiveCvPrimer extends SmallCvPrimer {

        public ComprehensiveCvPrimer(DaoFactory daoFactory) {
            super(daoFactory);
        }

        @Override
        public void createCVs() {
            super.createCVs();

            getCvObject(CvInteractorType.class, CvInteractorType.PROTEIN, CvInteractorType.PROTEIN_MI_REF);
            getCvObject(CvInteractorType.class, CvInteractorType.DNA, CvInteractorType.DNA_MI_REF);
            getCvObject(CvDatabase.class, CvDatabase.UNIPROT, CvDatabase.UNIPROT_MI_REF);
            getCvObject(CvDatabase.class, CvDatabase.INTERPRO, CvDatabase.INTERPRO_MI_REF);
            getCvObject(CvXrefQualifier.class, CvXrefQualifier.SECONDARY_AC, CvXrefQualifier.SECONDARY_AC_MI_REF);
            getCvObject(CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT, CvXrefQualifier.ISOFORM_PARENT_MI_REF);
            getCvObject(CvAliasType.class, CvAliasType.GENE_NAME, CvAliasType.GENE_NAME_MI_REF);
            getCvObject(CvAliasType.class, CvAliasType.GENE_NAME_SYNONYM, CvAliasType.GENE_NAME_SYNONYM_MI_REF);
            getCvObject(CvAliasType.class, CvAliasType.ISOFORM_SYNONYM, CvAliasType.ISOFORM_SYNONYM_MI_REF);
            getCvObject(CvAliasType.class, CvAliasType.LOCUS_NAME, CvAliasType.LOCUS_NAME_MI_REF);
            getCvObject(CvAliasType.class, CvAliasType.ORF_NAME, CvAliasType.ORF_NAME_MI_REF);
            getCvObject(CvInteractionType.class, CvInteractionType.DIRECT_INTERACTION, CvInteractionType.DIRECT_INTERACTION_MI_REF);
            getCvObject(CvExperimentalRole.class, CvExperimentalRole.ANCILLARY, CvExperimentalRole.ANCILLARY_MI_REF);
            getCvObject(CvBiologicalRole.class, CvBiologicalRole.COFACTOR, CvBiologicalRole.COFACTOR_MI_REF);
            
            getCvObject(CvTopic.class, CvTopic.ISOFORM_COMMENT);
        }
    }

    //////////////////////
    // Helper methods

    private ProteinService buildProteinService() {
        UniprotService uniprotService = new MockUniprotService();
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        return service;
    }

    private ProteinService buildProteinService( UniprotService uniprotService ) {
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        return service;
    }

    private Protein searchByShortlabel( List<ProteinImpl> proteins, String shortlabel ) {
        for ( ProteinImpl protein : proteins ) {
            if ( protein.getShortLabel().equals( shortlabel ) ) {
                return protein;
            }
        }
        return null;
    }

    private void clearProteinsFromDatabase() throws IntactTransactionException {

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        // delete all interactors
        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        InteractorDao idao = daoFactory.getInteractorDao();
        List all = idao.getAll();
        System.out.println( "Searching for objects to delete, found " + all.size() + " interactor(s)." );

        if ( !all.isEmpty() ) {
            System.out.println( "Now deleting them all..." );
            idao.deleteAll( all );

            IntactContext.getCurrentInstance().getDataContext().commitTransaction();

            // check that interactor count is 0
            IntactContext.getCurrentInstance().getDataContext().beginTransaction();

            daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
            idao = daoFactory.getInteractorDao();

            List list = idao.getAll();
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
            assertNotNull( list );
            assertTrue( list.isEmpty() );
            list = null;
        } else {
            IntactContext.getCurrentInstance().getDataContext().commitTransaction();
            System.out.println( "Database was already cleared of any interactor." );
        }
    }

    ////////////////////
    // Tests

    @Test
    public void testRetrieve_CDC42_CANFA() throws Exception {


        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinService service = buildProteinService();
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        
        // Make sure that the uniprotService still contains the protein and it's 2 splice variants, as we got a bug
        // due to a method that was deleting the splice variants (findMatches method in ProteinServiceImpl class).
        UniprotService uniprotService = service.getUniprotService();
        Collection<UniprotProtein>uniprotProteins = uniprotService.retrieve(MockUniprotProtein.CANFA_PRIMARY_AC);
        for(UniprotProtein prot : uniprotProteins){
            System.out.println("Uniprot prot.getPrimaryAc() = " + prot.getPrimaryAc());
            Collection<UniprotSpliceVariant> svs = prot.getSpliceVariants();
            assertEquals(2, svs.size());
        }
        assertEquals( 3, proteins.size() );
        //Check that proteins contains, P60952 and its 2 splice variants.
        boolean P60952found = false;
        boolean P60952_1found = false;
        boolean P60952_2found = false;
        for(Protein protein : proteins){
            System.out.println("protein.getAc() = " + protein.getAc());
            InteractorXref uniprotIdentity = ProteinUtils.getUniprotXref(protein);
            System.out.println("uniprotIdentity.getPrimaryId() = " + uniprotIdentity.getPrimaryId());
            if("P60952".equals(uniprotIdentity.getPrimaryId())){
                P60952found = true;
            } else if("P60952-1".equals(uniprotIdentity.getPrimaryId())){
                P60952_1found = true;
            } else if("P60952-2".equals(uniprotIdentity.getPrimaryId())){
                P60952_2found = true;
            }
        }
        assertTrue(P60952found);
        assertTrue(P60952_1found);
        assertTrue(P60952_2found);

        // The retrieve method return a collection of proteins containing the master protein and it's splice variant if
        // any. We want to be sure to get the master protein.
        Protein protein = getProteinForPrimaryAc(proteins, MockUniprotProtein.CANFA_PRIMARY_AC);//proteins.iterator().next();

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();
        Institution owner = IntactContext.getCurrentInstance().getConfig().getInstitution();
        CvAliasType isoformSynonym = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.ISOFORM_SYNONYM_MI_REF );
        CvAliasType gene = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.GENE_NAME_MI_REF );
        CvXrefQualifier identity = ( CvXrefQualifier ) cvDao.getByPsiMiRef( CvXrefQualifier.IDENTITY_MI_REF );
        CvXrefQualifier secondaryAc = ( CvXrefQualifier ) cvDao.getByPsiMiRef( CvXrefQualifier.SECONDARY_AC_MI_REF );
        CvXrefQualifier isoformParent = ( CvXrefQualifier ) cvDao.getByPsiMiRef( CvXrefQualifier.ISOFORM_PARENT_MI_REF );
        CvDatabase uniprot = ( CvDatabase ) cvDao.getByPsiMiRef( CvDatabase.UNIPROT_MI_REF );
        CvDatabase interpro = ( CvDatabase ) cvDao.getByPsiMiRef( CvDatabase.INTERPRO_MI_REF );
        CvDatabase intact = ( CvDatabase ) cvDao.getByPsiMiRef( CvDatabase.INTACT_MI_REF );
        CvTopic isoformComment = ( CvTopic ) cvDao.getByShortLabel( CvTopic.ISOFORM_COMMENT );

        assertNotNull( protein.getAc() );
        assertEquals( "cdc42_canfa", protein.getShortLabel() );
        assertEquals( "Cell division control protein 42 homolog precursor (G25K GTP-binding protein)", protein.getFullName() );
        assertEquals( "34B44F9225EC106B", protein.getCrc64() );
        assertEquals( "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                "ETQPKRKCCIF", protein.getSequence() );
        assertNotNull( protein.getBioSource() );
        assertNotNull( protein.getBioSource().getAc() );
        assertEquals( "9615", protein.getBioSource().getTaxId() );
        assertEquals( 7, protein.getXrefs().size() );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, uniprot, "P60952", identity ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, uniprot, "P21181", secondaryAc ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, uniprot, "P25763", secondaryAc ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR003578", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR013753", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR001806", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR005225", null ) ) );
        assertEquals( 1, protein.getAliases().size() );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, gene, "CDC42" ) ) );
        assertEquals( 0, protein.getAnnotations().size() );

        //////////////////////
        // Splice variants

        ProteinDao pdao = daoFactory.getProteinDao();
        List<ProteinImpl> variants = pdao.getSpliceVariants( protein );
        assertEquals( 2, variants.size() );

        Protein sv1 = searchByShortlabel( variants, "P60952-1" );
        assertNotNull( sv1 );
        assertEquals( "Cell division control protein 42 homolog precursor (G25K GTP-binding protein)", sv1.getFullName() );
        assertTrue( sv1.getXrefs().contains( new InteractorXref( owner, intact, protein.getAc(), isoformParent ) ) );
        assertTrue( sv1.getXrefs().contains( new InteractorXref( owner, uniprot, "P60952-1", identity ) ) );
        assertTrue( sv1.getXrefs().contains( new InteractorXref( owner, uniprot, "P21181-1", secondaryAc ) ) );
        assertEquals( "34B44F9225EC106B", sv1.getCrc64() );
        assertEquals( "MQTIKCVVVGDGAVGKTCLLISYTTNKFPSEYVPTVFDNYAVTVMIGGEPYTLGLFDTAG" +
                "QEDYDRLRPLSYPQTDVFLVCFSVVSPSSFENVKEKWVPEITHHCPKTPFLLVGTQIDLR" +
                "DDPSTIEKLAKNKQKPITPETAEKLARDLKAVKYVECSALTQRGLKNVFDEAILAALEPP" +
                "ETQPKRKCCIF", sv1.getSequence() );
        assertTrue( sv1.getAliases().contains( new InteractorAlias( owner, sv1, isoformSynonym, "Brain" ) ) );
        assertTrue( sv1.getAnnotations().contains(
                new Annotation( owner, isoformComment, "Has not been isolated in dog so far" ) ) );

        Protein sv2 = searchByShortlabel( variants, "P60952-2" );
        assertNotNull( sv2 );
        assertEquals( "Cell division control protein 42 homolog precursor (G25K GTP-binding protein)", sv2.getFullName() );
        assertTrue( sv2.getXrefs().contains( new InteractorXref( owner, intact, protein.getAc(), isoformParent ) ) );
        assertTrue( sv2.getXrefs().contains( new InteractorXref( owner, uniprot, "P60952-2", identity ) ) );
        assertTrue( sv2.getXrefs().contains( new InteractorXref( owner, uniprot, "P21181-4", secondaryAc ) ) );
        assertEquals( Crc64.getCrc64( "MQTIKCVKRKCCIF" ), sv2.getCrc64() );
        assertEquals( "MQTIKCVKRKCCIF", sv2.getSequence() );
        assertTrue( sv2.getAliases().contains( new InteractorAlias( owner, sv2, isoformSynonym, "Placental" ) ) );

        // check that a second call to the service bring the same protein
        String ac = protein.getAc();
        String sv1ac = sv1.getAc();
        String sv2ac = sv2.getAc();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult =  service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */
        proteins = uniprotServiceResult.getProteins();

        assertNotNull(service.retrieve("P60952-2"));
        pdao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        Protein sv = pdao.getByAc(sv1ac);
        InteractorXref svXref = ProteinUtils.getUniprotXref(sv);
        assertEquals(svXref.getPrimaryId(), sv1.getShortLabel());

        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );

        protein = getProteinForPrimaryAc(proteins, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals( ac, protein.getAc() );

        variants = pdao.getSpliceVariants( protein );
        assertEquals( 2, variants.size() );

        sv1 = searchByShortlabel( variants, "P60952-1" );
        assertEquals( sv1ac, sv1.getAc() );

        sv2 = searchByShortlabel( variants, "P60952-2" );
        assertEquals( sv2ac, sv2.getAc() );
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

    }

    private Protein getProteinForPrimaryAc(Collection<Protein> proteins, String primaryAc) throws ProteinServiceException {
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        Protein proteinToReturn = null;
        for(Protein protein : proteins){
            for(InteractorXref xref : protein.getXrefs()){
                if(uniprot.equals(xref.getCvDatabase())
                        && identity.equals(xref.getCvXrefQualifier())
                        && primaryAc.equals(xref.getPrimaryId())){
                    if(proteinToReturn == null){
                        proteinToReturn = protein;
                    }else{
                        throw new ProteinServiceException("2 proteins with the same identity");
                    }
                }
            }
        }
        return proteinToReturn;
    }

    @Test
    public void testRetrieve_spliceVariant() throws Exception {
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService service = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        service.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        service.add( "P60952-2", canfa );
        ProteinService proteinService = buildProteinService( service );
        UniprotServiceResult uniprotServiceResult = proteinService.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> prots = proteinDao.getByXrefLike(uniprot, identity, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(1,prots.size());
        ProteinImpl parentProtein = prots.get(0);
        String parentProteinAc = parentProtein.getAc();
        prots = proteinDao.getByXrefLike(uniprot, identity, "P60952-2");
        assertEquals(1, prots.size());
        ProteinImpl spliceVariant = prots.get(0);
        String spliceVariantAc = spliceVariant.getAc();
        String spliceVariantShortlabel = spliceVariant.getShortLabel();
        spliceVariant.setShortLabel("SAPERLIPOPETE");
        proteinDao.saveOrUpdate(spliceVariant);
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = uniprotServiceResult = proteinService.retrieve( "P60952-2" );
        Collection<Protein> resultProteins = uniprotServiceResult.getProteins();
        assertEquals(3, resultProteins.size());
        boolean found = false;
        for(Protein prot : resultProteins){
            if (parentProteinAc.equals(prot.getAc())){
                found = true;
            }
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        prots = proteinDao.getByXrefLike(uniprot, identity, "P60952-2");
        assertEquals(1, prots.size());
        spliceVariant = prots.get(0);
        assertEquals(spliceVariantAc, spliceVariant.getAc());
        assertEquals(spliceVariantShortlabel, spliceVariant.getShortLabel());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testRetrieve_update_CDC42_CANFA() throws Exception {
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService service = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        service.add( "P60952", canfa );

        ProteinService proteinService = buildProteinService( service );
        UniprotServiceResult uniprotServiceResult = proteinService.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );

        // update shortlabel
        canfa.setId( "FOO_BAR" );
        canfa.setDescription( "LALALA" );
        canfa.setSequence( "LLLLLLLLLLLLL" );
        canfa.setCrc64( "XXXXXXXXXXXXX" );

        // provoking recycling and deletion of Xrefs
        canfa.getCrossReferences().addAll( new UniprotProteinXrefBuilder()
                .add( "IPR00000", "InterPro", "he he" )
                .build() );

        canfa.getCrossReferences().removeAll( new UniprotProteinXrefBuilder()
                .add( "IPR003578", "InterPro", "GTPase_Rho" )
                .add( "IPR005225", "InterPro", "Small_GTP_bd" )
                .build() );

        canfa.getSynomyms().add( "s" );
        canfa.getOrfs().add( "o" );
        canfa.getLocuses().add( "l" );
        canfa.getGenes().add( "foo" );
        canfa.getGenes().remove( "CDC42" );

        uniprotServiceResult = proteinService.retrieve( "P60952" );
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        for(String errorType : keySet){
            String error = errors.get(errorType);
            System.out.println("error message is : " + error);
        }
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = proteins.iterator().next();

        assertEquals( "foo_bar", protein.getShortLabel() );
        assertEquals( "LALALA", protein.getFullName() );
        assertEquals( "LLLLLLLLLLLLL", protein.getSequence() );
        assertEquals( "XXXXXXXXXXXXX", protein.getCrc64() );

        DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();

        Institution owner = IntactContext.getCurrentInstance().getConfig().getInstitution();
        CvDatabase interpro = ( CvDatabase ) cvDao.getByPsiMiRef( CvDatabase.INTERPRO_MI_REF );
        CvAliasType gene = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.GENE_NAME_MI_REF );
        CvAliasType synonym = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.GENE_NAME_SYNONYM_MI_REF );
        CvAliasType orf = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.ORF_NAME_MI_REF );
        CvAliasType locus = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.LOCUS_NAME_MI_REF );

        assertEquals( 6, protein.getXrefs().size() );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR00000", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR013753", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR001806", null ) ) );

        assertEquals( 4, protein.getAliases().size() );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, gene, "foo" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, synonym, "s" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, orf, "o" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, locus, "l" ) ) );


        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testRetrieve_sequenceUpdate() throws ProteinServiceException, IntactTransactionException {

        // clear database content.
        clearProteinsFromDatabase();

        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( "P60952", canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        UniprotServiceResult uniprotServiceResult =  service.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = proteins.iterator().next();
        String proteinSeq = protein.getSequence();
        String proteinCrc = protein.getCrc64();

        // Update the seqence/CRC of the protein
        assertTrue( proteinSeq.length() > 20 );
        String newSequence = proteinSeq.substring( 2, 20 );
        canfa.setSequence( newSequence );
        canfa.setSequenceLength( canfa.getSequence().length() );
        canfa.setCrc64( Crc64.getCrc64( canfa.getSequence() ) );

        protein = null;

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        uniprotServiceResult = service.retrieve( "P60952" );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        protein = proteins.iterator().next();

        // check that we have retrieved the exact same protein.
        assertEquals( newSequence, protein.getSequence() );
        assertEquals( Crc64.getCrc64( newSequence ), protein.getCrc64() );

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testRetrieve_intact1_uniprot0() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        assertEquals(3,uniprotServiceResult.getProteins().size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        // Re-initialize the uniprot service so that it does not contain any more the CANFA entry.
        uniprotService = new FlexibleMockUniprotService();
        canfa = MockUniprotProtein.build_CDC42_HUMAN();
        uniprotService.add( MockUniprotProtein.CDC42_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_2, canfa );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_3, canfa );

        service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );


        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());
        for(String errorType : keySet){
            String error = errors.get(errorType);
            assertEquals("Couldn't update protein with uniprot id = " + uniprotServiceResult.getQuerySentToService() + ". It was found" +
                    " in IntAct but was not found in Uniprot.", error);
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


    }

    public void testRetrieve_intact0_uniprot0() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CDC42_PRIMARY_AC );
        assertEquals(0, uniprotServiceResult.getProteins().size());
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());
        for(String errorType : keySet){
            String error = errors.get(errorType);
            assertEquals("Could not udpate protein with uniprot id = " + uniprotServiceResult.getQuerySentToService() + ". No " +
                    "corresponding entry found in uniprot.",error);
        }

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

    }

    /**
     * Test that the protein xref and the protein are udpated when : countPrimary == 0 && countSecondary == 1
     */
    @Test
    public void testRetrieve_primaryCount0_secondaryCount1() throws Exception{

        // clear database content.
        clearProteinsFromDatabase();

        /*----------------------------------------------------------
        Create in the db, the CANFA protein with primary Ac P60952
         -----------------------------------------------------------*/

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        //Do some settings
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertEquals( 3,proteins.size() );

        String proteinAc = "";
        for(Protein protein : proteins){
            InteractorXref uniprotIdentity = ProteinUtils.getUniprotXref(protein);
            if(MockUniprotProtein.CANFA_PRIMARY_AC.equals(uniprotIdentity.getPrimaryId())){
                proteinAc = protein.getAc();
            }
        }

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        /*--------------------------------------------------------------------------------------------------------------
        In the db, modify the sequence of P60952, and set it's xref identity to uniprot to P21181 which -in uniprot-
        is a secondary ac of P60952
         -------------------------------------------------------------------------------------------------------------*/
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        //Make sure that no protein in the database have an xref identity to uniprot with primaryAc = P21181
        List<ProteinImpl> P21181 = proteinDao.getByXrefLike(uniprot,identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(0,P21181.size());
        //Get the protein we created earlier, with xref identity to uniprot and primary Ac P60952
        ProteinImpl P60952 = proteinDao.getByXref(MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(proteinAc, P60952.getAc());
        //Change it's identity xref to P21181 which is the secondary ac of the CANFA protein in Uniprot
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(P60952);
        assertNotNull(uniprotXref);
        uniprotXref.setPrimaryId(MockUniprotProtein.CANFA_SECONDARY_AC_1);
        //Change the sequence
        P60952.setSequence("TRALALA");
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        //Retrieve (= update P60952)
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull(proteins);
        assertEquals(3, proteins.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        // As the protein has been update, we shouldn't get anything searching by xref identity to uniprot with P22181.
        // We make sure that this is the case.
        P21181 = proteinDao.getByXrefLike(uniprot,identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(0,P21181.size());
        // Make sure that there is in the db one prot corresponding to P60952
        List protP60952 = proteinDao.getByXrefLike( uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC );
        assertNotNull(protP60952);
        assertEquals(1, protP60952.size());
        assertTrue(protP60952.iterator().hasNext());
        P60952 = (ProteinImpl) protP60952.iterator().next();
        assertEquals(proteinAc, P60952.getAc());
        uniprotXref = ProteinUtils.getUniprotXref(P60952);
        assertNotNull(uniprotXref);
        assertEquals(uniprotXref.getPrimaryId(), MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(P60952.getSequence(),MockUniprotProtein.CANFA_SEQUENCE);
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

    }

    /**
     * Check that nothing is update if more then 1 proteins are found in uniprot.
     */
    @Test
    public void testRetrieve_uniprotAcReturningMoreThen1EntryWithDifferentSpecies() throws Exception{

        // clear database content.
        clearProteinsFromDatabase();

        /*----------------------------------------------------------
       Create in the db, the CANFA protein with primary Ac P60952
        -----------------------------------------------------------*/

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        //Do some settings
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );
        UniprotProtein human = MockUniprotProtein.build_CDC42_HUMAN();
        uniprotService.add( MockUniprotProtein.CDC42_PRIMARY_AC, human );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_1, human );
        Collection<UniprotProtein> uniprotProteins = new ArrayList(2);
        uniprotProteins.add(human);
        uniprotProteins.add(canfa);
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_2, uniprotProteins );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_3, human );


        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CDC42_SECONDARY_AC_2 );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertEquals( 0,proteins.size() );
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());
        for(String errorType : keySet){
            String error = errors.get(errorType);
            assertTrue(("Trying to update "+ uniprotServiceResult.getQuerySentToService() +" returned a set of proteins belonging to different organisms.").equals(error));
        }

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        Collection<ProteinImpl> intactProteins = proteinDao.getByUniprotId(canfa.getPrimaryAc());
        assertEquals(0, intactProteins.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

    }

    /**
     * Check that nothing is update if more then 1 proteins are found in uniprot.
     */
    @Test
    public void testRetrieve_uniprotAcReturningMoreThen1EntryWithSameSpecies() throws Exception{

        // clear database content.
        clearProteinsFromDatabase();

        /*----------------------------------------------------------
       Create in the db, the CANFA protein with primary Ac P60952
        -----------------------------------------------------------*/

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        //Do some settings
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );
        UniprotProtein human = MockUniprotProtein.build_CDC42_HUMAN();
        uniprotService.add( MockUniprotProtein.CDC42_PRIMARY_AC, human );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_1, human );
        canfa.setOrganism(human.getOrganism());
        Collection<UniprotProtein> uniprotProteins = new ArrayList(2);
        uniprotProteins.add(human);
        uniprotProteins.add(canfa);
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_2, uniprotProteins );
        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_3, human );


        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CDC42_SECONDARY_AC_2 );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertEquals( 0,proteins.size() );
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());
        for(String errorType : keySet){
            String error = errors.get(errorType);
            assertTrue(("Trying to update "+ uniprotServiceResult.getQuerySentToService() +" returned a set of proteins belonging to the same organism.").equals(error));
        }
    }

//    private Protein getProtein(String uniprotId, ProteinDao proteinDao){
//        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
//        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
//                // As the protein has been update, we shouldn't get anything searching by xref identity to uniprot with P22181.
//                // We make sure that this is the case.
//        List proteins = proteinDao.getByXrefLike(uniprot,identity,uniprotId);
//        if(proteins.size() == 0){
//            return null;
//        }else if(proteins.size()>1){
//
//        }
//    }

    @Test
    public void testRetrieve_primaryCount0_secondaryCount2() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins();
        assertEquals( 3,proteinsColl.size() );
        String proteinAc = "";
        for(Protein protein : proteinsColl){
            proteinAc = protein.getAc();
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(1, proteinsList.size());
        ProteinImpl protein = proteinsList.get(0);
        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protein);
        uniprotXref.setPrimaryId(MockUniprotProtein.CANFA_SECONDARY_AC_1);
        protein.setSequence("BLABLA");
        proteinDao.saveOrUpdate(protein);
        uniprotXref = ProteinUtils.getUniprotXref(protein);
        System.out.println("uniprotXref.getPrimaryId() = " + uniprotXref.getPrimaryId());
        String ac = protein.getAc();
        Protein duplicatedProtein = new ProteinImpl(IntactContext.getCurrentInstance().getInstitution(),
                protein.getBioSource(),
                protein.getShortLabel(),
                protein.getCvInteractorType());
        proteinDao.saveOrUpdate((ProteinImpl) duplicatedProtein);
        duplicatedProtein.setSequence("BLABLA");
        InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(),
                uniprot,
                MockUniprotProtein.CANFA_SECONDARY_AC_1,
                identity);
        newXref.setParent(duplicatedProtein);
        XrefDao xrefDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        duplicatedProtein.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) duplicatedProtein);
        System.out.println("ac = " + ac);
        System.out.println("duplicatedProtein.getAc() = " + duplicatedProtein.getAc());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(2, proteinsList.size());
        proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(0, proteinsList.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(0,proteinsColl.size());
        System.out.println("proteinsColl.size() = " + proteinsColl.size());
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());
        for(String errorType : keySet){
            String error = errors.get(errorType);
            assertTrue(error.contains("More than one IntAct protein is matching secondary AC(s):"));
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testRetrieve_primaryCount1_secondaryCount1() throws Exception{

        // clear database content.
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        clearProteinsFromDatabase();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        if(IntactContext.getCurrentInstance().getCvContext().getByLabel(CvTopic.class, "to-delete") == null) {
            CvTopic toDelete = new CvTopic(IntactContext.getCurrentInstance().getInstitution(), "to-delete");
            CvObjectDao cvObjectDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao(CvTopic.class);
            cvObjectDao.saveOrUpdate(toDelete);
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins() ;
        assertEquals( 3,proteinsColl.size() );
        Protein masterProtein = getProteinForPrimaryAc(proteinsColl, MockUniprotProtein.CANFA_PRIMARY_AC);
        String proteinAc = masterProtein.getAc();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        Collection<Protein> proteins = new ArrayList<Protein>();
        for (int i = 0; i < proteinsList.size(); i++) {
            ProteinImpl protein =  proteinsList.get(i);
            proteins.add(protein);
        }
        assertEquals(1, proteinsList.size());
        // proteincColl and proteins contain the updated master protein and it's splice variant, we want to get the master
        // protein.
        Protein protein = getProteinForPrimaryAc(proteins, MockUniprotProtein.CANFA_PRIMARY_AC);
        String primaryProteinAc = protein.getAc();

        Protein secondaryProt = new ProteinImpl(IntactContext.getCurrentInstance().getInstitution(),
                protein.getBioSource(),
                protein.getShortLabel(),
                protein.getCvInteractorType());
        proteinDao.saveOrUpdate((ProteinImpl) secondaryProt);
        secondaryProt.setSequence("BLABLA");
        InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(),
                uniprot,
                MockUniprotProtein.CANFA_SECONDARY_AC_1,
                identity);
        newXref.setParent(secondaryProt);
        XrefDao xrefDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        secondaryProt.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) secondaryProt);
        String secondaryProtAc = secondaryProt.getAc();
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<String> messages = uniprotServiceResult.getMessages();
        assertEquals(1,messages.size());
        for(String message : messages){
            assertTrue(message.contains("The protein which are going to be merged :"));
        }
        Collection<String> acsOfProtToDelete = ProteinToDeleteManager.getAcToDelete();
        assertEquals(1, acsOfProtToDelete.size());
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        for(String ac : acsOfProtToDelete){
            ProteinImpl protToDel = proteinDao.getByAc(ac);
            proteinDao.delete(protToDel);
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(3,proteinsColl.size());
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();

        ProteinImpl proteinPrimaryAc = proteinDao.getByAc(primaryProteinAc);
        ProteinImpl proteinSecondaryAc = proteinDao.getByAc(secondaryProtAc);
        if((proteinPrimaryAc==null && proteinSecondaryAc==null)){
            fail("one of them shouldn't be null, because they should have been merged into one protein");
        }
        if((proteinPrimaryAc!=null && proteinSecondaryAc!=null)){
            fail("one of them should be null, they should have been " +
                    "merged into one but not both should have been deleted.");
        }
        assertTrue((proteinPrimaryAc==null && proteinSecondaryAc!=null) || (proteinPrimaryAc!=null && proteinSecondaryAc==null));

        System.out.println("proteinsColl.size() = " + proteinsColl.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testRetrieve_throwException() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );
        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        try{
            UniprotServiceResult uniprotServiceResult = service.retrieve((String) null);
            fail("Should have thrown a NullPointerException");
        }catch(IllegalArgumentException e){

        }
        try{
            UniprotServiceResult uniprotServiceResult = service.retrieve("");
            fail("Should have thrown an IllegalArgumentException");
        }catch(IllegalArgumentException e){

        }
    }


//    public void testRetrieve_withSecondaryAcSharedBy2UniprotEntry() throws Exception{
//        // clear database content.
//        clearProteinsFromDatabase();
//
//        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
//        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
//        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
//        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
//        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
//        UniprotProtein cdc42 = MockUniprotProtein.build_CDC42_HUMAN();
//        uniprotService.add( MockUniprotProtein.CDC42_PRIMARY_AC, cdc42 );
//        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_1, cdc42 );
//        uniprotService.add( MockUniprotProtein.CDC42_SECONDARY_AC_3, cdc42 );
//        Collection<UniprotProtein> proteins = new ArrayList<UniprotProtein>();
//        proteins.add(cdc42);
//        proteins.add(canfa);
//        uniprotService.add(MockUniprotProtein.CDC42_SECONDARY_AC_2, proteins);
//
//        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
//        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
//
//        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CDC42_PRIMARY_AC );
//        /*UniprotServiceResult*/ uniprotServiceResult = service.retrieve( MockUniprotProtein.CDC42_SECONDARY_AC_2 );
//        assertEquals(2, uniprotServiceResult.getProteins().size());
//        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
//
//
//    }

    @Test
    public void testRetrieve_primaryCount2_secondaryCount1() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins();
        assertEquals( 3,proteinsColl.size() );
        String proteinAc = "";
        for(Protein protein : proteinsColl){
            proteinAc = protein.getAc();
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(1, proteinsList.size());
        ProteinImpl protein = proteinsList.get(0);

        /**Create in intact db a second protein with primary identity xref to uniprot equal to the primary id of the uniprot entry.**/
        Protein duplicatedPrimaryProt = new ProteinImpl(IntactContext.getCurrentInstance().getInstitution(),
                protein.getBioSource(),
                protein.getShortLabel(),
                protein.getCvInteractorType());
        proteinDao.saveOrUpdate((ProteinImpl) duplicatedPrimaryProt);
        duplicatedPrimaryProt.setSequence("BLABLA");
        InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(),
                uniprot,
                MockUniprotProtein.CANFA_PRIMARY_AC,
                identity);
        newXref.setParent(duplicatedPrimaryProt);
        XrefDao xrefDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        duplicatedPrimaryProt.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) duplicatedPrimaryProt);



        /**Create in intact db a protein with primary identity xref to uniprot equal to the secondary id of the uniprot entry.**/
        Protein secondaryProt = new ProteinImpl(IntactContext.getCurrentInstance().getInstitution(),
                protein.getBioSource(),
                protein.getShortLabel(),
                protein.getCvInteractorType());
        proteinDao.saveOrUpdate((ProteinImpl) secondaryProt);
        secondaryProt.setSequence("BLABLA");
        newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(),
                uniprot,
                MockUniprotProtein.CANFA_SECONDARY_AC_1,
                identity);
        newXref.setParent(secondaryProt);
        xrefDao.saveOrUpdate(newXref);
        secondaryProt.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) secondaryProt);
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        // Make sure that we have set the database so that there is 2 protein in Intact for the uniprot primary Ac and
        // one corresponding to the uniprot secondary ac.
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(2,proteinsList.size());
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(1,proteinsList.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(0,proteinsColl.size());
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_SECONDARY_AC_1 );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(0,proteinsColl.size());

        System.out.println("proteinsColl.size() = " + proteinsColl.size());
//        Collection<String> errors =  uniprotServiceResult.getErrors();
//        //        assertEquals(1,messages.size());
//        for (String message : errors){
//            System.out.println("MESSAGE : ");
//            System.out.println("message = " + message);
////            assertTrue(message.contains("Unexpected number of protein found in IntAct for UniprotEntry(P60952) Count of " +
////                    "protein in Intact for the Uniprot entry primary ac(1) for the Uniprot entry secondary ac(s)(1)"));
//        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        // Make sure that we still have in the database, 2 proteins for the uniprot primary Ac and
        // one corresponding to the uniprot secondary ac (check that nothing as been updated).
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(2,proteinsList.size());
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(1,proteinsList.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

    }

    @Test
    public void testRetrieve_spliceVariantWith2UniprotIdentity() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins();
        assertEquals( 3,proteinsColl.size() );
        String proteinAc = "";
        for(Protein protein : proteinsColl){
            proteinAc = protein.getAc();
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class, CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(1, proteinsList.size());
        ProteinImpl protein = proteinsList.get(0);
        List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants(protein);
        ProteinImpl spliceVariant = spliceVariants.get(0);
        assertNotNull(ProteinUtils.getUniprotXref(spliceVariant));
        InteractorXref newXref = new InteractorXref(IntactContext.getCurrentInstance().getInstitution(),
                uniprot,
                "P12345",
                identity);
        newXref.setParent(spliceVariant);
        XrefDao xrefDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        spliceVariant.addXref(newXref);
        proteinDao.saveOrUpdate(spliceVariant);
        // We know that the spliceVariant we have created is wrong, the retrieve method will check if it's used in an
        // interaction, if it's not it will delete, if it is it will just sent a message. Here we don't want the splice
        // variant to be deleted, so we attach it to the an interaction.
        Institution owner = IntactContext.getCurrentInstance().getInstitution();
        BioSource bioSource = new BioSource(owner, "human", "9606");
        BioSourceDao bioSourceDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getBioSourceDao();
        bioSourceDao.saveOrUpdate(bioSource);
        Experiment exp = new Experiment(owner, "yang-1997-1", bioSource);
        ExperimentDao expDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getExperimentDao();
        expDao.saveOrUpdate(exp);
        Collection<Experiment> experiments = new ArrayList<Experiment>();
        experiments.add(exp);
        CvContext cvContext = IntactContext.getCurrentInstance().getCvContext();
        Interaction interaction = new InteractionImpl(experiments,
                cvContext.getByMiRef(CvInteractionType.class, CvInteractionType.DIRECT_INTERACTION_MI_REF),
                cvContext.getByMiRef(CvInteractorType.class,CvInteractorType.DNA_MI_REF), "dlw2-arg1-1", owner);
        InteractionDao interactionDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getInteractionDao();
        interactionDao.saveOrUpdate((InteractionImpl) interaction);
        exp.addInteraction(interaction);
        expDao.saveOrUpdate(exp);
        Component component = new Component(owner, interaction, spliceVariant,
                cvContext.getByMiRef(CvExperimentalRole.class, CvExperimentalRole.ANCILLARY_MI_REF),
                cvContext.getByMiRef(CvBiologicalRole.class, CvBiologicalRole.COFACTOR_MI_REF));
        ComponentDao componentDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getComponentDao();
        componentDao.saveOrUpdate(component);
        interaction.addComponent(component);
        interactionDao.saveOrUpdate((InteractionImpl) interaction);

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        //todo : in this case it will not return an error but a message as the splice variant has no active instance it
        //just delete it. Change the test : check on message size and content and add a test to check that if the splice
        // variant is involved in an  interaction it returns an error.

        Set <Map.Entry<String,String>> errorSet = errors.entrySet();
        Iterator<Map.Entry<String,String>> iterator = errorSet.iterator();
        while(iterator.hasNext()){
            Map.Entry entry = iterator.next();
            System.out.println(entry.getKey() + ":" + entry.getValue());
        }
        assertEquals(2,errors.size());
        boolean multipleIdErrorFound = false;
        boolean notSpliceVarInIntactErrorFound = false;
        for(String errorType : keySet){
            String error = errors.get(errorType);
            if(error.contains("Splice variants with multiple identity")){
                multipleIdErrorFound = true;
            }
            if (error.contains("Protein being a splice variant in IntAct but not in Uniprot and being part of an interaction.")){
                notSpliceVarInIntactErrorFound = true;
            }
        }
        // if this is false it means that the protein update didn't realise the splice variant had 2 identities.
        assertTrue(multipleIdErrorFound);
        //if this is false it means that the protein update didn't realise that the one of the uniprot identity of the
        // splice variant did not correspond to a protein that is in Uniprot a splice variant of the given master (ie. :
        // one of the identity is wrong and does not even correspond to a splice variant)
        assertTrue(notSpliceVarInIntactErrorFound);
        // A message for the wrong splice variant will be sent but the
        // uniprotService will re-create a correct splice variant and still return the protein with it's 2 splice
        // variants as normal.
        // todo : what shall we do as in this stage it would be twice the splice variant : 1 newly created and correct
        // and one old with 2 identities.
        assertEquals(3,uniprotServiceResult.getProteins().size());
        
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


    }

    @Test
    public void testRetrieve_spliceVariantFoundInIntactNotInUniprot() throws Exception{

        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        if(IntactContext.getCurrentInstance().getCvContext().getByLabel(CvTopic.class, "to-delete") == null) {
            CvTopic toDelete = new CvTopic(IntactContext.getCurrentInstance().getInstitution(), "to-delete");
            CvObjectDao cvObjectDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getCvObjectDao(CvTopic.class);
            cvObjectDao.saveOrUpdate(toDelete);
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins();
        assertEquals( 3,proteinsColl.size() );
        String proteinAc = "";
        for(Protein protein : proteinsColl){
            proteinAc = protein.getAc();
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        //In the uniprotService replace canfa with splice variants by canfa without, so that when will try to retrieve
        // canfa, we can test the case where the intact protein has splice variants but not the uniprot entry.
        UniprotProtein canfaWithNoSpliceVariant = MockUniprotProtein.build_CDC42_CANFA_WITH_NO_SPLICE_VARIANT();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfaWithNoSpliceVariant );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfaWithNoSpliceVariant );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfaWithNoSpliceVariant );
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        //todo : as the splice variant is not attached to any interaction it's ac will be put in the ProteinToDeleteManager
        //  it won't be deleted as it has to be done manually, but no error message will be sent just a message will be
        // sent.
        assertEquals(1,uniprotServiceResult.getProteins().size());

        Collection<String> messages = uniprotServiceResult.getMessages();
        for(String message : messages){
            System.out.println("message = " + message);
        }
        assertNotNull(uniprotServiceResult.getMessages());
        // todo : check why there's twice the same message.
        assertEquals(2,uniprotServiceResult.getMessages().size());

        // Assert that the message found it the write one.
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


    }

    @Test
    public void testRetrieve_1spliceVariantFoundInIntact2InUniprot() throws Exception{
        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_1, canfa );
        uniprotService.add( MockUniprotProtein.CANFA_SECONDARY_AC_2, canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );
        //Create the CANFA protein in the empty database, assert it has been created And commit.
        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteinsColl = uniprotServiceResult.getProteins();
        assertEquals( 3,proteinsColl.size() );
        String proteinAc = "";
        for(Protein protein : proteinsColl){
            proteinAc = protein.getAc();
        }
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        ProteinDao proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        CvDatabase uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class,CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class,CvXrefQualifier.IDENTITY_MI_REF);
        List<ProteinImpl> proteins = proteinDao.getByXrefLike(uniprot, identity, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertNotNull(proteins);
        assertEquals(1,proteins.size());
        Protein intactCanfa = proteins.get(0);
        List<ProteinImpl> spliceVariants = proteinDao.getSpliceVariants(intactCanfa);
        assertEquals(2,spliceVariants.size());
        //We delete one of the splice variants so that the intact canfa has only 1 splice variant when the uniprot canfa
        //got one.
        ProteinImpl splice2delete = spliceVariants.get(1);
        proteinDao.delete(splice2delete);
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        //Check that canfa has 1 splice variant and update it in Intact
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class,CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class,CvXrefQualifier.IDENTITY_MI_REF);
        proteins = proteinDao.getByXrefLike(uniprot, identity, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertNotNull(proteins);
        assertEquals(1,proteins.size());
        intactCanfa = proteins.get(0);
        spliceVariants = proteinDao.getSpliceVariants(intactCanfa);
        assertEquals(1,spliceVariants.size());
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        assertEquals(3, uniprotServiceResult.getProteins().size());
        Map<String,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        for(String errorType : keySet){
            System.out.println(errors.get(errorType));
        }
        assertEquals(0, uniprotServiceResult.getErrors().size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        //Check now that canfa has 2 splice variants now.
        IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        proteinDao = IntactContext.getCurrentInstance().getDataContext().getDaoFactory().getProteinDao();
        uniprot = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvDatabase.class,CvDatabase.UNIPROT_MI_REF);
        identity = IntactContext.getCurrentInstance().getCvContext().getByMiRef(CvXrefQualifier.class,CvXrefQualifier.IDENTITY_MI_REF);
        proteins = proteinDao.getByXrefLike(uniprot, identity, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertNotNull(proteins);
        assertEquals(1,proteins.size());
        intactCanfa = proteins.get(0);
        spliceVariants = proteinDao.getSpliceVariants(intactCanfa);
        assertEquals(2,spliceVariants.size());
        IntactContext.getCurrentInstance().getDataContext().commitTransaction();


    }

    @Test
    public void testRetrieve_TrEMBL_to_SP() throws Exception {
        // checks that protein moving from TrEMBL to SP are detected and updated accordingly.
        // Essentially, that means having a new Primary AC and the current on in the databse becoming secondary.

        // clear database content.
        clearProteinsFromDatabase();

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        //  ACs are P60952, P21181, P25763
        uniprotService.add( "P60952", canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );

        UniprotServiceResult uniprotServiceResult = service.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = proteins.iterator().next();
        String proteinAc = protein.getAc();
        String proteinSeq = protein.getSequence();

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();

        // Set a new primary Id
        canfa.getSecondaryAcs().add( 0, "P60952" );
        canfa.setPrimaryAc( "P12345" );
        canfa.setSequence( "XXXX" );
        canfa.setSequenceLength( canfa.getSequence().length() );
        canfa.setCrc64( "YYYYYYYYYYYYYY" );

        uniprotService.clear();
        uniprotService.add( "P12345", canfa );

        IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        uniprotServiceResult = service.retrieve( "P12345" );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        protein = proteins.iterator().next();

        // check that we have retrieved the exact same protein.
        assertEquals( proteinAc, protein.getAc() );
        assertEquals( "XXXX", protein.getSequence() );

        IntactContext.getCurrentInstance().getDataContext().commitTransaction();
    }

    @Test
    public void testConstructor() throws Exception{
        try{
            ProteinService proteinService = new ProteinServiceImpl(null);
            fail("Should have thrown and IllegalArgumentExcpetion.");
        }catch(IllegalArgumentException e){
            assertTrue(true);
        }
    }

    @Test
    public void testSetBioSource() throws Exception{
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        try{
            service.setBioSourceService(null );
            fail("Should have thrown and IllegalArgumentExcpetion.");
        }catch(IllegalArgumentException e){
            assertTrue(true);
        }
    }

}
