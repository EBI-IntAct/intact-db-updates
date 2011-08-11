package uk.ac.ebi.intact.dbupdate.prot.actions;

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
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.OutOfDateParticipantFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Tester of OutOfDateParticipantFixerImpl
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>15-Nov-2010</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class OutOfDateParticipantFixerTest  extends IntactBasicTestCase {

    private OutOfDateParticipantFixerImpl participantFixer;
    @Before
    public void setUp(){
        participantFixer = new OutOfDateParticipantFixerImpl(new RangeFixerImpl());
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }
    @After
    public void after() throws Exception {
        participantFixer = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Create a deprecated protein : move the listed active instances, add 'no-uniprot-update' and a caution.
     */
    public void create_deprecated_protein(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        DataContext context = getDataContext();
        TransactionStatus status = context.beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        primary.addAlias(getMockBuilder().createAliasGeneName(primary, "CDC42"));
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(primary, random2);

        RangeUpdateReport rangeReport = new RangeUpdateReport();

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();

            if (primary.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                rangeReport.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());

        // create deprecated participant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary, uniprot, rangeReport, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primary.getAc());

        ProteinTranscript proteinTranscript = participantFixer.createDeprecatedProtein(evt);

        Assert.assertNotNull(proteinTranscript);
        Assert.assertNull(proteinTranscript.getUniprotVariant());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript.getProtein().getActiveInstances().size());
        Assert.assertTrue(hasAnnotation(proteinTranscript.getProtein(), null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(proteinTranscript.getProtein(), null, CvTopic.CAUTION));
        Assert.assertEquals(proteinTranscript.getProtein().getXrefs().size(), primary.getXrefs().size());
        Assert.assertEquals(proteinTranscript.getProtein().getAliases().size(), primary.getAliases().size());

        context.commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein with feature conflicts is not a splice variant or feature chain of the uniprot protein.
     * We don't want to create a deprecated protein so the interactions are still attached to the protein with feature conflicts.
     */
    public void fix_participant_no(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(primary, random2);

        RangeUpdateReport rangeReport = new RangeUpdateReport();

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();

            if (primary.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                rangeReport.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());

        // create deprecated participant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary, uniprot, rangeReport, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primary.getAc());

        ProteinTranscript proteinTranscript = participantFixer.fixParticipantWithRangeConflicts(evt, false, true);

        Assert.assertNull(proteinTranscript);
        Assert.assertEquals(2, primary.getActiveInstances().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * The protein with feature conflicts is not a splice variant or feature chain of the uniprot protein.
     * We enabled the creation of a deprecated protein so the interactions are moved to the deprecated protein.
     */
    public void fix_participant_yes_create_deprecated_protein(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();
        String sequence = "AAFFSSPPAAMMYYLLLLLAAAAAAAAAA";

        Protein primary = getMockBuilder().createProtein("P60953", "primary");
        primary.setSequence(sequence);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionPrimary = getMockBuilder().createInteraction(primary, random1);
        Interaction interactionToMove = getMockBuilder().createInteraction(primary, random2);

        RangeUpdateReport rangeReport = new RangeUpdateReport();

        for (Component c : interactionPrimary.getComponents()){
            c.getBindingDomains().clear();
        }
        for (Component c : interactionToMove.getComponents()){
            c.getBindingDomains().clear();

            if (primary.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                rangeReport.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionPrimary);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionToMove);

        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(2, primary.getActiveInstances().size());

        // create deprecated participant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primary, uniprot, rangeReport, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primary.getAc());

        ProteinTranscript proteinTranscript = participantFixer.fixParticipantWithRangeConflicts(evt, true, true);

        Assert.assertNotNull(proteinTranscript);
        Assert.assertNull(proteinTranscript.getUniprotVariant());
        Assert.assertEquals(1, primary.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript.getProtein().getActiveInstances().size());
        Assert.assertTrue(hasAnnotation(proteinTranscript.getProtein(), null, CvTopic.NON_UNIPROT));
        Assert.assertTrue(hasAnnotation(proteinTranscript.getProtein(), null, CvTopic.CAUTION));
        Assert.assertEquals(proteinTranscript.getProtein().getXrefs().size(), primary.getXrefs().size());
        Assert.assertEquals(proteinTranscript.getProtein().getAliases().size(), primary.getAliases().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein with feature conflicts is a splice variant and the other protein is a feature chain of the uniprot protein.
     * No intact proteins exist for these splice variants/feature chains.
     * A new splice variant/feature chain is created in Intact. The interactions with feature conflicts are attached to it.
     * The new splice variant/feature chain in IntAct has a parent reference to the previous protein containing feature conflicts.
     */
    public void fix_participant_uniprot_transcript_yes_intact_protein_no(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant sv : uniprot.getSpliceVariants()){
            if (sv.getPrimaryAc().equalsIgnoreCase("P60953-2")){
                variant = sv;
            }
        }

        UniprotFeatureChain chain = uniprot.getFeatureChains().iterator().next();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primaryIsoform = getMockBuilder().createProtein("P60953", "primary(futur isoform)");
        primaryIsoform.setSequence(variant.getSequence());
        primaryIsoform.addAlias(getMockBuilder().createAliasGeneName(primaryIsoform, "CDC42"));
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryIsoform);

        Protein primaryChain = getMockBuilder().createProtein("P60954", "primary(futur chain)");
        primaryChain.setSequence(chain.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryChain);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionIsoform = getMockBuilder().createInteraction(primaryIsoform, random1);
        Interaction interactionChain = getMockBuilder().createInteraction(primaryChain, random2);

        RangeUpdateReport reportIsoform = new RangeUpdateReport();
        RangeUpdateReport reportChain = new RangeUpdateReport();

        for (Component c : interactionIsoform.getComponents()){
            c.getBindingDomains().clear();

            if (primaryIsoform.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportIsoform.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }
        for (Component c : interactionChain.getComponents()){
            c.getBindingDomains().clear();

            if (primaryChain.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportChain.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionIsoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionChain);

        Assert.assertEquals(4, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());

        // create splice variant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryIsoform, uniprot, reportIsoform, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryIsoform.getAc());

        ProteinTranscript proteinTranscript = participantFixer.fixParticipantWithRangeConflicts(evt, true, true);

        Assert.assertNotNull(proteinTranscript);
        Assert.assertEquals(variant, proteinTranscript.getUniprotVariant());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript.getProtein().getActiveInstances().size());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(proteinTranscript.getProtein()).getPrimaryId());
        Assert.assertTrue(hasXRef(proteinTranscript.getProtein(), primaryIsoform.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertEquals(5, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, proteinTranscript.getProtein().getXrefs().size());
        Assert.assertEquals(0, proteinTranscript.getProtein().getAnnotations().size());
        Assert.assertEquals(0, proteinTranscript.getProtein().getAliases().size());
        Assert.assertEquals(variant.getSequence(), proteinTranscript.getProtein().getSequence());

        // create feature chain
        OutOfDateParticipantFoundEvent evt2 = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryChain, uniprot, reportChain, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, primaryChain.getAc());

        ProteinTranscript proteinTranscript2 = participantFixer.fixParticipantWithRangeConflicts(evt2, true, true);

        Assert.assertNotNull(proteinTranscript2);
        Assert.assertEquals(chain, proteinTranscript2.getUniprotVariant());
        Assert.assertEquals(0, primaryChain.getActiveInstances().size());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript2.getProtein().getActiveInstances().size());
        Assert.assertEquals(chain.getPrimaryAc(), ProteinUtils.getUniprotXref(proteinTranscript2.getProtein()).getPrimaryId());
        Assert.assertTrue(hasXRef(proteinTranscript2.getProtein(), primaryChain.getAc(), CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT));
        Assert.assertEquals(6, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(chain.getSequence(), proteinTranscript2.getProtein().getSequence());
        Assert.assertEquals(2, proteinTranscript2.getProtein().getXrefs().size());
        Assert.assertEquals(0, proteinTranscript2.getProtein().getAnnotations().size());
        Assert.assertEquals(0, proteinTranscript2.getProtein().getAliases().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein with feature conflicts is a splice variant and the other protein is a feature chain of the uniprot protein.
     * Intact proteins with sequences up to date already exist for these splice variants/feature chains.
     * The interactions with feature conflicts are attached to the intact splice variant/feature chain.
     */
    public void fix_participant_uniprot_transcript_yes_intact_protein_yes_same_sequence(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant sv : uniprot.getSpliceVariants()){
            if (sv.getPrimaryAc().equalsIgnoreCase("P60953-2")){
                variant = sv;
            }
        }

        UniprotFeatureChain chain = uniprot.getFeatureChains().iterator().next();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60952", "primary");
        primary.setSequence(uniprot.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Protein primaryIsoform = getMockBuilder().createProtein("P60953", "primary(futur isoform)");
        primaryIsoform.setSequence(variant.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryIsoform);

        Protein primaryChain = getMockBuilder().createProtein("P60954", "primary(futur chain)");
        primaryChain.setSequence(chain.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryChain);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, variant.getPrimaryAc(), "isoform_sequene_up_to_date");
        isoform.setSequence(variant.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Protein featureChain = getMockBuilder().createProteinChain(primary, chain.getPrimaryAc(), "chain_sequene_up_to_date");
        featureChain.setSequence(chain.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(featureChain);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionIsoform = getMockBuilder().createInteraction(primaryIsoform, random1);
        Interaction interactionChain = getMockBuilder().createInteraction(primaryChain, random2);

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variant));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(featureChain, chain));

        RangeUpdateReport reportIsoform = new RangeUpdateReport();
        RangeUpdateReport reportChain = new RangeUpdateReport();

        for (Component c : interactionIsoform.getComponents()){
            c.getBindingDomains().clear();

            if (primaryIsoform.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportIsoform.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }
        for (Component c : interactionChain.getComponents()){
            c.getBindingDomains().clear();

            if (primaryChain.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportChain.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionIsoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionChain);

        Assert.assertEquals(7, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());

        // create splice variant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryIsoform, uniprot, reportIsoform, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, primary.getAc());

        ProteinTranscript proteinTranscript = participantFixer.fixParticipantWithRangeConflicts(evt, true, true);

        Assert.assertNotNull(proteinTranscript);
        Assert.assertEquals(variant, proteinTranscript.getUniprotVariant());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());
        Assert.assertEquals(isoform.getAc(), proteinTranscript.getProtein().getAc());
        Assert.assertEquals(1, isoform.getActiveInstances().size());
        Assert.assertTrue(hasXRef(proteinTranscript.getProtein(), primary.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertEquals(7, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        // create feature chain
        OutOfDateParticipantFoundEvent evt2 = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryChain, uniprot, reportChain, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, primary.getAc());

        ProteinTranscript proteinTranscript2 = participantFixer.fixParticipantWithRangeConflicts(evt2, true, true);

        Assert.assertNotNull(proteinTranscript2);
        Assert.assertEquals(chain, proteinTranscript2.getUniprotVariant());
        Assert.assertEquals(0, primaryChain.getActiveInstances().size());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(featureChain.getAc(), proteinTranscript2.getProtein().getAc());
        Assert.assertEquals(1, featureChain.getActiveInstances().size());
        Assert.assertTrue(hasXRef(proteinTranscript2.getProtein(), primary.getAc(), CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT));
        Assert.assertEquals(7, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * One protein with feature conflicts is a splice variant and the other protein is a feature chain of the uniprot protein.
     * Intact proteins exist for these splice variants/feature chains but their sequence is not up to date.
     * A new splice variant/feature chain is created in Intact. The interactions with feature conflicts are attached to it.
     * The new splice variant/feature chain in IntAct has a parent reference to the previous protein containing feature conflicts.
     */
    public void fix_participant_uniprot_transcript_yes_intact_protein_yes_different_sequence(){
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();
        uniprot.getFeatureChains().add(new UniprotFeatureChain("PRO-1", uniprot.getOrganism(), "AAACCTA"));

        UniprotSpliceVariant variant = null;

        for (UniprotSpliceVariant sv : uniprot.getSpliceVariants()){
            if (sv.getPrimaryAc().equalsIgnoreCase("P60953-2")){
                variant = sv;
            }
        }

        UniprotFeatureChain chain = uniprot.getFeatureChains().iterator().next();

        TransactionStatus status = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        Protein primary = getMockBuilder().createProtein("P60952", "primary");
        primary.setSequence(uniprot.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primary);

        Protein primaryIsoform = getMockBuilder().createProtein("P60953", "primary(futur isoform)");
        primaryIsoform.setSequence(variant.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryIsoform);

        Protein primaryChain = getMockBuilder().createProtein("P60954", "primary(futur chain)");
        primaryChain.setSequence(chain.getSequence());
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(primaryChain);

        Protein isoform = getMockBuilder().createProteinSpliceVariant(primary, variant.getPrimaryAc(), "isoform_sequene_up_to_date");
        isoform.setSequence(null);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(isoform);

        Protein featureChain = getMockBuilder().createProteinChain(primary, chain.getPrimaryAc(), "chain_sequene_up_to_date");
        featureChain.setSequence(null);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(featureChain);

        Protein random1 = getMockBuilder().createProteinRandom();
        Protein random2 = getMockBuilder().createProteinRandom();
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random1, random2);

        Interaction interactionIsoform = getMockBuilder().createInteraction(primaryIsoform, random1);
        Interaction interactionChain = getMockBuilder().createInteraction(primaryChain, random2);

        Collection<ProteinTranscript> primaryIsoforms = new ArrayList<ProteinTranscript>();
        primaryIsoforms.add(new ProteinTranscript(isoform, variant));
        Collection<ProteinTranscript> primaryChains = new ArrayList<ProteinTranscript>();
        primaryChains.add(new ProteinTranscript(featureChain, chain));

        RangeUpdateReport reportIsoform = new RangeUpdateReport();
        RangeUpdateReport reportChain = new RangeUpdateReport();

        for (Component c : interactionIsoform.getComponents()){
            c.getBindingDomains().clear();

            if (primaryIsoform.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportIsoform.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }
        for (Component c : interactionChain.getComponents()){
            c.getBindingDomains().clear();

            if (primaryChain.getAc().equalsIgnoreCase(c.getInteractor().getAc())){
                reportChain.getInvalidComponents().put(c, Collections.EMPTY_LIST);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionIsoform);
        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interactionChain);

        Assert.assertEquals(7, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getInteractionDao().countAll());
        Assert.assertEquals(1, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());

        // create splice variant
        OutOfDateParticipantFoundEvent evt = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryIsoform, uniprot, reportIsoform, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, primary.getAc());

        ProteinTranscript proteinTranscript = participantFixer.fixParticipantWithRangeConflicts(evt, true, true);

        Assert.assertNotNull(proteinTranscript);
        Assert.assertEquals(variant, proteinTranscript.getUniprotVariant());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, primaryChain.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript.getProtein().getActiveInstances().size());
        Assert.assertNotSame(isoform.getAc(), proteinTranscript.getProtein().getAc());
        Assert.assertEquals(variant.getPrimaryAc(), ProteinUtils.getUniprotXref(proteinTranscript.getProtein()).getPrimaryId());
        Assert.assertTrue(hasXRef(proteinTranscript.getProtein(), primary.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertEquals(8, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(2, proteinTranscript.getProtein().getXrefs().size());
        Assert.assertEquals(0, proteinTranscript.getProtein().getAnnotations().size());
        Assert.assertEquals(0, proteinTranscript.getProtein().getAliases().size());
        Assert.assertEquals(variant.getSequence(), proteinTranscript.getProtein().getSequence());

        // create feature chain
        OutOfDateParticipantFoundEvent evt2 = new OutOfDateParticipantFoundEvent(new ProteinUpdateProcessor(), IntactContext.getCurrentInstance().getDataContext(), primaryChain, uniprot, reportChain, primaryIsoforms, Collections.EMPTY_LIST, primaryChains, primary.getAc());

        ProteinTranscript proteinTranscript2 = participantFixer.fixParticipantWithRangeConflicts(evt2, true, true);

        Assert.assertNotNull(proteinTranscript2);
        Assert.assertEquals(chain, proteinTranscript2.getUniprotVariant());
        Assert.assertEquals(0, primaryChain.getActiveInstances().size());
        Assert.assertEquals(0, primaryIsoform.getActiveInstances().size());
        Assert.assertEquals(1, proteinTranscript2.getProtein().getActiveInstances().size());
        Assert.assertNotSame(isoform.getAc(), proteinTranscript.getProtein().getAc());
        Assert.assertEquals(chain.getPrimaryAc(), ProteinUtils.getUniprotXref(proteinTranscript2.getProtein()).getPrimaryId());
        Assert.assertTrue(hasXRef(proteinTranscript2.getProtein(), primary.getAc(), CvDatabase.INTACT, CvXrefQualifier.CHAIN_PARENT));
        Assert.assertEquals(9, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());
        Assert.assertEquals(chain.getSequence(), proteinTranscript2.getProtein().getSequence());
        Assert.assertEquals(2, proteinTranscript2.getProtein().getXrefs().size());
        Assert.assertEquals(0, proteinTranscript2.getProtein().getAnnotations().size());
        Assert.assertEquals(0, proteinTranscript2.getProtein().getAliases().size());

        IntactContext.getCurrentInstance().getDataContext().commitTransaction(status);
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
