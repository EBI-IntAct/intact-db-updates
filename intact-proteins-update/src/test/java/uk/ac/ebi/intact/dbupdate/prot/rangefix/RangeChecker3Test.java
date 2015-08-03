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
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.CvTopic;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Collection;

/**
 * Third tester of RangeChecker
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>08-Dec-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class RangeChecker3Test extends IntactBasicTestCase {

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
     * One feature with valid range should be updated : ranges shifted properly (insertions upstream feature)
     */
    public void update_valid_range_upstreamInsertion() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ZABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(1, report.getShiftedRanges().size());

        Assert.assertEquals(3, range.getFromIntervalStart());
        Assert.assertEquals(3, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range should be updated : ranges not shifted because feature is not touched by the changes (insertions downstream feature)
     */
    public void update_valid_range_insertionDownstream() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEFZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(0, report.getShiftedRanges().size()); // not shifted because it is only the downstream sequence which is updated.

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range should be updated : ranges shifted properly (insertions of the same amino acid as the first in the feature)
     */
    public void update_valid_range_insertion_afterStart() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // int oldSequenceLength = 578;

        String oldFeatureSequence = "AQKN";
        // newFeatureSequence = "AAQKN" -> "AQKN";

        String previousSequence = "MMAVAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
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

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        // Update the ranges
        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, previousSequence, true_sequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(1, report.getShiftedRanges().size());

        Collection<UpdatedRange> updatedRanges = report.getShiftedRanges();
        Assert.assertNotNull(updatedRanges);
        Assert.assertEquals(1, updatedRanges.size());
        Assert.assertEquals(range.getAc(), updatedRanges.iterator().next().getNewRange().getAc());
        Assert.assertFalse(hasAnnotation(feature, null, CvTopic.CAUTION));

        // the ranges have been shifted
        Assert.assertEquals(7, range.getFromIntervalStart());
        Assert.assertEquals(7, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range should be updated : ranges shifted properly (deletion and then insertion of the same amino acid as the first in the feature)
     */
    public void update_valid_range_deletion_start() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldFeatureSequence = "AQKN";
        // newFeatureSequence = "?QKN" -> "AQKN";

        String previousSequence = "MMAVAAAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
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
        Range range = getMockBuilder().createRange(8,8,11,11);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        // Update the ranges
        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, previousSequence, true_sequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(1, report.getShiftedRanges().size());

        Collection<UpdatedRange> updatedRanges = report.getShiftedRanges();
        Assert.assertNotNull(updatedRanges);
        Assert.assertEquals(1, updatedRanges.size());
        Assert.assertEquals(range.getAc(), updatedRanges.iterator().next().getNewRange().getAc());
        Assert.assertFalse(hasAnnotation(feature, null, CvTopic.CAUTION));

        // the ranges have been shifted
        Assert.assertEquals(7, range.getFromIntervalStart());
        Assert.assertEquals(7, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range at the end of the sequence should be updated : ranges shifted properly
     * and a caution is added to the feature because it is not c-terminal anymore
     */
    public void update_valid_range_end_sequence() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        // int oldSequenceLength = 579;

        String oldFeatureSequence = "KLRRPF";
        // newFeatureSequence = "KLRRPF", not at the end of the sequence anymore

        String previousSequence = "MMAVAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
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
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPFPAVQ";


        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(573,573,578,578);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        // Update the ranges
        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, previousSequence, true_sequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(1, report.getShiftedRanges().size());

        Collection<UpdatedRange> updatedRanges = report.getShiftedRanges();
        Assert.assertNotNull(updatedRanges);
        Assert.assertEquals(1, updatedRanges.size());
        Assert.assertEquals(range.getAc(), updatedRanges.iterator().next().getNewRange().getAc());
        Assert.assertTrue(hasAnnotation(feature, null, CvTopic.CAUTION));

        // the ranges have been shifted
        Assert.assertEquals(574, range.getFromIntervalStart());
        Assert.assertEquals(574, range.getFromIntervalEnd());
        Assert.assertEquals(579, range.getToIntervalStart());
        Assert.assertEquals(579, range.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One feature with valid range at the beginning of the sequence should be updated : ranges shifted properly
     * and a caution is added to the feature because it is not n-terminal anymore
     */
    public void update_valid_range_beginning_sequence() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        String oldFeatureSequence = "MMAVAA";

        String previousSequence = "MMAVAAQKNREMFAIKKSYSIENGYPSRRRSLVDDARFETLVVKQTKQTVLEEARSKAN" +
                "DDSLEDCIVQAQEHIPSEQDVELQDEHANLENLPLEEYVPVEEDVEFESVEQEQSESQSQ" +
                "EPEGNQQPTKNDYGLTEDEILLANAASESSDAEAAMQSAALVVRLKEGISSLGRILKAIE" +
                "TFHGTVQHVESRQSRVEGVDHDVLIKLDMTRGNLLQLIRSLRQSGSFSSMNLMADNNLNV" +
                "KAPWFPKHASELDNCNHLMTKYEPDLDPHNMGFADKVYRQRRKEIAEIAFAYKYGDPIPF" +
                "IDYSDVEVKTWRSVFKTVQDLAPKHACAEYRAAFQKLQDEQIFVETRLPQLQEMSDFLRK" +
                "NTGFSLRPAAGLLTARDFLASLAFRIFQSTQYVRHVNSPYHTPEPDSIHELLGHMPLLAD" +
                "PSFAQFSQEIGLASLGASDEEIEKLSTVYWFTVEFGLCKEHGQIKAYGAGLLSSYGELLH" +
                "AISDKCEHRAFEPASTAVQPYQDQEYQPIYYVAESFEDAKDKFRRWVSTMSRPFEVRFNP" +
                "HTERVEVLDSVDKLETLVHQMNTEILHLTNAISKLRRPF";

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
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        getCorePersister().saveOrUpdate(feature);

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        // Update the ranges
        RangeUpdateReport report = new RangeUpdateReport();

        rangeChecker.shiftFeatureRanges(feature, previousSequence, true_sequence, IntactContext.getCurrentInstance().getDataContext(), report);
        Assert.assertEquals(1, report.getShiftedRanges().size());

        Collection<UpdatedRange> updatedRanges = report.getShiftedRanges();
        Assert.assertNotNull(updatedRanges);
        Assert.assertEquals(1, updatedRanges.size());
        Assert.assertEquals(range.getAc(), updatedRanges.iterator().next().getNewRange().getAc());
        Assert.assertTrue(hasAnnotation(feature, null, CvTopic.CAUTION));

        // the ranges have been shifted
        Assert.assertEquals(5, range.getFromIntervalStart());
        Assert.assertEquals(5, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, range.getFullSequence());

        getDataContext().commitTransaction(status);
    }

    private boolean hasAnnotation( Feature f, String text, String cvTopic) {
        final Collection<Annotation> annotations = f.getAnnotations();
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
