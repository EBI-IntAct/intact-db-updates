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
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.unit.IntactBasicTestCase;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.OutOfDateParticipantFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.RangeFixerImpl;
import uk.ac.ebi.intact.dbupdate.prot.actions.impl.UniprotProteinUpdaterImpl;
import uk.ac.ebi.intact.dbupdate.prot.event.UpdateCaseEvent;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;
import uk.ac.ebi.intact.util.protein.ComprehensiveCvPrimer;
import uk.ac.ebi.intact.util.protein.mock.MockUniprotProtein;

import java.util.ArrayList;
import java.util.Collection;
import java.util.Collections;

/**
 * Third tester of UniprotProteinUpdater
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Jan-2011</pre>
 */
@ContextConfiguration(locations = {"classpath*:/META-INF/dbupdate.spring.xml"} )
public class UniprotProteinUpdater3Test extends IntactBasicTestCase {

    private UniprotProteinUpdaterImpl updater;

    @Before
    public void before() throws Exception {
        updater = new UniprotProteinUpdaterImpl(new OutOfDateParticipantFixerImpl(new RangeFixerImpl()));
        TransactionStatus status = getDataContext().beginTransaction();

        ComprehensiveCvPrimer primer = new ComprehensiveCvPrimer(getDaoFactory());
        primer.createCVs();

        getDataContext().commitTransaction(status);
    }

    @After
    public void after() throws Exception {
        updater = null;
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The organism of the protein is null and will be updated as well with the organism in uniprot
     */
    public void update_master_protein_biosource_null() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";
        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.setBioSource(null);
        protein.setSequence(sequence);
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());
        Assert.assertEquals(uniprot.getOrganism().getTaxid(), Integer.parseInt(updatedProtein.getBioSource().getTaxId()));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The organism of the protein is different than the uniprot protein : there is a conflict and the protein cannot ba updated
     */
    public void update_master_protein_organism_different() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        String sequence = "AAAIILKY";
        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("-1");
        protein.setSequence(sequence);
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        InteractorAlias alias = getMockBuilder().createAlias(protein, "name",
                getMockBuilder().createCvObject(CvAliasType.class, CvAliasType.ORF_NAME_MI_REF,
                        CvAliasType.ORF_NAME));
        protein.addAlias(alias);

        InteractorXref ref = getMockBuilder().createXref(protein, "test",
                getMockBuilder().createCvObject(CvXrefQualifier.class, CvXrefQualifier.IDENTITY_MI_REF,
                        CvXrefQualifier.IDENTITY), getMockBuilder().createCvObject(CvDatabase.class, CvDatabase.REFSEQ_MI_REF,
                        CvDatabase.REFSEQ));
        protein.addXref(ref);

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(0, evt.getPrimaryProteins().size());
        Assert.assertEquals(0, evt.getXrefUpdaterReports().size());

        Assert.assertNotSame(uniprot.getOrganism().getTaxid(), Integer.parseInt(protein.getBioSource().getTaxId()));
        Assert.assertNotSame(uniprot.getId().toLowerCase(), protein.getShortLabel());
        Assert.assertNotSame(uniprot.getDescription(), protein.getFullName());
        Assert.assertNotSame(uniprot.getSequence(), protein.getSequence());
        Assert.assertNotSame(uniprot.getCrc64(), protein.getCrc64());
        Assert.assertEquals(uniprot.getPrimaryAc(), ProteinUtils.getUniprotXref(protein).getPrimaryId());

        Assert.assertEquals(2, protein.getXrefs().size());
        Assert.assertEquals(1, protein.getAliases().size());
        Assert.assertTrue(hasAlias(protein, CvAliasType.ORF_NAME, "name"));
        Assert.assertTrue(hasXRef(protein, "test", CvDatabase.REFSEQ, CvXrefQualifier.IDENTITY));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges will be shifted properly
     */
    public void update_master_interaction_range_shifted() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence().substring(5));
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(1, updatedProtein.getActiveInstances().size());

        Assert.assertEquals(7, range.getFromIntervalStart());
        Assert.assertEquals(7, range.getFromIntervalEnd());
        Assert.assertEquals(10, range.getToIntervalStart());
        Assert.assertEquals(10, range.getToIntervalEnd());

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges cannot be shifted properly : as no protein transcript in uniprot has the same sequence, a deprecated
     * protein will be created and the interactions will be attached to this protein
     */
    public void update_master_interaction_range_impossible_to_shift_no_uniprot_update() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence());
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        uniprot.setSequence(protein.getSequence().substring(4));

        for (UniprotSpliceVariant v : uniprot.getSpliceVariants()){
            if (v.getSequence().equals(protein.getSequence())){
                v.setSequence(uniprot.getSequence());
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        Component componentWithFeatureConflicts = null;
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
                componentWithFeatureConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(1, updatedProtein.getActiveInstances().size());
        Assert.assertEquals(2, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Assert.assertEquals(0, range.getFromIntervalStart());
        Assert.assertEquals(0, range.getFromIntervalEnd());
        Assert.assertEquals(0, range.getToIntervalStart());
        Assert.assertEquals(0, range.getToIntervalEnd());

        Protein noUniprotUpdate = (Protein) componentWithFeatureConflicts.getInteractor();
        Assert.assertTrue(ProteinUtils.isFromUniprot(noUniprotUpdate));
        Assert.assertNotNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(noUniprotUpdate.getAc()));

        getDataContext().commitTransaction(status);
    }

    @Test
    @DirtiesContext
    @Transactional(propagation = Propagation.NEVER)
    /**
     * Update a protein in intact with the uniprot protein.
     * The protein has a sequence not up to date and is involved in one interaction with features to shift.
     * The ranges cannot be shifted properly : one protein transcript in uniprot has the same sequence but no intact protein
     * is matching this splice variant: it will be created and the interactions will be attached to this transcript
     */
    public void update_master_interaction_range_impossible_to_shift_create_isoform() throws Exception{
        TransactionStatus status = getDataContext().beginTransaction();
        UniprotProtein uniprot = MockUniprotProtein.build_CDC42_HUMAN();

        Protein protein = getMockBuilder().createProtein(uniprot.getPrimaryAc(), "intact");
        protein.getBioSource().setTaxId("9606");
        protein.setSequence(uniprot.getSequence());
        protein.getAnnotations().clear();
        protein.getAliases().clear();

        uniprot.setSequence(protein.getSequence().substring(4));

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(protein);

        Protein random = getMockBuilder().createProteinRandom();

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(random);

        Range range = getMockBuilder().createRange(2, 2, 5, 5);
        Feature feature = getMockBuilder().createFeatureRandom();
        feature.getRanges().clear();
        feature.addRange(range);

        Interaction interaction = getMockBuilder().createInteraction(protein, random);
        Component componentWithFeatureConflicts = null;
        for (Component c : interaction.getComponents()){
            c.getBindingDomains().clear();

            if (c.getInteractor().getAc().equals(protein.getAc())){
                c.addBindingDomain(feature);
                componentWithFeatureConflicts = c;
            }
        }

        IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(interaction);

        Collection<Protein> primaryProteins = new ArrayList<Protein>();
        primaryProteins.add(protein);

        UpdateCaseEvent evt = new UpdateCaseEvent(new ProteinUpdateProcessor(),
                IntactContext.getCurrentInstance().getDataContext(), uniprot, primaryProteins,
                Collections.EMPTY_LIST, new ArrayList<ProteinTranscript>(), Collections.EMPTY_LIST, Collections.EMPTY_LIST, uniprot.getPrimaryAc());

        updater.createOrUpdateProtein(evt);

        Assert.assertEquals(1, evt.getPrimaryProteins().size());
        Assert.assertEquals(1, evt.getPrimaryIsoforms().size());
        Assert.assertEquals(1, evt.getXrefUpdaterReports().size());

        Protein updatedProtein = evt.getPrimaryProteins().iterator().next();
        Assert.assertEquals(protein.getAc(), updatedProtein.getAc());

        Assert.assertEquals(uniprot.getSequence(), updatedProtein.getSequence());
        Assert.assertEquals(uniprot.getCrc64(), updatedProtein.getCrc64());
        Assert.assertEquals(0, updatedProtein.getActiveInstances().size());
        Assert.assertEquals(3, IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().countAll());

        Assert.assertEquals(2, range.getFromIntervalStart());
        Assert.assertEquals(2, range.getFromIntervalEnd());
        Assert.assertEquals(5, range.getToIntervalStart());
        Assert.assertEquals(5, range.getToIntervalEnd());

        Protein transcript = (Protein) componentWithFeatureConflicts.getInteractor();
        Assert.assertTrue(hasXRef(transcript, protein.getAc(), CvDatabase.INTACT, CvXrefQualifier.ISOFORM_PARENT));
        Assert.assertNotNull(IntactContext.getCurrentInstance().getDaoFactory().getProteinDao().getByAc(transcript.getAc()));

        getDataContext().commitTransaction(status);
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
