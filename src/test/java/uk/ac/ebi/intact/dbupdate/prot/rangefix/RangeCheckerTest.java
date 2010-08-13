/**
 * Copyright 2008 The European Bioinformatics Institute, and others.
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *     http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */
package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import org.junit.After;
import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.model.CvFuzzyType;
import uk.ac.ebi.intact.model.Feature;
import uk.ac.ebi.intact.model.Range;

import java.util.Collection;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeCheckerTest extends IntactBasicTestCase {

    private RangeChecker rangeChecker;

    @Before
    public void before() throws Exception {
        rangeChecker = new RangeChecker();
    }

    @After
    public void after() throws Exception {
        rangeChecker = null;
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_noChanges() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals("BCD", rangeSeq);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());

        Assert.assertTrue(updatedRanges.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_substitutionWithinRange() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        Assert.assertTrue(range.getSequence().startsWith("BCD"));

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());

        Assert.assertTrue(updatedRanges.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        Assert.assertTrue(range.getSequence().startsWith("BCD"));

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertNotSame(rangeAfterFix, rangeSeq);
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_upstreamInsertion() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ZABCDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(feature);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());

        Assert.assertEquals(1, updatedRanges.size());

        Assert.assertEquals(3, range.getFromIntervalStart());
        Assert.assertEquals(3, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_insertionDownstream() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABCDEFZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        String rangeSeq = oldSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());

        Assert.assertTrue(updatedRanges.isEmpty());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());

        String rangeAfterFix = newSequence.substring(range.getFromIntervalStart()-1, range.getToIntervalEnd());
        Assert.assertEquals(rangeAfterFix, rangeSeq);
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_differentSeq() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ZZAZZDZFEZZZZZZ";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 4, 4);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(4, range.getToIntervalStart());
        Assert.assertEquals(4, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_substitutionInExactPosition() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "ABZDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(3, 3, 3, 3);
        range.prepareSequence(oldSequence);
        feature.addRange(range);
        Assert.assertEquals('C',range.getSequence().charAt(0));

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(3, range.getFromIntervalStart());
        Assert.assertEquals(3, range.getFromIntervalEnd());
        Assert.assertEquals(3, range.getToIntervalStart());
        Assert.assertEquals(3, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_undeterminedRange() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setUndetermined(true);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_undeterminedRange_reset() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(2, 2, 3, 3);
        range.setUndetermined(true);
        range.prepareSequence(oldSequence);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(3, range.getToIntervalStart());
        Assert.assertEquals(3, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_cTerminal_from() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_MI_REF, CvFuzzyType.C_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_cTerminal_to() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.C_TERMINAL_MI_REF, CvFuzzyType.C_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setToCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_nTerminal_from() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_MI_REF, CvFuzzyType.N_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setFromCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_nTerminal_to() throws Exception {
        String oldSequence = "ABCDEF";
        String newSequence = "CDEF";

        CvFuzzyType fuzzyType = getMockBuilder().createCvObject(CvFuzzyType.class, CvFuzzyType.N_TERMINAL_MI_REF, CvFuzzyType.N_TERMINAL);

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(0, 0, 0, 0);
        range.setToCvFuzzyType(fuzzyType);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_toOutOfBounds() throws Exception {
        String oldSequence = "MDNCLAAAALNGVDRRSLQRSAKLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";
        String newSequence = "MDNCLAAAALNGVDRRSLQRSARLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 220, 220);
        feature.addRange(range);


        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(1, range.getFromIntervalStart());
        Assert.assertEquals(1, range.getFromIntervalEnd());
        Assert.assertEquals(220, range.getToIntervalStart());
        Assert.assertEquals(220, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_toOutOfBounds2() throws Exception {
        String oldSequence = "MDNCLAAAAL";
        String newSequence = "MDNCLAAAALNGVDRRSLQRSARLALEVLERAKRRAVDWHALERPKGCMGVLAREAPHLEKQPAAGPQRVLPGEREERPP" +
                "TLSASFRTMAEFMDYTSSQCGKYYSSVPEEGGATHVYRYHRGESKLHMCLDIGNGQRKDRKKTSLGPGGSYQISEHAPEA" +
                "SQPAENISKDLYIEVYPGTYSVTVGSNDLTKKTHVVAVDSGQSVDLVFPV";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 40, 40);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(1, range.getFromIntervalStart());
        Assert.assertEquals(1, range.getFromIntervalEnd());
        Assert.assertEquals(40, range.getToIntervalStart());
        Assert.assertEquals(40, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_toOutOfBounds3() throws Exception {
        String oldSequence = "MDNCLAAAAL";
        String newSequence = "MDNCLA";

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(1, 1, 40, 40);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(1, range.getFromIntervalStart());
        Assert.assertEquals(1, range.getFromIntervalEnd());
        Assert.assertEquals(40, range.getToIntervalStart());
        Assert.assertEquals(40, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void shiftFeatureRanges_toOutOfBounds4() throws Exception {
        String oldSequence = "MDNCLAHAHAHAHAHAAAAL";
        String newSequence = "MDNCLA";

        Assert.assertEquals(20, oldSequence.length());
        Assert.assertEquals(6, newSequence.length());

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range range = getMockBuilder().createRange(20, 20, 20, 20);
        feature.addRange(range);

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        Assert.assertEquals(20, range.getFromIntervalStart());
        Assert.assertEquals(20, range.getFromIntervalEnd());
        Assert.assertEquals(20, range.getToIntervalStart());
        Assert.assertEquals(20, range.getToIntervalEnd());
    }

    @Test @DirtiesContext
    public void isSequenceChanged_yes3() throws Exception {
        String oldSequence = "MALKSINISGNFEWCPFEEYKNYLLCFNSHNLLYSNNNSLNNYIYLLDINLNSEIRNLEIVNKYNFEDALKYDNDVIKGG" +
                "NKKNNKNNKNNHNNNSVNEYVTCFEWMNSNNFVDINNNEELSKGIIVGGLTNGDIVLLNAKNLFETNRNYDNFILSKTNI" +
                "HDNGINCLEYNRHKNNLIATGGNDGQLFITDIENLYSPTSYDPYLDKNNLQKITCLNWNKKVSHILATSSNNGNTVIWDL" +
                "KIKKSAVSFRDPHSRTKTSSLSWLSNQPTQVLISYDDDKNPCLQLWDLRNSNYPIKEIIGHSKGINNICFSPIDTNLLLS" +
                "SGKDVTKCWYLDNNNFDIFNEINNSANNIYSKWSPYIPDLFASSTNMDTIQINSINNGNKMTSKYIPTFYKKEAGICIGF" +
                "GGKICTFDNSTNNMSNVNNMNNVNNMNNINSFNNDNSCDGEYDSNKGKNKSTQKKFLIKYHIYPTDMELISEADNFEKYI" +
                "TSGNYKEFCESKINKCDDDHEKLTWQILQLLCTSQRGDIVKYLGHDINNIVDKIMQTIGKQPGFIFKTLIDEKENNNNNN" +
                "NNNSTNQMYQNDVLLHNDPNLMNNYLLKDNMNPNIMLNNNNNNINNRTGTNVMYSNGQNLLGDTNHNEENFNGNFDIDPE" +
                "KFFRELGEKTENEKIKQNEEDISGNDEHLLNSSIKGKENKTKNKKSGLGTDDNNDNGDHNKNEGSNINGEHVSEHILNEK" +
                "NNTNNWNLGIEAIIKECVLIGNIETAVELCLHKNRMADALLLSSFGGEQLWHKTKTIYIKKQNDNFLKNINYVLDDKLEN" +
                "LINNVDLNSWEEALSILCTYAINNPNFNSLCEMLAKRLQNEKFDIRAASICYLCACNFSETVEIWNNMPSKKTSLLNVLQ" +
                "DIVEKMTILKMIIKYENFNSIMNQKISQYAELLANSGRLKAAMTFLCLIQHDQSIESLILRDRIYNSANHVLCQQIKPPI" +
                "SPFQIVDIKPSPNVYQNNMYNNNNNNNNININSSSNNNNNNNNNKVLSSMHHPMQQFNQCNVNKMYTSTSNIINNNTMNS" +
                "NFKSVIPPPLPMNTQMNNSTSSIQPPPSVPPTKFHTQIINNTMNSRSSIATTTKNYPTSNLNSVIPTSMNNMNTNISHGN" +
                "NVTPPYMSQTNVAVPNMNNNNNNNNTMNPTYPSLPKFPNYNLNSQVQQNSIIPEKQLTSPMFSSNSYGNINKTHTTNNAV" +
                "PPPPNVTSSVVTPPMPSNQLNNTRSSFADIQNVVSPPRNKNQSISSTANLNYQHDNQFNKRECMEQPVYPMTNQSSMFSM" +
                "NNTMQKKNVPGGFQDNTSQMNYGMQPTGSPPPSSLSTTSPIAGALTVTPGMPVPWPIPTTTQQLGSTTQSTANENKKIQT" +
                "ATKEQNGVLMNRNHIENIKKTISNLLNIYTSQESVKKKADDVSSKVYELFEKLDCGAFNEQINDSLLNLVNCINANDFKT" +
                "TNKIIVDLSRNLWDGSNKAW";
        String newSequence = "MALKSINISGNFEWCPFEEYKNYLLCFNSHNLLYSNNNSLNNYIYLLDINLNSEIRNLEIVNKYNFEDALKYDNDVIKGG" +
                "NKKNNKNNKNNHNNNSVNEYVTCFEWMNSNNFVDINNNEELSKGIIVGGLTNGDIVLLNAKNLFETNRNYDNFILSKTNI" +
                "HDNGINCLEYNRHKNNLIATGGNDGQLFITDIENLYSPTSYDPYLDKNNLQKITCLNWNKKVSHILATSSNNGNTVIWDL" +
                "KIKKSAVSFRDPHSRTKTSSLSWLSNQPTQVLISYDDDKNPCLQLWDLRNSNYPIKEIIGHSKGINNICFSPIDTNLLLS" +
                "SGKDVTKCWYLDNNNFDIFNEINNSANNIYSKWSPYIPDLFASSTNMDTIQINSINNGNKMTSKYIPTFYKKEAGICIGF" +
                "GGKICTFDNSTNNMSNVNNMNNVNNMNNINSFNNDNSCDGEYDSNKGKNKSTQKKFLIKYHIYPTDMELISEADNFEKYI" +
                "TSGNYKEFCESKINKCDDDHEKLTWQILQLLCTSQRGDIVKYLGHDINNIVDKIMQTIGKQPGFIFKTLIDEKENNNNNN" +
                "NNNSTNQMYQNDVLLHNDPNLMNNYLLKDNMNPNIMLNNNNNNINNRTGTNVMYSNGQNLLGDTNHNEENFNGNFDIDPE" +
                "KFFRELGEKTENEKIKQNEEDISGNDEHLLNSSIKGKENKTKNKKSGLGTDDNNDNGDHNKNEGSNINGEHVSEHILNEK" +
                "NNTNNWNLGIEAIIKECVLIGNIETAVELCLHKNRMADALLLSSFGGEQLWHKTKTIYIKKQNDNFLKNINYVLDDKLEN" +
                "LINNVDLNSWEEALSILCTYAINNPNFNSLCEMLAKRLQNEKFDIRAASICYLCACNFSETVEIWNNMPSKKTSLLNVLQ" +
                "DIVEKMTILKMIIKYENFNSIMNQKISQYAELLANSGRLKAAMTFLCLIQHDQSIESLILRDRIYNSANHVLCQQIKPPI" +
                "SPFQIVDIKPSPNVYQNNMYNNNNNNNNININSSSNNNNNNNNNKVLSSMHHPMQQFNQCNVNKMYTSTSNIINNNTMNS" +
                "NFKSVIPPPLPMNTQMNNSTSSIQPPPSVPPTKFHTQIINNTMNSRSSIATTTKNYPTSNLNSVIPTSMNNMNTNISHGN" +
                "NVTPPYMSQTNVAVPNMNNNNNNNNTMNPTYPSLPKFPNYNLNSQVQQNSIIPEKQLTSPMFSSNSYGNINKTHTTNNAV" +
                "PPPPNVTSSVVTPPMPSNQLNNTRSSFADIQNVVSPPRNKNQSISSTANLNYQHDNQFNKRECMEQPVYPMTNQSSMFSM" +
                "NNTMQKKNVPGGFQDNTSQMNYGMQPTGSPPPSSLSTTSPIAGALTVTPGMPVPWPIPTTTQQLGSTTQSTANENKKIQT" +
                "ATKEQNGVLMNRNHIENIKKTISNLLNIYTSQESVKKKADDVSSKVYELFEKLDCGAFNEQINDSLLNLVNCINANDFKT" +
                "TNKIIVDLSRNLWDGSNKAWIMGVKHIIPKC";

        Assert.assertEquals(1460, oldSequence.length());
        Assert.assertEquals(1471, newSequence.length());

        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();

        Range oldRange = getMockBuilder().createRange(1300, 1300, 1460, 1460);
        oldRange.prepareSequence(oldSequence);
        feature.addRange(oldRange);

        Assert.assertNotNull(oldRange.getFullSequence());
        Assert.assertNotNull(oldRange.getSequence());

        String oldSeq = oldRange.getFullSequence();

        final Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence, new ProteinUpdateProcessor());
        Assert.assertEquals(0, updatedRanges.size());

        String newSeq = oldRange.getFullSequence();

        //Assert.assertEquals(oldSeq, newSeq);

        Assert.assertEquals(1300, oldRange.getFromIntervalStart());
        Assert.assertEquals(1300, oldRange.getFromIntervalEnd());
        Assert.assertEquals(1460, oldRange.getToIntervalStart());
        Assert.assertEquals(1460, oldRange.getToIntervalEnd());
    }
}
