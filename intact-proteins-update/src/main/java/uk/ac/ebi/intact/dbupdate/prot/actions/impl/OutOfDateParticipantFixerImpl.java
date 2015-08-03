package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.core.persistence.dao.AnnotationDao;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProteinTranscript;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.OutOfDateParticipantFixer;
import uk.ac.ebi.intact.dbupdate.prot.actions.RangeFixer;
import uk.ac.ebi.intact.dbupdate.prot.event.OutOfDateParticipantFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ComponentTools;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotFeatureChain;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.model.UniprotProteinTranscript;
import uk.ac.ebi.intact.uniprot.model.UniprotSpliceVariant;

import java.util.ArrayList;
import java.util.Collection;

/**
 * This class is dealing with participants having interactions with range conflicts
 * and look if the uniprot entry doesn't contain isoforms/feature chains with the same sequence to remap the interactions
 * having range conflicts and which cannot be remapped to the lates canonical sequence in uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */

public class OutOfDateParticipantFixerImpl implements OutOfDateParticipantFixer {

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( OutOfDateParticipantFixerImpl.class );

    public static final String FEATURE_OBSOLETE = "This protein is not up-to-date anymore with the uniprot protein because of feature conflicts. ";

    private RangeFixer rangeFixer;

    public OutOfDateParticipantFixerImpl(RangeFixer rangeFixer){
        if (rangeFixer != null){
            this.rangeFixer = rangeFixer;
        }
        else {
            this.rangeFixer = new RangeFixerImpl();
        }
    }
    /**
     *
     * @param sequence : the protein sequence to retrieve
     * @param uniprotProtein : the uniprot entry to look into
     * @return the UniprotProteinTranscript with the exact same sequence and which is not the canonical sequence. Return null if
     * not protein transcript matches the exact sequence without having the canonical sequence
     */
    public UniprotProteinTranscript findTranscriptsWithIdenticalSequence(String sequence, UniprotProtein uniprotProtein){
        // get all the isoforms and feature chains attached to the uniprot entry
        Collection<UniprotProteinTranscript> proteinTranscripts = new ArrayList<UniprotProteinTranscript>();
        proteinTranscripts.addAll(uniprotProtein.getSpliceVariants());
        proteinTranscripts.addAll(uniprotProtein.getFeatureChains());

        // if the uniprot entry contains isoforms and feature chains
        if (!proteinTranscripts.isEmpty()){

            for (UniprotProteinTranscript pt : proteinTranscripts){
                // if the sequence is exactly the same as a protein transcript and this uniprot transcript doesn't have the canonical sequence, return it
                if (sequence.equalsIgnoreCase(pt.getSequence()) && !sequence.equalsIgnoreCase(uniprotProtein.getSequence())){
                    return pt;
                }
            }
        }

        // not transcript in uniprot has this exact sequence
        return null;
    }

    /**
     *
     * @param sequence : the protein sequence to retrieve
     * @param uniprotProteinTranscript : the uniprot transcript with the sequence we want to exclude
     * @param uniprotProtein : the uniprot entry to look into
     * @return the UniprotProteinTranscript with the exact same sequence and which is not uniprotproteinTranscript. Return null if
     * not protein transcript matches the exact sequence
     */
    public UniprotProteinTranscript findTranscriptsWithIdenticalSequence(String sequence, UniprotProteinTranscript uniprotProteinTranscript, UniprotProtein uniprotProtein){
        // get all the isoforms and feature chains attached to the uniprot entry
        Collection<UniprotProteinTranscript> proteinTranscripts = new ArrayList<UniprotProteinTranscript>();
        proteinTranscripts.addAll(uniprotProtein.getSpliceVariants());
        proteinTranscripts.addAll(uniprotProtein.getFeatureChains());

        // if the uniprot entry contains isoforms and feature chains different from the uniprotprotein transcript we want to exclude
        if (!(proteinTranscripts.size() == 1 && proteinTranscripts.contains(uniprotProteinTranscript)) && !proteinTranscripts.isEmpty()){

            for (UniprotProteinTranscript pt : proteinTranscripts){
                if (sequence.equalsIgnoreCase(pt.getSequence()) ){

                    if (uniprotProteinTranscript != null){
                        if (!sequence.equalsIgnoreCase(uniprotProteinTranscript.getSequence())){
                            return pt;
                        }
                    }
                    else {
                        return pt;
                    }
                }
            }
        }

        return null;
    }

    /**
     *
     * @param spliceVariant : The uniprot splice variant
     * @param proteinWithConflicts : the protein having range conflicts and for what we want to move the interactions to a new splice variant
     * @param componentsToFix : the components with range conflicts to move
     * @param factory : the daofactory
     * @param evt : event containing protein with conflicts and the list of components to move
     * @return the new ProteinTranscript with the components with previous range conflicts attached to it
     */
    private ProteinTranscript createSpliceVariant(UniprotSpliceVariant spliceVariant, Protein proteinWithConflicts, Collection<Component> componentsToFix, DaoFactory factory, OutOfDateParticipantFoundEvent evt){

        // get the qualifier 'isoform-parent'
        CvXrefQualifier isoformParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.ISOFORM_PARENT_MI_REF);

        if (isoformParent == null) {
            isoformParent = CvObjectUtils.createCvObject(proteinWithConflicts.getOwner(), CvXrefQualifier.class, CvXrefQualifier.ISOFORM_PARENT_MI_REF, CvXrefQualifier.ISOFORM_PARENT);
            factory.getCvObjectDao(CvXrefQualifier.class).persist(isoformParent);
        }

        // create an intact splice variant
        return createProteinTranscript(spliceVariant, proteinWithConflicts, componentsToFix, factory, evt, isoformParent);
    }

    /**
     *
     * @param featureChain : the feature chain in uniprot
     * @param proteinWithConflicts : the protein having range conflicts and for what we want to move the interactions to a new splice variant
     * @param componentsToFix : the components with range conflicts to move
     * @param factory : the daofactory
     * @param evt : event containing protein with conflicts and the list of components to move
     * @return the new ProteinTranscript with the components with previous range conflicts attached to it
     */
    private ProteinTranscript createFeatureChain(UniprotFeatureChain featureChain, Protein proteinWithConflicts, Collection<Component> componentsToFix, DaoFactory factory, OutOfDateParticipantFoundEvent evt){
        // get the qualifier 'chain-parent'
        CvXrefQualifier chainParent = factory.getCvObjectDao(CvXrefQualifier.class).getByPsiMiRef(CvXrefQualifier.CHAIN_PARENT_MI_REF);

        if (chainParent == null) {
            chainParent = CvObjectUtils.createCvObject(proteinWithConflicts.getOwner(), CvXrefQualifier.class, CvXrefQualifier.CHAIN_PARENT_MI_REF, CvXrefQualifier.CHAIN_PARENT);
            factory.getCvObjectDao(CvXrefQualifier.class).persist(chainParent);
        }

        // create an intact feature chain
        return createProteinTranscript(featureChain, proteinWithConflicts, componentsToFix, factory, evt, chainParent);
    }

    /**
     /**
     * @param uniprotTranscript : the transcript in uniprot
     * @param proteinWithConflicts : the protein having range conflicts and for what we want to move the interactions to a new splice variant
     * @param componentsToFix : the components with range conflicts to move
     * @param factory : the daofactory
     * @param evt : event containing protein with conflicts and the list of components to move
     * @param qualifierParent : the parent ac of the transcript in intact
     * @return the new ProteinTranscript with the components with previous range conflicts attached to it
     */
    private ProteinTranscript createProteinTranscript(UniprotProteinTranscript uniprotTranscript, Protein proteinWithConflicts, Collection<Component> componentsToFix, DaoFactory factory, OutOfDateParticipantFoundEvent evt, CvXrefQualifier qualifierParent){
        // create a protein transcript having the primary ac of the uniprot protein transcript as uniprot identity and which has the same sequence, same biosource, same owner etc.
        Protein transcriptIntact = cloneProteinForTranscript(factory, proteinWithConflicts, uniprotTranscript.getPrimaryAc());

        // get the database intact
        Institution owner = transcriptIntact.getOwner();
        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

        if (db == null){
            db = CvObjectUtils.createCvObject(owner, CvDatabase.class, CvDatabase.INTACT_MI_REF, CvDatabase.INTACT);
            factory.getCvObjectDao(CvDatabase.class).persist(db);
        }

        // the parent
        String parentXrefAc = evt.getValidParentAc() != null ? evt.getValidParentAc() : proteinWithConflicts.getAc();

        // add the transcript parent xref which is the protein having range conflicts : it is the valid parent ac of the event
        InteractorXref parent = new InteractorXref(owner, db, parentXrefAc, qualifierParent);
        transcriptIntact.addXref(parent);
        factory.getXrefDao(InteractorXref.class).persist(parent);

        String primaryAc = evt.getProtein() != null ? evt.getProtein().getPrimaryAc() : null;
        // move the components having range conflicts
        ComponentTools.moveComponents(transcriptIntact, proteinWithConflicts, evt.getDataContext(), (ProteinUpdateProcessor) evt.getSource(), componentsToFix, primaryAc);

        // log in 'created.csv'
        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

            processor.fireOnProteinCreated(new ProteinEvent(processor, evt.getDataContext(), transcriptIntact, "The protein is a feature chain created because of feature ranges impossible to remap to the canonical sequence in uniprot."));

            evt.setRemappedProteinAc(transcriptIntact.getAc());
        }

        // return the new ProteinTranscript
        return new ProteinTranscript(transcriptIntact, uniprotTranscript);
    }

    /**
     *
     * @param evt : evt containing protein with conflicts, the components having range conflicts and the ac of the parent protein in case we create a new transcript
     * @param createDeprecatedParticipant
     * @param fixOutOfDateRanges
     * @return the protein transcript with the remapped protein if remapping possible. If not and createDeprecated protein is true, a deprecated protein is returned. If fixOutOfDateRanges is true and remapping was impossible
     * and createDeprecatedParticipant was false, we can fix the ranges (set to undetermined), otherwise we just log them
     */
    public ProteinTranscript fixParticipantWithRangeConflicts(OutOfDateParticipantFoundEvent evt, boolean createDeprecatedParticipant, boolean fixOutOfDateRanges){

        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // the components with range conflicts
        Collection<Component> componentsToFix = evt.getComponentsToFix();

        // the original protein associated with these components
        Protein protein = evt.getProteinWithConflicts();

        // the uniprot entry
        UniprotProtein uniprotProtein = evt.getProtein();

        // sequence without conflicts is the sequence of the intact protein
        String sequenceWithoutConflicts = protein.getSequence();

        String uniprot = evt.getProtein() != null ? evt.getProtein().getPrimaryAc() : null;

        // if the sequence without conflicts is not null, try to remap to uniprot protein transcript having the exact same sequence
        if (sequenceWithoutConflicts != null){

            // the protein transcript with the exact sequence
            UniprotProteinTranscript possibleMatch = findTranscriptsWithIdenticalSequence(sequenceWithoutConflicts, uniprotProtein);

            // if one protein transcript in uniprot has the exact same sequence
            if (possibleMatch != null){
                ProteinTranscript fixedProtein;

                // if we have a splice variant
                if (possibleMatch instanceof UniprotSpliceVariant){

                    // try to find an intact primary isoform representing the same uniprot transcript and having its sequence up to date with uniprot
                    ProteinTranscript intactMatch = findAndMoveInteractionsToIntactProteinTranscript(evt.getDataContext(), protein, sequenceWithoutConflicts, possibleMatch, evt.getPrimaryIsoforms(), (ProteinUpdateProcessor) evt.getSource());

                    // no primary isoforms in intact are mapping the uniprot transcript, try to find an intact secondary isoform representing the same uniprot transcript and having its sequence up to date with uniprot
                    if (intactMatch == null){
                        intactMatch = findAndMoveInteractionsToIntactProteinTranscript(evt.getDataContext(), protein, sequenceWithoutConflicts, possibleMatch, evt.getSecondaryIsoforms(), (ProteinUpdateProcessor) evt.getSource());
                    }

                    // return the intact entry if it exists and is up to date with uniprot
                    if (intactMatch != null){
                        evt.setRemappedProteinAc(intactMatch.getProtein().getAc());

                        fixedProtein = intactMatch;
                    }
                    else {
                        // create the intact entry matching the uniprot transcript
                        fixedProtein = createSpliceVariant((UniprotSpliceVariant) possibleMatch, protein, componentsToFix, factory, evt);
                    }
                }
                else {
                    // try to find an intact feature chain representing the same uniprot transcript and having its sequence up to date with uniprot
                    ProteinTranscript intactMatch = findAndMoveInteractionsToIntactProteinTranscript(evt.getDataContext(), protein, sequenceWithoutConflicts, possibleMatch, evt.getPrimaryFeatureChains(), (ProteinUpdateProcessor) evt.getSource());

                    // return the intact entry if it exists and is up to date with uniprot
                    if (intactMatch != null){

                        fixedProtein = intactMatch;
                    }
                    else {
                        // create the intact entry matching the uniprot transcript
                        fixedProtein = createFeatureChain((UniprotFeatureChain) possibleMatch, protein, componentsToFix, factory, evt);
                    }
                }

                // If we had invalid ranges, we will fix them. If we had out of date ranges, we just log them
                rangeFixer.processInvalidRanges(fixedProtein.getProtein(), evt.getDataContext(), uniprot, fixedProtein.getProtein().getSequence(), evt.getInvalidRangeReport(), fixedProtein, (ProteinUpdateProcessor)evt.getSource(), false);

                // log in 'out_of_date_participant.csv'
                if (evt.getSource() instanceof ProteinUpdateProcessor){
                    ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                    evt.setRemappedProteinAc(fixedProtein.getProtein().getAc());
                    processor.fireOnOutOfDateParticipantFound(evt);
                }

                // we update the created transcript and the protein having range conflicts
                factory.getProteinDao().update((ProteinImpl) fixedProtein.getProtein());
                factory.getProteinDao().update((ProteinImpl) protein);

                return fixedProtein;
            }
            else {
                log.warn("No isoform/feature chain has the same sequence as " + protein.getAc());
            }
        }
        else {
            log.warn("Impossible to remap to an isoform/feature chain with the same sequence as " + protein.getAc() + " because the protein sequence is null");
        }

        // if no uniprot transcript has the same sequence and creating deprecated proteins is enabled, we create a deprecated protein, otherwise return null
        if (createDeprecatedParticipant){
            ProteinTranscript fixedProtein = createDeprecatedProtein(evt);

            // If we had invalid ranges, we will fix them. If we had out of date ranges, we just log them
            rangeFixer.processInvalidRanges(fixedProtein.getProtein(), evt.getDataContext(), uniprot, fixedProtein.getProtein().getSequence(), evt.getInvalidRangeReport(), fixedProtein, (ProteinUpdateProcessor)evt.getSource(), false);

            // log in 'out_of_date_participant.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnOutOfDateParticipantFound(evt);
            }

            // we update the created transcript and the protein having range conflicts
            factory.getProteinDao().update((ProteinImpl) fixedProtein.getProtein());
            factory.getProteinDao().update((ProteinImpl) protein);

            return fixedProtein;
        }
        // impossible to fix the conflict.
        else {
            rangeFixer.processInvalidRanges(protein, evt.getDataContext(), uniprot, protein.getSequence(), evt.getInvalidRangeReport(), null, (ProteinUpdateProcessor)evt.getSource(), fixOutOfDateRanges);

            // log in 'out_of_date_participant.csv'
            if (evt.getSource() instanceof ProteinUpdateProcessor){
                ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();

                processor.fireOnOutOfDateParticipantFound(evt);
            }
        }

        return null;
    }

    /**
     * Move components from the protein with conflicts to the intact splice variant if possible
     * @param context : the context
     * @param protein : the protein
     * @param sequenceWithoutConflicts
     * @param possibleMatch
     * @param proteinTranscripts
     * @return Protein transcript in intact matching the uniprot transcript and having the exact same sequence, null otherwise
     */
    private ProteinTranscript findAndMoveInteractionsToIntactProteinTranscript(DataContext context, Protein protein, String sequenceWithoutConflicts, UniprotProteinTranscript possibleMatch, Collection<ProteinTranscript> proteinTranscripts, ProteinUpdateProcessor processor) {
        for (ProteinTranscript p : proteinTranscripts){
            if (possibleMatch.equals(p.getUniprotVariant()) && !ProteinTools.isSequenceChanged(p.getProtein().getSequence(), sequenceWithoutConflicts) && !protein.getAc().equals(p.getProtein().getAc())){
                ProteinTools.moveInteractionsBetweenProteins(p.getProtein(), protein, context, processor, possibleMatch.getPrimaryAc());
                return p;
            }
        }

        return null;
    }

    /**
     *
     * @param evt : evt containing proteins having feature conflicts and components with the feature conflicts
     * @return  a deprecated protein (associated with no transcript)
     */
    public ProteinTranscript createDeprecatedProtein(OutOfDateParticipantFoundEvent evt){

        DaoFactory factory = evt.getDataContext().getDaoFactory();

        // move the components with range conflicts to the deprecated protein
        Collection<Component> componentsToFix = evt.getComponentsToFix();
        Protein protein = evt.getProteinWithConflicts();

        // create a deprecated protein
        Protein noUniprotUpdate = cloneDeprecatedProtein(factory, protein);

        // add no-uniprot-update and caution
        addAnnotations(noUniprotUpdate, factory);

        for (Component component : componentsToFix){
            protein.removeActiveInstance(component);
            noUniprotUpdate.addActiveInstance(component);
            component.setInteractorAc(noUniprotUpdate.getAc());
            factory.getComponentDao().update(component);
        }

        // log in created.csv
        if (evt.getSource() instanceof ProteinUpdateProcessor){
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            ProteinEvent protEvt = new ProteinEvent(processor, evt.getDataContext(), noUniprotUpdate, "The protein is a deprecated protein which needed to be created possibly because of range conflicts.");

            processor.fireOnProteinCreated(protEvt);
            processor.fireNonUniprotProteinFound(protEvt);

            evt.setRemappedProteinAc(noUniprotUpdate.getAc());
        }

        return new ProteinTranscript(noUniprotUpdate, null);
    }

    @Override
    public RangeFixer getRangeFixer() {
        return this.rangeFixer;
    }

    public void setRangeFixer(RangeFixer rangeFixer) {
        this.rangeFixer = rangeFixer;
    }

    /**
     *
     * @param factory
     * @param protein : the protein to clone
     * @return a new Protein having same biosource, xrefs, sequence, aliases as the protein to clone
     */
    private Protein cloneDeprecatedProtein(DaoFactory factory, Protein protein) {
        Protein created = new ProteinImpl(protein.getOwner(), protein.getBioSource(), protein.getShortLabel() +"_deprecated", protein.getCvInteractorType());

        factory.getProteinDao().persist((ProteinImpl) created);

        for (InteractorAlias a : protein.getAliases()){
            InteractorAlias copy = new InteractorAlias(a.getOwner(), created, a.getCvAliasType(), a.getName());
            copy.setParent(created);
            factory.getAliasDao(InteractorAlias.class).persist(copy);
            created.addAlias(copy);
        }

        for (InteractorXref x : protein.getXrefs()){
            InteractorXref copy = new InteractorXref(x.getOwner(), x.getCvDatabase(), x.getPrimaryId(), x.getCvXrefQualifier());
            copy.setParent(created);
            copy.setParentAc(created.getAc());
            factory.getXrefDao(InteractorXref.class).persist(copy);
            created.addXref(copy);
        }

        created.setCrc64(protein.getCrc64());
        created.setSequence(protein.getSequence());

        return created;
    }

    /**
     *
     * @param factory
     * @param protein
     * @param primaryAc
     * @return a new Protein transcript having same biosource and sequence as the protein to clone
     */
    private Protein cloneProteinForTranscript(DaoFactory factory, Protein protein, String primaryAc) {
        Protein created = new ProteinImpl(protein.getOwner(), protein.getBioSource(), protein.getShortLabel()+"_clone", protein.getCvInteractorType());

        InteractorXref identity = ProteinUtils.getUniprotXref(protein);

        InteractorXref copy = new InteractorXref(protein.getOwner(), identity.getCvDatabase(), primaryAc, identity.getCvXrefQualifier());
        created.addXref(copy);

        created.setCrc64(protein.getCrc64());
        created.setSequence(protein.getSequence());

        factory.getProteinDao().persist((ProteinImpl) created);
        return created;
    }

    /**
     * Add no-uniprot-update and caution to the protein
     * @param protein
     * @param factory
     */
    private void addAnnotations(Protein protein, DaoFactory factory){

        CvTopic no_uniprot_update = factory.getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.NON_UNIPROT);

        if (no_uniprot_update == null){
            no_uniprot_update = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, null, CvTopic.NON_UNIPROT);
            factory.getCvObjectDao(CvTopic.class).persist(no_uniprot_update);
        }
        CvTopic caution = factory.getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(protein.getOwner(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            factory.getCvObjectDao(CvTopic.class).persist(caution);
        }

        boolean has_no_uniprot_update = false;
        boolean has_caution = false;

        for (Annotation annotation : protein.getAnnotations()){
            if (no_uniprot_update.equals(annotation.getCvTopic())){
                has_no_uniprot_update = true;
            }
            else if (caution.equals(annotation.getCvTopic())){
                if (annotation.getAnnotationText() != null){
                    if (annotation.getAnnotationText().equalsIgnoreCase(FEATURE_OBSOLETE)){
                        has_caution = true;
                    }
                }
            }
        }
        AnnotationDao annotationDao = factory.getAnnotationDao();

        if (!has_no_uniprot_update){
            Annotation no_uniprot = new Annotation(no_uniprot_update, null);
            annotationDao.persist(no_uniprot);

            protein.addAnnotation(no_uniprot);
        }

        if (!has_caution){
            Annotation demerge = new Annotation(caution, FEATURE_OBSOLETE);
            annotationDao.persist(demerge);

            protein.addAnnotation(demerge);
        }
    }
}
