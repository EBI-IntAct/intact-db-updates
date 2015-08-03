package uk.ac.ebi.intact.util.protein;

import org.junit.Before;
import org.junit.Ignore;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import uk.ac.ebi.intact.bridges.taxonomy.DummyTaxonomyService;
import uk.ac.ebi.intact.core.config.CvPrimer;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.CvObjectDao;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.core.persistence.dao.XrefDao;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.mock.FlexibleMockUniprotService;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotService;
import uk.ac.ebi.intact.util.protein.utils.ProteinToDeleteManager;
import uk.ac.ebi.intact.util.protein.utils.UniprotServiceResult;

import java.util.*;

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
public class ProteinServiceImpl2Test extends IntactBasicTestCase {

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


     /**
     * Check that nothing is update if more then 1 proteins are found in uniprot.
     */
    @Test
    @Ignore
    public void retrieve_uniprotAcReturningMoreThan1EntryWithDifferentSpecies() throws Exception {

        // Create in the db, the CANFA protein with primary Ac P60952

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


        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        Collection<ProteinImpl> intactProteins = proteinDao.getByUniprotId(canfa.getPrimaryAc());
        assertEquals(0, intactProteins.size());


    }

    /**
     * Check that nothing is update if more then 1 proteins are found in uniprot.
     */
    @Test
    @Ignore
    public void retrieve_uniprotAcReturningMoreThan1EntryWithSameSpecies() throws Exception{

        /*----------------------------------------------------------
       Create in the db, the CANFA protein with primary Ac P60952
        -----------------------------------------------------------*/


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
//        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
//        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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
    @Ignore
    public void retrieve_primaryCount0_secondaryCount2() throws Exception{

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



        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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
        XrefDao xrefDao = getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        duplicatedProtein.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) duplicatedProtein);
        System.out.println("ac = " + ac);
        System.out.println("duplicatedProtein.getAc() = " + duplicatedProtein.getAc());



        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(2, proteinsList.size());
        proteinsList = proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(0, proteinsList.size());


        // we will find 4 proteins : 2 duplicated proteins (ProteinServiceImpl doesn't merge duplicated proteins) and 2 splice variants.
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(4,proteinsColl.size());
        System.out.println("proteinsColl.size() = " + proteinsColl.size());
        Map<String ,String> errors = uniprotServiceResult.getErrors();
        Set<String> keySet = errors.keySet();
        assertEquals(1,errors.size());


    }

    @Test
    @Ignore
    public void retrieve_primaryCount1_secondaryCount1() throws Exception{


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



        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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
        XrefDao xrefDao = getDaoFactory().getXrefDao();
        xrefDao.saveOrUpdate(newXref);
        secondaryProt.addXref(newXref);
        proteinDao.saveOrUpdate((ProteinImpl) secondaryProt);
        String secondaryProtAc = secondaryProt.getAc();

        // one error should be found because we have a duplicate. ProteinServiceImpl doesn't process duplicates!!!
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        assertEquals(1,uniprotServiceResult.getErrors().size());

        // ProteinServiceImpl doesn't process duplicates!!! So we don't merge the duplicates
        Collection<String> acsOfProtToDelete = ProteinToDeleteManager.getAcToDelete();
        assertEquals(0, acsOfProtToDelete.size());
        proteinDao = getDaoFactory().getProteinDao();
        for(String ac : acsOfProtToDelete){
            ProteinImpl protToDel = proteinDao.getByAc(ac);
            proteinDao.delete(protToDel);
        }



        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        // we have 3 proteins + 1 duplicate not deleted because no duplicateFixer
        assertEquals(4,proteinsColl.size());
        proteinDao = getDaoFactory().getProteinDao();

        ProteinImpl proteinPrimaryAc = proteinDao.getByAc(primaryProteinAc);
        ProteinImpl proteinSecondaryAc = proteinDao.getByAc(secondaryProtAc);
        if((proteinPrimaryAc==null && proteinSecondaryAc==null)){
            fail("one of them shouldn't be null, because they should have been merged into one protein");
        }
        // no duplicate fixer !!!
        //if((proteinPrimaryAc!=null && proteinSecondaryAc!=null)){
            //fail("one of them should be null, they should have been " +
                    //"merged into one but not both should have been deleted.");
        //}
        //assertTrue((proteinPrimaryAc==null && proteinSecondaryAc!=null) || (proteinPrimaryAc!=null && proteinSecondaryAc==null));

        System.out.println("proteinsColl.size() = " + proteinsColl.size());

    }

    @Test
    @Ignore
    public void retrieve_throwException() throws Exception{


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
//        
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
//        
//
//
//    }

    @Test
    @Ignore
    public void retrieve_primaryCount2_secondaryCount1() throws Exception{

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



        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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
        XrefDao xrefDao = getDaoFactory().getXrefDao();
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


        // Make sure that we have set the database so that there is 2 protein in Intact for the uniprot primary Ac and
        // one corresponding to the uniprot secondary ac.

        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        assertEquals(2,proteinsList.size());
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(1,proteinsList.size());




        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_PRIMARY_AC );
        proteinsColl = uniprotServiceResult.getProteins();
        // we have 1 protein with 2 duplicates (one secodary and 2 primary) and we have 2 splice variants
        assertEquals(5,proteinsColl.size());
        uniprotServiceResult = service.retrieve( MockUniprotProtein.CANFA_SECONDARY_AC_1 );
        proteinsColl = uniprotServiceResult.getProteins();
        assertEquals(5,proteinsColl.size());

        System.out.println("proteinsColl.size() = " + proteinsColl.size());
//        Collection<String> errors =  uniprotServiceResult.getErrors();
//        //        assertEquals(1,messages.size());
//        for (String message : errors){
//            System.out.println("MESSAGE : ");
//            System.out.println("message = " + message);
////            assertTrue(message.contains("Unexpected number of protein found in IntAct for UniprotEntry(P60952) Count of " +
////                    "protein in Intact for the Uniprot entry primary ac(1) for the Uniprot entry secondary ac(s)(1)"));
//        }


        // Make sure that we still have in the database, 2 proteins for the uniprot primary Ac and
        // one corresponding to the uniprot secondary ac (check that nothing as been updated).

        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_PRIMARY_AC);
        // 2 + 1 duplicated
        assertEquals(3,proteinsList.size());
        // all secondary acs have been updated
        proteinsList =  proteinDao.getByXrefLike(uniprot, identity,MockUniprotProtein.CANFA_SECONDARY_AC_1);
        assertEquals(0,proteinsList.size());


    }

    @Test
    @Ignore
    public void retrieve_spliceVariantFoundInIntactNotInUniprot() throws Exception{


        if(getDaoFactory().getCvObjectDao().getByShortLabel(CvTopic.class, "to-delete") == null) {
            CvTopic toDelete = new CvTopic(IntactContext.getCurrentInstance().getInstitution(), "to-delete");
            CvObjectDao cvObjectDao = getDaoFactory().getCvObjectDao(CvTopic.class);
            cvObjectDao.saveOrUpdate(toDelete);
        }




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



    }

    @Test
    @Ignore
    public void retrieve_1spliceVariantFoundInIntact2InUniprot() throws Exception{


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



        ProteinDao proteinDao = getDaoFactory().getProteinDao();
        CvDatabase uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        CvXrefQualifier identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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


        //Check that canfa has 1 splice variant and update it in Intact

        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
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


        //Check now that canfa has 2 splice variants now.

        proteinDao = getDaoFactory().getProteinDao();
        uniprot = getDaoFactory().getCvObjectDao(CvDatabase.class).getByPsiMiRef(CvDatabase.UNIPROT_MI_REF);
        identity = getDaoFactory().getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.IDENTITY_MI_REF);
        proteins = proteinDao.getByXrefLike(uniprot, identity, MockUniprotProtein.CANFA_PRIMARY_AC);
        assertNotNull(proteins);
        assertEquals(1,proteins.size());
        intactCanfa = proteins.get(0);
        spliceVariants = proteinDao.getSpliceVariants(intactCanfa);
        assertEquals(2,spliceVariants.size());



    }

    @Test
    @Ignore
    public void retrieve_TrEMBL_to_SP() throws Exception {
        // checks that protein moving from TrEMBL to SP are detected and updated accordingly.
        // Essentially, that means having a new Primary AC and the current on in the databse becoming secondary.



        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA();
        String previousSequence = canfa.getSequence();

        //  ACs are P60952, P21181, P25763
        uniprotService.add( "P60952", canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );

        UniprotServiceResult uniprotServiceResult = service.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );
        Protein protein = null;
        for (Protein pro : proteins){
            if (ProteinUtils.getUniprotXref(pro).getPrimaryId().equals("P60952")){
                protein = pro;
            }
        }
        String proteinAc = protein.getAc();
        String proteinSeq = protein.getSequence();



        // Set a new primary Id
        canfa.getSecondaryAcs().add( 0, "P60952" );
        canfa.setPrimaryAc( "P12345" );
        canfa.setSequence( "XXXX" );
        canfa.setSequenceLength( canfa.getSequence().length() );
        canfa.setCrc64( "YYYYYYYYYYYYYY" );

        uniprotService.clear();
        uniprotService.add( "P12345", canfa );



        uniprotServiceResult = service.retrieve( "P12345" );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 3, proteins.size() );

        for (Protein pro : proteins){
            if (ProteinUtils.getUniprotXref(pro).getPrimaryId().equals("P12345")){
                protein = pro;
            }
        }

        // check that we have retrieved the exact same protein.
        assertEquals( proteinAc, protein.getAc() );

        assertEquals( "XXXX", protein.getSequence() );


    }

    @Test
    @Ignore
    public void setBioSource() throws Exception{
        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        try{
            service.setBioSourceService(null );
            fail("Should have thrown and IllegalArgumentExcpetion.");
        }catch(IllegalArgumentException e){
            assertTrue(true);
        }
    }

    @Test
    @Ignore
    public void alias_update() throws Exception {
        // Aim: load a protein from uniprot into IntAct, then change its gene name and check that on the next update
        //      the intact object has been updated accordingly.



        FlexibleMockUniprotService uniprotService = new FlexibleMockUniprotService();
        UniprotProtein canfa = MockUniprotProtein.build_CDC42_CANFA_WITH_NO_SPLICE_VARIANT();
        //  ACs are P60952, P21181, P25763
        uniprotService.add( "P60952", canfa );

        ProteinService service = ProteinServiceFactory.getInstance().buildProteinService( uniprotService );
        service.setBioSourceService( BioSourceServiceFactory.getInstance().buildBioSourceService( new DummyTaxonomyService() ) );

        UniprotServiceResult uniprotServiceResult = service.retrieve( "P60952" );
        Collection<Protein> proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 1, proteins.size() );
        Protein protein = proteins.iterator().next();
        final String ac = protein.getAc();
        assertNotNull( ac );





        // update the Uniprot entry
        canfa.getGenes().clear();

        uniprotServiceResult = service.retrieve( "P60952" );
        proteins = uniprotServiceResult.getProteins();
        assertNotNull( proteins );
        assertEquals( 1, proteins.size() );
        protein = proteins.iterator().next();
        assertNotNull( protein.getAc() );
        assertEquals( ac, protein.getAc() );

        assertEquals( 0, protein.getAliases().size() );

    }
}
