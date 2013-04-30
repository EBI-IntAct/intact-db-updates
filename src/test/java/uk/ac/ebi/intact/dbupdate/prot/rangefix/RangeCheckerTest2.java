package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.CvFuzzyType;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Collection;

/**
 * Second tester of RangeChecker
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>21-Oct-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class RangeCheckerTest2 extends IntactBasicTestCase {

    private RangeChecker rangeChecker;

    @Before
    public void before() throws Exception {
        rangeChecker = new RangeChecker();

        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        rangeChecker = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with undetermined range : is valid
     */
    public void valid_undeterminedRange() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setUndetermined(true);

        CvFuzzyType undetermined = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.UNDETERMINED_MI_REF, CvFuzzyType.UNDETERMINED);

        range.setFromCvFuzzyType(undetermined);
        range.setToCvFuzzyType(undetermined);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with 'n-n' ranges : is valid
     */
    public void valid_nTerminalRegion() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_REGION_MI_REF, CvFuzzyType.N_TERMINAL_REGION);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with 'n-?' range : is valid
     */
    public void valid_nTerminalRegion_undetermined() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_REGION_MI_REF, CvFuzzyType.N_TERMINAL_REGION);
        CvFuzzyType fuzzyType2 = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.UNDETERMINED_MI_REF, CvFuzzyType.UNDETERMINED);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        range.setToCvFuzzyType(fuzzyType2);
        feature.addRange(range);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with '?-c' range : is valid
     */
    public void valid_undetermined_cTerminalRegion() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.UNDETERMINED_MI_REF, CvFuzzyType.UNDETERMINED);
        CvFuzzyType fuzzyType2 = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_REGION_MI_REF, CvFuzzyType.C_TERMINAL_REGION);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        range.setToCvFuzzyType(fuzzyType2);

        feature.addRange(range);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with 'c-c' range : is valid
     */
    public void valid_cTerminalRegion() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_REGION_MI_REF, CvFuzzyType.C_TERMINAL_REGION);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setToCvFuzzyType(fuzzyType);
        range.setFromCvFuzzyType(fuzzyType);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is still valid because the new sequence is identical to the previous sequence
     */
    public void check_valid_range_noChanges() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals("BCD", rangeSeq);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);
        Assert.assertEquals(0, invalidRanges.size());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with invalid range : cannot prepare the sequence
     */
    public void prepare_feature_sequence_invalid_range() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String true_sequence = "MQITMMAVAAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
                "DDSLEDCIVQAQEHIPSEQDVELQDEHANLENLPLEEYVPVEEDVEFESVEQEQSESQSQ" +
                "EPEGNQQPTKNDYGLTEDEILLANAASESSDAEAAMQSAALVVRLKEGISSLGRILKAIE" +
                "TFHGTVQHVESRQSRVEGVDHDVLIKLDMTRGNLLQLIRSLRQSGSFSSMNLMADNNLNV" +
                "KAPWFPKHASELDNCNHLMTKYEPDLDMNHPGFADKVYRQRRKEIAEIAFAYKYGDPIPF" +
                "IDYSDVEVKTWRSVFKTVQDLAPKHACAEYRAAFQKLQDEQIFVETRLPQLQEMSDFLRK" +
                "NTGFSLRPAAGLLTARDFLASLAFRIFQSTQYVRHVNSPYHTPEPDSIHELLGHMPLLAD" +
                "PSFAQFSQEIGLASLGASDEEIEKLSTVYWFTVEFGLCKEHGQIKAYGAGLLSSYGELLH" +
                "AISDKCEHRAFEPASTAVQPYQDQEYQPIYYVAESFEDAKDKFRRWVSTMSRPFEVRFNP" +
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPFPAVQ";


        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(0,0,2,2);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(null, range.getFullSequence());

        // Update the ranges
        Collection<UpdatedRange> updatedRanges = rangeChecker.prepareFeatureSequences(feature, true_sequence, IntactContext.getCurrentInstance().getDataContext());
        Assert.assertEquals(0, updatedRanges.size());

        // the sequence of the feature has not been set
        Assert.assertEquals(null, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because of a substitution within the feature
     */
    public void impossible_to_Shift_substitutionWithinRange() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertTrue(range.getFullSequence().equals("BCD"));

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("2..2-4..4", invalid.getNewRange().toString());
        Assert.assertEquals(newSequence, invalid.getSequence().toString());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because of a totally different sequence
     */
    public void impossible_to_shift_differentSeq() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ZZAZZDZFEZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("4..4-6..6", invalid.getNewRange().toString());
        Assert.assertEquals(newSequence, invalid.getSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because of a substitution of the feature
     */
    public void impossible_to_shift_substitutionInExactPosition() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(3, 3, 3, 3);
        range.prepareSequence(oldSequence);
        feature.addRange(range);
        Assert.assertEquals('C',range.getSequence().charAt(0));
        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("0..0-0..0", invalid.getNewRange().toString());
        Assert.assertEquals(newSequence, invalid.getSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because of a deletion within the feature
     */
    public void impossible_to_shift_deletion_feature() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 3, 3);
        range.setUndetermined(true);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("0..0-1..1", invalid.getNewRange().toString());
        Assert.assertEquals(newSequence, invalid.getSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with invalid range : cannot be updated because it is invalid with the previous sequence
     */
    public void impossible_to_shift_OutOfBounds_oldSequence() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "MDNCLAAAAL";
        String newSequence = "MDNCLAAAALNGVDRRSLQRSARLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 100, 100);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);


        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals(null, invalid.getNewRange());
        Assert.assertEquals(oldSequence, invalid.getSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because it becomes out of bounds
     */
    public void impossible_to_shift_toOutOfBounds_newSequence() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "MDNCLAAAAL";
        String newSequence = "MDNCLA";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(6, 6, 9, 9);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, oldSequence, newSequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("6..6-0..0", invalid.getNewRange().toString());
        Assert.assertEquals(newSequence, invalid.getSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Feature with valid range : is not valid with the new sequence because of a difference in the feature sequences
     */
    public void impossible_to_shift_different_feature_sequence() throws Exception {

        TransactionStatus status = getDataContext().beginTransaction();


        String oldFeatureSequence = "BQKN";
        //String newFeatureSequence = "AQKN";

        String previousSequence = "MMAVABQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
                "DDSLEDCIVQAQEHIPSEQDVELQDEHANLENLPLEEYVPVEEDVEFESVEQEQSESQSQ" +
                "EPEGNQQPTKNDYGLTEDEILLANAASESSDAEAAMQSAALVVRLKEGISSLGRILKAIE" +
                "TFHGTVQHVESRQSRVEGVDHDVLIKLDMTRGNLLQLIRSLRQSGSFSSMNLMADNNLNV" +
                "KAPWFPKHASELDNCNHLMTKYEPDLDPHNMGFADKVYRQRRKEIAEIAFAYKYGDPIPF" +
                "IDYSDVEVKTWRSVFKTVQDLAPKHACAEYRAAFQKLQDEQIFVETRLPQLQEMSDFLRK" +
                "NTGFSLRPAAGLLTARDFLASLAFRIFQSTQYVRHVNSPYHTPEPDSIHELLGHMPLLAD" +
                "PSFAQFSQEIGLASLGASDEEIEKLSTVYWFTVEFGLCKEHGQIKAYGAGLLSSYGELLH" +
                "AISDKCEHRAFEPASTAVQPYQDQEYQPIYYVAESFEDAKDKFRRWVSTMSRPFEVRFNP" +
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPF";

        String true_sequence = "MMAVAAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
                "DDSLEDCIVQAQEHIPSEQDVELQDEHANLENLPLEEYVPVEEDVEFESVEQEQSESQSQ" +
                "EPEGNQQPTKNDYGLTEDEILLANAASESSDAEAAMQSAALVVRLKEGISSLGRILKAIE" +
                "TFHGTVQHVESRQSRVEGVDHDVLIKLDMTRGNLLQLIRSLRQSGSFSSMNLMADNNLNV" +
                "KAPWFPKHASELDNCNHLMTKYEPDLDMNHPGFADKVYRQRRKEIAEIAFAYKYGDPIPF" +
                "IDYSDVEVKTWRSVFKTVQDLAPKHACAEYRAAFQKLQDEQIFVETRLPQLQEMSDFLRK" +
                "NTGFSLRPAAGLLTARDFLASLAFRIFQSTQYVRHVNSPYHTPEPDSIHELLGHMPLLAD" +
                "PSFAQFSQEIGLASLGASDEEIEKLSTVYWFTVEFGLCKEHGQIKAYGAGLLSSYGELLH" +
                "AISDKCEHRAFEPASTAVQPYQDQEYQPIYYVAESFEDAKDKFRRWVSTMSRPFEVRFNP" +
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPF";


        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(6,6,9,9);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        // collect invalid ranges
        final Collection<InvalidRange> invalidRanges = rangeChecker.collectRangesImpossibleToShift(feature, previousSequence, true_sequence);

        Assert.assertEquals(1, invalidRanges.size());

        InvalidRange invalid = invalidRanges.iterator().next();
        Assert.assertEquals(range.getAc(), invalid.getRangeAc());
        Assert.assertEquals("7..7-10..10", invalid.getNewRange().toString());
        Assert.assertEquals(true_sequence, invalid.getSequence());

        getDataContext().commitTransaction(status);

    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range should be updated : ranges are not shifted because the protein sequence was null but as the range is still valid with the
     * uniprot sequence, only the feature sequence will be updated
     */
    public void prepare_feature_sequence_valid_range() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // int oldSequenceLength = 579;

        String featureSequence = "MQITMM";

        String true_sequence = "MQITMMAVAAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
                "DDSLEDCIVQAQEHIPSEQDVELQDEHANLENLPLEEYVPVEEDVEFESVEQEQSESQSQ" +
                "EPEGNQQPTKNDYGLTEDEILLANAASESSDAEAAMQSAALVVRLKEGISSLGRILKAIE" +
                "TFHGTVQHVESRQSRVEGVDHDVLIKLDMTRGNLLQLIRSLRQSGSFSSMNLMADNNLNV" +
                "KAPWFPKHASELDNCNHLMTKYEPDLDMNHPGFADKVYRQRRKEIAEIAFAYKYGDPIPF" +
                "IDYSDVEVKTWRSVFKTVQDLAPKHACAEYRAAFQKLQDEQIFVETRLPQLQEMSDFLRK" +
                "NTGFSLRPAAGLLTARDFLASLAFRIFQSTQYVRHVNSPYHTPEPDSIHELLGHMPLLAD" +
                "PSFAQFSQEIGLASLGASDEEIEKLSTVYWFTVEFGLCKEHGQIKAYGAGLLSSYGELLH" +
                "AISDKCEHRAFEPASTAVQPYQDQEYQPIYYVAESFEDAKDKFRRWVSTMSRPFEVRFNP" +
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPFPAVQ";


        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(1,1,6,6);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(null, range.getFullSequence());

        // Update the ranges
        Collection<UpdatedRange> updatedRanges = rangeChecker.prepareFeatureSequences(feature, true_sequence, IntactContext.getCurrentInstance().getDataContext());
        Assert.assertEquals(1, updatedRanges.size());
        Assert.assertEquals(range.getAc(), updatedRanges.iterator().next().getNewRange().getAc());

        // the sequence of the feature has been set properly
        Assert.assertEquals(featureSequence, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }
}
