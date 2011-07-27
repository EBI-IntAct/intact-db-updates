package uk.ac.ebi.intact.util.protein;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;
import uk.ac.ebi.intact.core.IntactTransactionException;
import uk.ac.ebi.intact.core.config.CvPrimer;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.Crc64;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.mock.FlexibleMockUniprotService;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;
import uk.ac.ebi.intact.util.protein.mock.UniprotProteinXrefBuilder;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.Collection;
import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.Assert.*;

/**
 * ProteinServiceImpl Tester.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since TODO artifact version
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinServiceImplTest extends IntactBasicTestCase {

    @Before
    public void before() throws Exception {
        //SchemaUtils.createSchema();   


        CvPrimer cvPrimer = new ComprehensiveCvPrimer(getDaoFactory());
        cvPrimer.createCVs();
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( false );
        ProteinUpdateContext.getInstance().getConfig().setDeleteProteinTranscriptWithoutInteractions( false );
        ProteinUpdateContext.getInstance().getConfig().setFixDuplicates( true );
        ProteinUpdateContext.getInstance().getConfig().setDeleteProtsWithoutInteractions( true );

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

    private Protein getProteinForPrimaryAc(Collection<Protein> proteins, String primaryAc) throws ProteinServiceException {
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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

    private Protein getProteinByShortlabel( Protein[] proteins, String label ) {
        for ( Protein protein : proteins ) {
            if( label.equalsIgnoreCase( protein.getShortLabel() ) ) {
                return protein;
            }
        }
        return null;
    }

    ////////////////////
    // Tests

    @Test
    @Ignore
    public void retrieve_CDC42_CANFA() throws Exception {

        ProteinService service = buildProteinService();

        UniprotServiceResult uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */

        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = getProteinByShortlabel( proteins.toArray( new Protein[]{} ), "cdc42_canfa" );
        assertNotNull( protein );
        assertEquals( 7, protein.getXrefs().size() );



        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );

        // Make sure that the uniprotService still contains the protein and it's 2 splice variants, as we got a bug
        // due to a method that was deleting the splice variants (findMatches method in ProteinServiceImpl class).
        UniprotService uniprotService = service.getUniprotService();
        Collection<UniprotProtein>uniprotProteins = uniprotService.retrieve(MockUniprotProtein.CANFA_PRIMARY_AC);
        for(UniprotProtein prot : uniprotProteins){
            Collection<UniprotSpliceVariant> svs = prot.getSpliceVariants();
            assertEquals(2, svs.size());
        }
        assertEquals( 3, proteins.size() );
        //Check that proteins contains, P60952 and its 2 splice variants.
        boolean P60952found = false;
        boolean P60952_1found = false;
        boolean P60952_2found = false;
        for(Protein p : proteins){
            InteractorXref uniprotIdentity = ProteinUtils.getUniprotXref(p);
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
        protein = getProteinForPrimaryAc(proteins, MockUniprotProtein.CANFA_PRIMARY_AC);

        DaoFactory daoFactory = getDaoFactory();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();
        Institution owner = IntactContext.getCurrentInstance().getInstitution();
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

        Protein sv1 = getProteinByShortlabel( variants.toArray( new Protein[variants.size()] ), "P60952-1" );
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

        Protein sv2 = getProteinByShortlabel( variants.toArray( new Protein[variants.size()] ), "P60952-2" );
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



        uniprotServiceResult =  service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC); /* CDC42_CANFA */
        proteins = uniprotServiceResult.getProteins();

        assertNotNull(service.retrieve("P60952-2"));
        pdao = getDaoFactory().getProteinDao();
        Protein sv = pdao.getByAc(sv1ac);
        InteractorXref svXref = ProteinUtils.getUniprotXref(sv);
        assertEquals(svXref.getPrimaryId(), sv1.getShortLabel().toUpperCase());

        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );

        protein = getProteinForPrimaryAc(proteins, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals( ac, protein.getAc() );

        variants = pdao.getSpliceVariants( protein );
        assertEquals( 2, variants.size() );

        sv1 = getProteinByShortlabel( variants.toArray( new Protein[variants.size()] ), "P60952-1" );
        assertEquals( sv1ac, sv1.getAc() );

        sv2 = getProteinByShortlabel( variants.toArray( new Protein[variants.size()] ), "P60952-2" );
        assertEquals( sv2ac, sv2.getAc() );


    }

    @Test
    @Ignore
    public void retrieve_spliceVariant() throws Exception {


        FlexibleMockUniprotService service = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        service.add( MockUniprotProtein.CANFA_PRIMARY_AC, canfa );
        service.add( "P60952-2", canfa );
        ProteinService proteinService = buildProteinService( service );
        UniprotServiceResult uniprotServiceResult = proteinService.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );



        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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



        uniprotServiceResult = proteinService.retrieve( "P60952-2" );
        Collection<Protein> resultProteins = uniprotServiceResult.getProteins();
        assertEquals(3, resultProteins.size());
        boolean found = false;
        for(Protein prot : resultProteins){
            if (parentProteinAc.equals(prot.getAc())){
                found = true;
            }
        }



        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        prots = proteinDao.getByXrefLike(uniprot, identity, "P60952-2");
        assertEquals(1, prots.size());
        spliceVariant = prots.get(0);
        assertEquals(spliceVariantAc, spliceVariant.getAc());
        assertEquals(spliceVariantShortlabel, spliceVariant.getShortLabel());

    }

    @Test
    @Ignore
    public void retrieve_update_CDC42_CANFA() throws Exception {


        FlexibleMockUniprotService service = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        service.add( "P60952", canfa );

        ProteinService proteinService = buildProteinService( service );
        UniprotServiceResult uniprotServiceResult = proteinService.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = getProteinByShortlabel( proteins.toArray( new Protein[]{} ), "cdc42_canfa" );
        assertNotNull( protein );
        assertEquals( 7, protein.getXrefs().size() );




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
        Map<String, String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        for(String errorType : keySet){
            String error = errors.get(errorType);
            System.out.println("error message is : " + error);
        }
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        protein = getProteinByShortlabel( proteins.toArray( new Protein[]{} ), "foo_bar" );
        assertNotNull( protein );

        assertEquals( "LALALA", protein.getFullName() );
        assertEquals( "LLLLLLLLLLLLL", protein.getSequence() );
        assertEquals( "XXXXXXXXXXXXX", protein.getCrc64() );

        DaoFactory daoFactory = getDaoFactory();
        CvObjectDao<CvObject> cvDao = daoFactory.getCvObjectDao();

        Institution owner = IntactContext.getCurrentInstance().getInstitution();
        CvDatabase interpro = ( CvDatabase ) cvDao.getByPsiMiRef( CvDatabase.INTERPRO_MI_REF );
        CvAliasType gene = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.GENE_NAME_MI_REF );
        CvAliasType synonym = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.GENE_NAME_SYNONYM_MI_REF );
        CvAliasType orf = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.ORF_NAME_MI_REF );
        CvAliasType locus = ( CvAliasType ) cvDao.getByPsiMiRef( CvAliasType.LOCUS_NAME_MI_REF );

        // 6 Xrefs: 3 UniProt + 3 InterPro
        assertEquals( 6, protein.getXrefs().size() );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR00000", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR013753", null ) ) );
        assertTrue( protein.getXrefs().contains( new InteractorXref( owner, interpro, "IPR001806", null ) ) );

        assertEquals( 4, protein.getAliases().size() );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, gene, "foo" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, synonym, "s" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, orf, "o" ) ) );
        assertTrue( protein.getAliases().contains( new InteractorAlias( owner, protein, locus, "l" ) ) );


    }

    @Test
    @Ignore
    public void retrieve_sequenceUpdate() throws ProteinServiceException, IntactTransactionException {

        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        uniprotService.add( "P60952", canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );



        UniprotServiceResult uniprotServiceResult =  service.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = null;

        for (Protein prot : proteins){
            InteractorXref ref = ProteinUtils.getUniprotXref(prot);

            if (ref != null && ref.getPrimaryId().equals("P60952")){
                protein = prot;
            }
        }

        assertTrue(protein != null);
        String proteinSeq = protein.getSequence();
        String proteinCrc = protein.getCrc64();

        // Update the seqence/CRC of the protein
        System.out.println("SEQ: "+proteinSeq);
        assertTrue( proteinSeq.length() > 20 );
        String newSequence = proteinSeq.substring( 2, 20 );
        canfa.setSequence( newSequence );
        canfa.setSequenceLength( canfa.getSequence().length() );
        canfa.setCrc64( Crc64.getCrc64( canfa.getSequence() ) );

        protein = null;


        uniprotServiceResult = service.retrieve( "P60952" );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );

        for (Protein pro : proteins){
            if (ProteinUtils.getUniprotXref(pro).getPrimaryId().equals("P60952")){
                protein = pro;
            }
        }

        // check that we have retrieved the exact same protein.
        assertEquals( newSequence, protein.getSequence() );
        assertEquals( Crc64.getCrc64( newSequence ), protein.getCrc64() );


    }

    @Test
    @Ignore
    public void retrieve_intact1_uniprot0() throws Exception{


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


        // not valid anymore because if a protein is not found in uniprot, it is a dead entry.
        
        //Map<String ,String> errors = uniprotServiceResult.getErrors();
        //Set<String> keySet = errors.keySet();
        //assertEquals(1,errors.size());
        //for(String errorType : keySet){
            //String error = errors.get(errorType);
           // assertEquals("Couldn't update protein with uniprot id = " + uniprotServiceResult.getQuerySentToService() + ". It was found" +
                    //" in IntAct but was not found in Uniprot.", error);
        //}



    }

    @Test
    @Ignore
    public void retrieve_intact0_uniprot0() throws Exception{


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


    }

    /**
     * Test that the protein xref and the protein are udpated when : countPrimary == 0 && countSecondary == 1
     */
    @Test
    @Ignore
    public void retrieve_primaryCount0_secondaryCount1() throws Exception{

        /*----------------------------------------------------------
        Create in the db, the CANFA protein with primary Ac P60952
         -----------------------------------------------------------*/


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



        /*--------------------------------------------------------------------------------------------------------------
        In the db, modify the sequence of P60952, and set it's xref identity to uniprot to P21181 which -in uniprot-
        is a secondary ac of P60952
         -------------------------------------------------------------------------------------------------------------*/

        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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


        //Retrieve (= update P60952)

        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull(proteins);
        assertEquals(3, proteins.size());



        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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


    }


}
