package uk.ac.ebi.intact.dbupdate.bioactiveentity.importer;

import junit.framework.Assert;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;
import uk.ac.ebi.intact.core.unit.IntactMockBuilder;
import uk.ac.ebi.intact.dbupdate.bioactiveentity.utils.BioActiveEntityUtils;
import uk.ac.ebi.intact.model.*;

import java.util.*;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 24/05/2013
 * Time: 10:44
 */
public class BioActiveEntityServiceImplTest {

    private BioActiveEntityService bioActiveEntityService;
    private IntactMockBuilder builder;
    private SmallMolecule water;

    private static final String CHEBI_ID = "CHEBI:15377"; //Water
    private static final String CHEBI_NAME = "water"; //Water

    private static final String NO_CHEBI_ID = "CHEIB:15377";
    private static final String NULL_CHEBI_ID = null; //Water

    private static final String CHEBI_ID_LONG_NAME = "CHEBI:45633";
    private static final String CHEBI_NAME_LONG_NAME_TRUNCATED = "(2S)-2-amino-4-{[1-(";
    private static final String CHEBI_NAME_LONG_NAME = "(2S)-2-amino-4-{[1-(6-oxo-1,6-dihydro-9H-purin-9-yl)-1," +
            "5-dideoxy-beta-D-ribofuranos-5-yl]sulfanyl}butanoic acid";


    @Before
    public void setUp() throws Exception {
        bioActiveEntityService = new BioActiveEntityServiceImpl();
        builder = new IntactMockBuilder(BioActiveEntityUtils.getInstitution());
        water = builder.createSmallMolecule(CHEBI_ID, CHEBI_NAME);
    }


    @After
    public void tearDown() {
        bioActiveEntityService = null;
        builder = null;
        water = null;
    }

    @Test
    public void testGetBioEntityByChebi() throws Exception {

        SmallMolecule chebiWater = bioActiveEntityService.getBioEntityByChebiId(CHEBI_ID);

        final SmallMolecule water = createWaterSmallMolecule();

        Assert.assertNotNull(chebiWater);

        Assert.assertEquals(water.getAc(), chebiWater.getAc());
        Assert.assertEquals(water.getShortLabel(), chebiWater.getShortLabel());
        Assert.assertEquals(water.getFullName(), chebiWater.getFullName());

        //This methods compare:
        // the xref cvDatabase, cvXrefQualifier, primaryId, secondaryId, release database
        // the alias type and name
        // the annotation type and text
        Collections.sort((List<InteractorXref>) water.getXrefs(), new XrefComparator());
        Collections.sort((List<InteractorXref>) chebiWater.getXrefs(), new XrefComparator());
        Assert.assertEquals(water.getXrefs(), chebiWater.getXrefs());
        Collections.sort((List<InteractorAlias>) water.getAliases(), new AliasesComparator());
        Collections.sort((List<InteractorAlias>) chebiWater.getAliases(), new AliasesComparator());
        Assert.assertEquals(water.getAliases(), chebiWater.getAliases());
        Collections.sort((List<Annotation>) water.getAnnotations(), new AnnotationComparator());
        Collections.sort((List<Annotation>) chebiWater.getAnnotations(), new AnnotationComparator());
        Assert.assertEquals(water.getAnnotations(), chebiWater.getAnnotations());

    }

    private class XrefComparator implements Comparator<InteractorXref> {
        @Override
        public int compare(InteractorXref o1, InteractorXref o2) {
            if (o1 != null && o2 != null) {
                if (o1.getPrimaryId() != null && o2.getPrimaryId() != null) {
                    return o1.getPrimaryId().compareTo(o2.getPrimaryId());
                } else {
                    if (o1.getPrimaryId() == null && o2.getPrimaryId() != null) {
                        return -1;
                    } else if (o1.getPrimaryId() != null && o2.getPrimaryId() == null) {
                        return 1;
                    }
                }
            } else {
                if (o1 == null && o2 != null) {
                    return -1;
                } else if (o1 != null && o2 == null) {
                    return 1;
                }
            }
            return 0;
        }
    }

    private class AnnotationComparator implements Comparator<Annotation> {
        @Override
        public int compare(Annotation o1, Annotation o2) {
            if (o1 != null && o2 != null) {
                if (o1.getCvTopic() != null && o2.getCvTopic() != null) {
                    return o1.getCvTopic().getIdentifier().compareTo(o2.getCvTopic().getIdentifier());
                } else {
                    if (o1.getCvTopic() == null && o2.getCvTopic() != null) {
                        return -1;
                    } else if (o1.getCvTopic() != null && o2.getCvTopic() == null) {
                        return 1;
                    }
                }
            } else {
                if (o1 == null && o2 != null) {
                    return -1;
                } else if (o1 != null && o2 == null) {
                    return 1;
                }
            }
            return 0;
        }
    }

    private class AliasesComparator implements Comparator<InteractorAlias> {
        @Override
        public int compare(InteractorAlias o1, InteractorAlias o2) {
            if (o1 != null && o2 != null) {
                if (o1.getCvAliasType() != null && o2.getCvAliasType() != null) {
                    if (o1.getCvAliasType().getIdentifier().compareTo(o2.getCvAliasType().getIdentifier()) == 0) {
                        String name1 = o1.getName();
                        String name2 = o2.getName();

                        //Same type we sort by name
                        if (name1 != null && name2 != null) {
                            return name1.compareTo(name2);
                        } else {
                            if (name1 == null && name2 != null) {
                                return -1;
                            } else if (name1 != null && name2 == null) {
                                return 1;
                            }
                        }
                    } else {
                        return 0;
                    }
                } else {
                    if (o1.getCvAliasType() == null && o2.getCvAliasType() != null) {
                        return -1;
                    } else if (o1.getCvAliasType() != null && o2.getCvAliasType() == null) {
                        return 1;
                    }
                }
            } else {
                if (o1 == null && o2 != null) {
                    return -1;
                } else if (o1 != null && o2 == null) {
                    return 1;
                }
            }
            return 0;
        }
    }

    @Test
    public void testTrucateShortLabel() throws BioActiveEntityServiceException {

        SmallMolecule chebi = bioActiveEntityService.getBioEntityByChebiId(CHEBI_ID_LONG_NAME);

        Assert.assertEquals(CHEBI_NAME_LONG_NAME_TRUNCATED, chebi.getShortLabel());
        Assert.assertEquals(CHEBI_NAME_LONG_NAME, chebi.getFullName());

    }

    @Test(expected = BioActiveEntityServiceException.class)
    public void testWrongChebiId() throws BioActiveEntityServiceException {

        SmallMolecule chebi = bioActiveEntityService.getBioEntityByChebiId(NO_CHEBI_ID);

    }

    @Test(expected = BioActiveEntityServiceException.class)
    public void testNullChebiId() throws BioActiveEntityServiceException {

        SmallMolecule chebi = bioActiveEntityService.getBioEntityByChebiId(NULL_CHEBI_ID);

    }

//    @Test
//    public void testGetBioEntityByChebiListRepeatedIds() throws BioActiveEntityServiceException {
//
//        List<String> repeatedChebiIds = createRepeatedChebiIds();
//
//        List<SmallMolecule> bioActiveEntities =
//                bioActiveEntityService.getBioEntityByChebiIdList(repeatedChebiIds);
//
//        Assert.assertEquals(1, bioActiveEntities.size());
//    }

//    @Test
//    public void testGetBioEntityByChebiListMoreThanMaximunChebiIds() throws BioActiveEntityServiceException {
//
//        List<String> moreThan50chebiIds = createMoreThan50chebiIds();
//
//        List<SmallMolecule> bioActiveEntities =
//                bioActiveEntityService.getBioEntityByChebiIdList(moreThan50chebiIds);
//
//        Assert.assertEquals(64, bioActiveEntities.size());
//
//    }
//
//    @Test
//    public void testGetBioEntityByChebiListLessThanMaximunChebiIds() throws BioActiveEntityServiceException {
//
//        List<String> lessThan50chebiIds = createLessThan50chebiIds();
//
//        List<SmallMolecule> bioActiveEntities =
//                bioActiveEntityService.getBioEntityByChebiIdList(lessThan50chebiIds);
//
//        Assert.assertEquals(32, bioActiveEntities.size());
//    }
//
//    @Test
//    public void testGetBioEntityByChebiListSecondaryIds() throws BioActiveEntityServiceException {
//
//        List<String> secondaryChebiIds = createSecondaryChebiIds();
//
//        List<SmallMolecule> bioActiveEntities =
//                bioActiveEntityService.getBioEntityByChebiIdList(secondaryChebiIds);
//
//        Assert.assertEquals(1, bioActiveEntities.size());
//    }


    private SmallMolecule createWaterSmallMolecule() {

        water.setFullName(CHEBI_NAME);

        //We add the secondary ids without remove the identity
        //Found 11 secondary IDs
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:27313"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:727419"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:44701"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:44819"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:42857"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:44292"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:43228"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:42043"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:5585"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:10743"));
        water.getXrefs().add(createSecondaryInteractorXref("CHEBI:13352"));

        //We replace the alias gene name that creates by default the mockup builder
        //Found 15 synonyms
        Collection<InteractorAlias> aliases = new ArrayList<InteractorAlias>();
        aliases.add(createSynonymInteractorAlias("[OH2]"));
        aliases.add(createSynonymInteractorAlias("acqua"));
        aliases.add(createSynonymInteractorAlias("agua"));
        aliases.add(createSynonymInteractorAlias("aqua"));
        aliases.add(createSynonymInteractorAlias("BOUND WATER"));
        aliases.add(createSynonymInteractorAlias("dihydridooxygen"));
        aliases.add(createSynonymInteractorAlias("dihydrogen oxide"));
        aliases.add(createSynonymInteractorAlias("eau"));
        aliases.add(createSynonymInteractorAlias("H2O"));
        aliases.add(createSynonymInteractorAlias("H2O"));
        aliases.add(createSynonymInteractorAlias("HOH"));
        aliases.add(createSynonymInteractorAlias("hydrogen hydroxide"));
        aliases.add(createSynonymInteractorAlias("Wasser"));
        aliases.add(createSynonymInteractorAlias("Water"));
        aliases.add(createSynonymInteractorAlias("WATER"));

        //Found 2 IUPAC names
        aliases.add(createIUPACInteractorAlias("oxidane"));
        aliases.add(createIUPACInteractorAlias("water"));

        water.setAliases(aliases);

        Collection<Annotation> annotations = new ArrayList<Annotation>();
        annotations.add(builder.createAnnotation("InChI=1S/H2O/h1H2", BioActiveEntityUtils.getInchiType()));
        annotations.add(builder.createAnnotation("XLYOFNOQVPJJNP-UHFFFAOYSA-N", BioActiveEntityUtils.getInchiKeyType()));
        annotations.add(builder.createAnnotation("O", BioActiveEntityUtils.getSmilesType()));

        water.setAnnotations(annotations);

        return water;
    }

    private InteractorXref createSecondaryInteractorXref(String secondaryId) {
        return builder.createXref(water, secondaryId,
                BioActiveEntityUtils.getSecondaryIDQualifier(),
                BioActiveEntityUtils.getChEBIDatabase());
    }

    private InteractorAlias createSynonymInteractorAlias(String text) {
        return builder.createAlias(water, text, BioActiveEntityUtils.getSynonymAliasType());
    }

    private InteractorAlias createIUPACInteractorAlias(String text) {
        return builder.createAlias(water, text, BioActiveEntityUtils.getIupacAliasType());
    }

    private static List<String> createMoreThan50chebiIds() {
        List<String> moreThan50chebiIds = new ArrayList<String>();

        moreThan50chebiIds.add("CHEBI:15377");
        moreThan50chebiIds.add("CHEBI:33813");
        moreThan50chebiIds.add("CHEBI:27314");
        moreThan50chebiIds.add("CHEBI:65383");
        moreThan50chebiIds.add("CHEBI:65384");
        moreThan50chebiIds.add("CHEBI:65382");
        moreThan50chebiIds.add("CHEBI:65385");
        moreThan50chebiIds.add("CHEBI:18219");
        moreThan50chebiIds.add("CHEBI:53502");
        moreThan50chebiIds.add("CHEBI:53503");
        moreThan50chebiIds.add("CHEBI:53504");
        moreThan50chebiIds.add("CHEBI:53542");
        moreThan50chebiIds.add("CHEBI:53667");
        moreThan50chebiIds.add("CHEBI:31520");
        moreThan50chebiIds.add("CHEBI:71015");
        moreThan50chebiIds.add("CHEBI:29375");
        moreThan50chebiIds.add("CHEBI:31642");
        moreThan50chebiIds.add("CHEBI:29374");
        moreThan50chebiIds.add("CHEBI:41981");
        moreThan50chebiIds.add("CHEBI:33811");
        moreThan50chebiIds.add("CHEBI:33806");
        moreThan50chebiIds.add("CHEBI:31822");
        moreThan50chebiIds.add("CHEBI:35511");
        moreThan50chebiIds.add("CHEBI:3566");
        moreThan50chebiIds.add("CHEBI:4496");
        moreThan50chebiIds.add("CHEBI:30115");
        moreThan50chebiIds.add("CHEBI:31795");
        moreThan50chebiIds.add("CHEBI:32138");
        moreThan50chebiIds.add("CHEBI:32142");
        moreThan50chebiIds.add("CHEBI:32150");
        moreThan50chebiIds.add("CHEBI:32583");
        moreThan50chebiIds.add("CHEBI:32584");     //32
        moreThan50chebiIds.add("CHEBI:32586");
        moreThan50chebiIds.add("CHEBI:33112");
        moreThan50chebiIds.add("CHEBI:53437");
        moreThan50chebiIds.add("CHEBI:59199");
        moreThan50chebiIds.add("CHEBI:59733");
        moreThan50chebiIds.add("CHEBI:63686");
        moreThan50chebiIds.add("CHEBI:63938");
        moreThan50chebiIds.add("CHEBI:64746");
        moreThan50chebiIds.add("CHEBI:64754");
        moreThan50chebiIds.add("CHEBI:9179");
        moreThan50chebiIds.add("CHEBI:34730");
        moreThan50chebiIds.add("CHEBI:3395");
        moreThan50chebiIds.add("CHEBI:7810");
        moreThan50chebiIds.add("CHEBI:30176");
        moreThan50chebiIds.add("CHEBI:31440");
        moreThan50chebiIds.add("CHEBI:32312");
        moreThan50chebiIds.add("CHEBI:34836");
        moreThan50chebiIds.add("CHEBI:36385");
        moreThan50chebiIds.add("CHEBI:38888");
        moreThan50chebiIds.add("CHEBI:52751");
        moreThan50chebiIds.add("CHEBI:52967");
        moreThan50chebiIds.add("CHEBI:53442");
        moreThan50chebiIds.add("CHEBI:58994");
        moreThan50chebiIds.add("CHEBI:59176");
        moreThan50chebiIds.add("CHEBI:59587");
        moreThan50chebiIds.add("CHEBI:59902");
        moreThan50chebiIds.add("CHEBI:59936");
        moreThan50chebiIds.add("CHEBI:60648");
        moreThan50chebiIds.add("CHEBI:60789");
        moreThan50chebiIds.add("CHEBI:60791");
        moreThan50chebiIds.add("CHEBI:63858");
        moreThan50chebiIds.add("CHEBI:63939");

        return moreThan50chebiIds;
    }

    private static List<String> createLessThan50chebiIds() {

        List<String> lessThan50chebiIds = new ArrayList<String>();

        lessThan50chebiIds.add("CHEBI:15377");
        lessThan50chebiIds.add("CHEBI:33813");
        lessThan50chebiIds.add("CHEBI:27314");
        lessThan50chebiIds.add("CHEBI:65383");
        lessThan50chebiIds.add("CHEBI:65384");
        lessThan50chebiIds.add("CHEBI:65382");
        lessThan50chebiIds.add("CHEBI:65385");
        lessThan50chebiIds.add("CHEBI:18219");
        lessThan50chebiIds.add("CHEBI:53502");
        lessThan50chebiIds.add("CHEBI:53503");
        lessThan50chebiIds.add("CHEBI:53504");
        lessThan50chebiIds.add("CHEBI:53542");
        lessThan50chebiIds.add("CHEBI:53667");
        lessThan50chebiIds.add("CHEBI:31520");
        lessThan50chebiIds.add("CHEBI:71015");
        lessThan50chebiIds.add("CHEBI:29375");
        lessThan50chebiIds.add("CHEBI:31642");
        lessThan50chebiIds.add("CHEBI:29374");
        lessThan50chebiIds.add("CHEBI:41981");
        lessThan50chebiIds.add("CHEBI:33811");
        lessThan50chebiIds.add("CHEBI:33806");
        lessThan50chebiIds.add("CHEBI:31822");
        lessThan50chebiIds.add("CHEBI:35511");
        lessThan50chebiIds.add("CHEBI:3566");
        lessThan50chebiIds.add("CHEBI:4496");
        lessThan50chebiIds.add("CHEBI:30115");
        lessThan50chebiIds.add("CHEBI:31795");
        lessThan50chebiIds.add("CHEBI:32138");
        lessThan50chebiIds.add("CHEBI:32142");
        lessThan50chebiIds.add("CHEBI:32150");
        lessThan50chebiIds.add("CHEBI:32583");
        lessThan50chebiIds.add("CHEBI:32584");     //32

        return lessThan50chebiIds;

    }

    private static List<String> createSecondaryChebiIds() {

        List<String> secondaryChebiIds = new ArrayList<String>();
        //Water secondary acs
        secondaryChebiIds.add("CHEBI:27313");
        secondaryChebiIds.add("CHEBI:727419");
        secondaryChebiIds.add("CHEBI:44701");
        secondaryChebiIds.add("CHEBI:44819");
        secondaryChebiIds.add("CHEBI:42857");
        secondaryChebiIds.add("CHEBI:44292");
        secondaryChebiIds.add("CHEBI:43228");
        secondaryChebiIds.add("CHEBI:42043");
        secondaryChebiIds.add("CHEBI:5585");
        secondaryChebiIds.add("CHEBI:10743");
        secondaryChebiIds.add("CHEBI:13352");

        return secondaryChebiIds;
    }

    private static List<String> createRepeatedChebiIds() {

        List<String> repeatedChebiIds = new ArrayList<String>();

        //Water
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");
        repeatedChebiIds.add("CHEBI:15377");

        return repeatedChebiIds;

    }


}
