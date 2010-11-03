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
package uk.ac.ebi.intact.dbupdate.prot;

import org.junit.Assert;
import org.junit.Before;
import org.junit.Test;
import org.springframework.test.annotation.DirtiesContext;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.transaction.TransactionStatus;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import uk.ac.ebi.intact.core.config.CvPrimer;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;

import java.util.Collection;
import java.util.Collections;
import java.util.Iterator;

/**
 * ProteinUpdateProcessor Tester.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/jpa.test.spring.xml"} )
@DirtiesContext(classMode = DirtiesContext.ClassMode.AFTER_EACH_TEST_METHOD)
public class ProteinUpdateProcessor2Test extends IntactBasicTestCase {

    @Before
    public void before_schema() throws Exception {
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }


    @Test
    @DirtiesContext
    public void deadUniprotProtein() throws Exception {

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        // interaction: no
        Protein deadProtein = getMockBuilder().createProtein("Pxxxxx", "dead protein");
        deadProtein.getBioSource().setTaxId( "7227" );
        deadProtein.getBioSource().setShortLabel( "drome" );
        deadProtein.getAliases().clear();

        getCorePersister().saveOrUpdate(deadProtein);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        Interaction interaction = getMockBuilder().createInteraction( deadProtein );

        getCorePersister().saveOrUpdate(deadProtein, interaction);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Protein reloadedMaster = getDaoFactory().getProteinDao().getByAc( deadProtein.getAc() );

        assertHasXref(reloadedMaster, CvDatabase.UNIPROT, CvXrefQualifier.UNIPROT_REMOVED_AC, "Pxxxxx");

        boolean hasNoUniprotUpdate = false;
        boolean hasCaution = false;

        Assert.assertEquals(2, reloadedMaster.getAnnotations().size());
        for (Annotation a : reloadedMaster.getAnnotations()){
            if (CvTopic.CAUTION_MI_REF.equals(a.getCvTopic().getIdentifier())){
                hasCaution = true;
            }
            else if (CvTopic.NON_UNIPROT.equals(a.getCvTopic().getShortLabel())){
                hasNoUniprotUpdate = true;
            }
        }

        Assert.assertTrue(hasNoUniprotUpdate);
        Assert.assertTrue(hasCaution);
    }

    @Test
    @DirtiesContext
    public void deadUniprotProtein_otherXRefs() throws Exception {
        CvPrimer cvPrimer = new ComprehensiveCvPrimer(getDaoFactory());
        cvPrimer.createCVs();

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        // interaction: no
        Protein deadProtein = getMockBuilder().createProtein("Pxxxxx", "dead protein");
        deadProtein.getBioSource().setTaxId( "7227" );
        deadProtein.getBioSource().setShortLabel( "drome" );
        deadProtein.getAliases().clear();

        deadProtein.addXref(getMockBuilder().createXref(deadProtein, "xxxx1", null, getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.UNIPROT_MI_REF, CvDatabase.UNIPROT)));
        deadProtein.addXref(getMockBuilder().createXref(deadProtein, "xxxx2", null, getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.CHEBI_MI_REF, CvDatabase.CHEBI)));
        deadProtein.addXref(getMockBuilder().createXref(deadProtein, "xxxx3", getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.CHAIN_PARENT_MI_REF, CvXrefQualifier.CHAIN_PARENT), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT)));

        getCorePersister().saveOrUpdate(deadProtein);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(4, deadProtein.getXrefs().size());

        Interaction interaction = getMockBuilder().createInteraction( deadProtein );

        getCorePersister().saveOrUpdate(deadProtein, interaction);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getProteinDao().countUniprotProteinsInvolvedInInteractions(), 0);
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Protein reloadedMaster = getDaoFactory().getProteinDao().getByAc( deadProtein.getAc() );

        Assert.assertEquals(2, reloadedMaster.getXrefs().size());
        assertHasXref(reloadedMaster, CvDatabase.UNIPROT, CvXrefQualifier.UNIPROT_REMOVED_AC, "Pxxxxx");
        assertHasXref(reloadedMaster, CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT, "xxxx3");

        boolean hasNoUniprotUpdate = false;
        boolean hasCaution = false;

        Assert.assertEquals(2, reloadedMaster.getAnnotations().size());
        for (Annotation a : reloadedMaster.getAnnotations()){
            if (CvTopic.CAUTION_MI_REF.equals(a.getCvTopic().getIdentifier())){
                hasCaution = true;
            }
            else if (CvTopic.NON_UNIPROT.equals(a.getCvTopic().getShortLabel())){
                hasNoUniprotUpdate = true;
            }
        }

        Assert.assertTrue(hasNoUniprotUpdate);
        Assert.assertTrue(hasCaution);
    }

    @Test
    @DirtiesContext
    public void range_shifting_OutOfBoundBeforeUpdate() throws Exception {

        // check that splice variants do get gene names like the masters do.

        // http://www.uniprot.org/uniprot/P18459

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");

        int oldSequenceLength = 579;

        String oldFeatureSequence = "AQKN";
        String newFeatureSequence = "AQKN";

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

        // interaction: no
        Protein prot = getMockBuilder().createProtein("P18459", "prot");
        prot.getBioSource().setTaxId( "7227" );
        prot.getBioSource().setShortLabel( "drome" );
        prot.getAliases().clear();
        prot.setSequence(previousSequence);

        getCorePersister().saveOrUpdate(prot);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component c = interaction.getComponents().iterator().next();
        c.getBindingDomains().clear();

        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(450,450,600,600);
        feature.getRanges().clear();
        feature.addRange(range);

        c.addBindingDomain(feature);

        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getComponentDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsBefore.size());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Feature reloadedFeature = getDaoFactory().getFeatureDao().getByAc( feature.getAc() );

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(reloadedFeature, Collections.singleton(invalid_range));
        Assert.assertEquals(1, cautionsAfter.size());

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Protein reloadedProtein = getDaoFactory().getProteinDao().getByAc( prot.getAc() );
        Assert.assertEquals(true_sequence, reloadedProtein.getSequence());
    }

    @Test
    @DirtiesContext
    public void range_shifting_update() throws Exception {

        // http://www.uniprot.org/uniprot/P18459

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");

        int oldSequenceLength = 579;

        String oldFeatureSequence = "AQKN";
        String newFeatureSequence = "AQKN";

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

        // interaction: no
        Protein prot = getMockBuilder().createProtein("P18459", "prot");
        prot.getBioSource().setTaxId( "7227" );
        prot.getBioSource().setShortLabel( "drome" );
        prot.getAliases().clear();
        prot.setSequence(previousSequence);

        getCorePersister().saveOrUpdate(prot);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component c = interaction.getComponents().iterator().next();
        c.getBindingDomains().clear();

        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(6,6,9,9);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        c.addBindingDomain(feature);

        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getComponentDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsBefore.size());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Feature reloadedFeature = getDaoFactory().getFeatureDao().getByAc( feature.getAc() );

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(reloadedFeature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsAfter.size());

        // the ranges have been shifted
        Range reloadedRange = reloadedFeature.getRanges().iterator().next();
        Assert.assertEquals(7, reloadedRange.getFromIntervalStart());
        Assert.assertEquals(7, reloadedRange.getFromIntervalEnd());
        Assert.assertEquals(10, reloadedRange.getToIntervalStart());
        Assert.assertEquals(10, reloadedRange.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, reloadedRange.getFullSequence());

        Protein reloadedProtein = getDaoFactory().getProteinDao().getByAc( prot.getAc() );
        Assert.assertEquals(true_sequence, reloadedProtein.getSequence());
    }

    @Test
    @DirtiesContext
    public void range_shifting_update2() throws Exception {

        // http://www.uniprot.org/uniprot/P18459

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");

        int oldSequenceLength = 579;

        String oldFeatureSequence = "AQKN";
        String newFeatureSequence = "AQKN";

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

        // interaction: no
        Protein prot = getMockBuilder().createProtein("P18459", "prot");
        prot.getBioSource().setTaxId( "7227" );
        prot.getBioSource().setShortLabel( "drome" );
        prot.getAliases().clear();
        prot.setSequence(previousSequence);

        getCorePersister().saveOrUpdate(prot);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());

        // interaction: yes

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component c = interaction.getComponents().iterator().next();
        c.getBindingDomains().clear();

        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(8,8,11,11);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        c.addBindingDomain(feature);

        getCorePersister().saveOrUpdate(interaction);

        Assert.assertEquals(1, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getComponentDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getInteractionDao().countAll());

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsBefore.size());

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        IntactContext.getCurrentInstance().getDaoFactory().getEntityManager().clear();

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Feature reloadedFeature = getDaoFactory().getFeatureDao().getByAc( feature.getAc() );

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(reloadedFeature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsAfter.size());

        // the ranges have been shifted
        Range reloadedRange = reloadedFeature.getRanges().iterator().next();
        Assert.assertEquals(7, reloadedRange.getFromIntervalStart());
        Assert.assertEquals(7, reloadedRange.getFromIntervalEnd());
        Assert.assertEquals(10, reloadedRange.getToIntervalStart());
        Assert.assertEquals(10, reloadedRange.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, reloadedRange.getFullSequence());

        Protein reloadedProtein = getDaoFactory().getProteinDao().getByAc( prot.getAc() );
        Assert.assertEquals(true_sequence, reloadedProtein.getSequence());
    }


    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    public void range_shifting_update_featureChanged() throws Exception {

        // http://www.uniprot.org/uniprot/P18459
        TransactionStatus status = getDataContext().beginTransaction();

        ProteinUpdateProcessorConfig configUpdate = new ProteinUpdateProcessorConfig();
        configUpdate.setDeleteProteinTranscriptWithoutInteractions( true );

        CvTopic invalid_range = getDaoFactory().getCvObjectDao(CvTopic.class).getByShortLabel("invalid-range");

        int oldSequenceLength = 579;

        String oldFeatureSequence = "BQKN";
        String newFeatureSequence = "AQKN";

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

        // interaction: no
        Protein prot = getMockBuilder().createProtein("P18459", "prot");
        prot.getBioSource().setTaxId( "7227" );
        prot.getBioSource().setShortLabel( "drome" );
        prot.getAliases().clear();
        prot.setSequence(previousSequence);

        Protein prot2 = getMockBuilder().createProteinRandom();

        getCorePersister().saveOrUpdate(prot, prot2);

        // interaction: yes

        Interaction interaction = getMockBuilder().createInteraction(prot);
        Component c = interaction.getComponents().iterator().next();
        c.getBindingDomains().clear();

        Feature feature = getMockBuilder().createFeatureRandom();
        Range range = getMockBuilder().createRange(6,6,9,9);
        range.prepareSequence(previousSequence);
        feature.getRanges().clear();
        feature.addRange(range);

        c.addBindingDomain(feature);

        Interaction interaction2 = getMockBuilder().createInteraction(prot, prot2);

        for (Component comp : interaction2.getComponents()){
            comp.getBindingDomains().clear();
        }

        getCorePersister().saveOrUpdate(interaction, interaction2);

        Assert.assertEquals(2, getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(3, getDaoFactory().getComponentDao().countAll());
        Assert.assertEquals(1, getDaoFactory().getFeatureDao().countAll());
        Assert.assertEquals(2, getDaoFactory().getInteractionDao().countAll());

        final Collection<Annotation> cautionsBefore = AnnotatedObjectUtils.findAnnotationsByCvTopic(feature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsBefore.size());

        getDataContext().commitTransaction(status);

        // try the updater
        ProteinUpdateProcessor protUpdateProcessor = new ProteinUpdateProcessor(configUpdate);
        protUpdateProcessor.updateAll();

        TransactionStatus status2 = getDataContext().beginTransaction();

        Assert.assertEquals(3, getDaoFactory().getProteinDao().countAll());

        // check that we do have 2 proteins, both of which have a gene name (ple), a synonym (TH) and an orf (CG10118).

        Feature reloadedFeature = getDaoFactory().getFeatureDao().getByAc( feature.getAc() );

        final Collection<Annotation> cautionsAfter = AnnotatedObjectUtils.findAnnotationsByCvTopic(reloadedFeature, Collections.singleton(invalid_range));
        Assert.assertEquals(0, cautionsAfter.size());
        
        // the ranges have been shifted
        Range reloadedRange = reloadedFeature.getRanges().iterator().next();
        Assert.assertEquals(6, reloadedRange.getFromIntervalStart());
        Assert.assertEquals(6, reloadedRange.getFromIntervalEnd());
        Assert.assertEquals(9, reloadedRange.getToIntervalStart());
        Assert.assertEquals(9, reloadedRange.getToIntervalEnd());

        Assert.assertEquals(oldFeatureSequence, reloadedRange.getFullSequence());

        Protein reloadedProtein = getDaoFactory().getProteinDao().getByAc( prot.getAc() );
        Assert.assertEquals(true_sequence, reloadedProtein.getSequence());

        getDataContext().commitTransaction(status2);
    }

     private void assertHasXref( AnnotatedObject ao, String db, String qualifier, String primaryId ) {

        Assert.assertNotNull( ao );

        for ( Iterator iterator = ao.getXrefs().iterator(); iterator.hasNext(); ) {
            Xref xref = (Xref) iterator.next();

            if( (xref.getCvDatabase().getIdentifier().equals(db) || xref.getCvDatabase().getShortLabel().equals(db) ) &&
                    (xref.getCvXrefQualifier().getIdentifier().equals(qualifier) || xref.getCvXrefQualifier().getShortLabel().equals(qualifier) ) &&
                    xref.getPrimaryId().equals( primaryId ) ) {
                // found it
                return;
            }
        }

        Assert.fail( "Could not find an Xref with db='"+db+"' qualifier='"+qualifier+"' and primaryId='"+primaryId+"'." );
    }



    // seems like the global protein update bring in new splice variant if they were not in the db yet.
    // But given that they don't have interaction we don't need them be added in the first place.
}
