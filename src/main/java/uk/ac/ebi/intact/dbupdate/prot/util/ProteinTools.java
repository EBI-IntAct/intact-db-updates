package uk.ac.ebi.intact.dbupdate.prot.util;

import uk.ac.ebi.intact.commons.util.DiffUtils;
import uk.ac.ebi.intact.commons.util.diff.Diff;
import uk.ac.ebi.intact.commons.util.diff.Operation;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.AnnotatedObjectUtils;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;

import java.util.*;

/**
 * Helper containing methods for handling proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinTools {

    private ProteinTools() {}

    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Collection<? extends Protein> sourceProteins, DaoFactory factory) {
        for (Protein sourceProtein : sourceProteins) {
            moveInteractionsBetweenProteins(destinationProtein, sourceProtein, factory);
        }
    }

    /**
     * Move the interactions attached to the source protein to the destination protein
     * @param destinationProtein : protein where to move the interactions
     * @param sourceProtein : the protein for what we want to move the interactions
     */
    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Protein sourceProtein, DaoFactory factory) {
        List<Component> componentsToMove = new ArrayList<Component>(sourceProtein.getActiveInstances());
        for (Component component : componentsToMove) {
            sourceProtein.removeActiveInstance(component);
            destinationProtein.addActiveInstance(component);
            factory.getComponentDao().update(component);
        }

        factory.getProteinDao().update((ProteinImpl) sourceProtein);
        factory.getProteinDao().update((ProteinImpl) destinationProtein);
    }

    /**
     * Copy the non identity cross reference from a source protein to a destination protein
     * @param destinationProtein
     * @param sourceProtein
     * @return the list of cross references we copied
     */
    public static List<InteractorXref> copyNonIdentityXrefs(Protein destinationProtein, Protein sourceProtein) {
        List<InteractorXref> copied = new ArrayList<InteractorXref>();

        for (InteractorXref xref : sourceProtein.getXrefs()) {

            if (xref.getCvXrefQualifier() != null && CvXrefQualifier.IDENTITY_MI_REF.equals(xref.getCvXrefQualifier().getIdentifier())) {
                continue;
            }
            if (!destinationProtein.getXrefs().contains(xref)) {
                final InteractorXref clonedXref = new InteractorXref(xref.getOwner(), xref.getCvDatabase(),
                        xref.getPrimaryId(), xref.getSecondaryId(),
                        xref.getDbRelease(), xref.getCvXrefQualifier());

                destinationProtein.addXref(clonedXref);
                copied.add(xref);
            }
        }

        return copied;
    }

    /**
     * Calculates an index which can be used to measure the amount of differences between
     * two sequences. The value is goes from 0 (sequences completely different) to 1 (sequence exactly the same).
     * This calculation uses a traditional diff algorithm to estimate the changes.
     * @param oldSeq Sequence A
     * @param newSeq Sequence B
     * @return The value
     */
    public static double calculateSequenceConservation(String oldSeq, String newSeq) {
        List<Diff> diffs = DiffUtils.diff(oldSeq, newSeq);

        // we count the amount of aminoacids included in the changes
        int equalAminoacidCount = 0;

        for (Diff diff : diffs) {
            if (diff.getOperation() == Operation.EQUAL) {
                equalAminoacidCount += diff.getText().length();
            }
        }

        // this parameter measures how equal the sequences are ( 0 <= relativeConservation <= 1)
        double relativeConservation = (double) equalAminoacidCount / oldSeq.length();
        return relativeConservation;
    }

    /**
     *
     * @param prot
     * @return the set of distinct uniprot identities attached to this protein
     */
    public static Set<InteractorXref> getDistinctUniprotIdentities(Protein prot){
        Set<InteractorXref> uniprotIdentities = new HashSet<InteractorXref>();

        for (InteractorXref ref : prot.getXrefs()){
            CvDatabase database = ref.getCvDatabase();

            if (database != null){
                if (database.getIdentifier().equals(CvDatabase.UNIPROT_MI_REF)){
                    CvXrefQualifier qualifier = ref.getCvXrefQualifier();
                    if (qualifier != null){
                        if (qualifier.getIdentifier().equals(CvXrefQualifier.IDENTITY_MI_REF)){
                            uniprotIdentities.add(ref);
                        }
                    }
                }
            }
        }

        return uniprotIdentities;
    }

    /**
     *
     * @param prot
     * @return the list of all the uniprot identities attached to this protein
     */
    public static List<InteractorXref> getAllUniprotIdentities(Protein prot){
        final List<InteractorXref> identities = ProteinUtils.getIdentityXrefs( prot );
        List<InteractorXref> uniprotIdentities = new ArrayList<InteractorXref>();

        for (InteractorXref ref : identities){
            CvDatabase database = ref.getCvDatabase();

            if (database != null){
                if (database.getIdentifier().equals(CvDatabase.UNIPROT_MI_REF)){
                    uniprotIdentities.add(ref);
                }
            }
        }

        return uniprotIdentities;
    }

    public static boolean hasUniqueDistinctUniprotIdentity(Protein prot){
        // get the distinct uniprot identities
        final Set<InteractorXref> uniprotIdentities = ProteinTools.getDistinctUniprotIdentities(prot);

        // if several uniprot identities, cannot find duplicates
        if( uniprotIdentities.size() != 1 ) {
            return false;
        }

        return true;
    }

    public static void filterNonUniprotAndMultipleUniprot(Collection<ProteinImpl> primaryProteins) {
        for (Iterator<ProteinImpl> proteinIterator = primaryProteins.iterator(); proteinIterator.hasNext();) {
            ProteinImpl protein = proteinIterator.next();

            if (!ProteinUtils.isFromUniprot(protein)) {
                proteinIterator.remove();
            }
            else if (!ProteinTools.hasUniqueDistinctUniprotIdentity(protein)){
                proteinIterator.remove();
            }
        }
    }

    public static void addIntactSecondaryReferences(Protein original, Protein duplicate, DaoFactory factory){
        // create an "intact-secondary" xref to the protein to be kept.
        // This will allow the user to search using old ACs
        Institution owner = duplicate.getOwner();

        // the database is always intact because the framework is the intact framework and when we merge two proteins of this framework, it becomes 'intact-secondary'
        CvDatabase db = factory.getCvObjectDao( CvDatabase.class ).getByPsiMiRef( CvDatabase.INTACT_MI_REF );

        if (db == null){
            db = CvObjectUtils.createCvObject(owner, CvDatabase.class, CvDatabase.MINT_MI_REF, CvDatabase.INTACT);
            factory.getCvObjectDao(CvDatabase.class).saveOrUpdate(db);
        }

        final String intactSecondaryLabel = "intact-secondary";
        boolean hasIntactSecondary = false;

        CvXrefQualifier intactSecondary = factory.getCvObjectDao(CvXrefQualifier.class).getByShortLabel(intactSecondaryLabel);

        if (intactSecondary == null) {
            intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, intactSecondaryLabel);
            factory.getCvObjectDao(CvXrefQualifier.class).saveOrUpdate(intactSecondary);
        }

        List<String> existingSecondaryAcs = new ArrayList<String>();

        for (InteractorXref ref : original.getXrefs()){
            if (ref.getCvDatabase() != null){
                if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                    if (ref.getCvXrefQualifier() != null){
                        if (ref.getCvXrefQualifier().getShortLabel().equals(intactSecondaryLabel)){
                            if (ref.getPrimaryId().equals(duplicate.getAc())){
                                hasIntactSecondary = true;
                            }
                            else {
                                existingSecondaryAcs.add(ref.getPrimaryId());
                            }
                        }
                    }
                }
            }
        }

        if (!hasIntactSecondary){
            InteractorXref xref = new InteractorXref(owner, db, duplicate.getAc(), intactSecondary);
            factory.getXrefDao(InteractorXref.class).persist(xref);

            original.addXref(xref);
        }

        Collection<InteractorXref> refsToRemove = new ArrayList<InteractorXref>();
        for (InteractorXref ref : duplicate.getXrefs()){
            if (ref.getCvDatabase() != null){
                if (ref.getCvDatabase().getIdentifier().equals(CvDatabase.INTACT_MI_REF)){
                    if (ref.getCvXrefQualifier() != null){
                        if (ref.getCvXrefQualifier().getShortLabel().equals(intactSecondaryLabel)){
                            if (!existingSecondaryAcs.contains(ref.getPrimaryId())){
                                original.addXref(ref);
                                ref.setParentAc(original.getAc());
                                refsToRemove.add(ref);

                                factory.getXrefDao(InteractorXref.class).update(ref);
                            }
                        }
                    }
                }
            }
        }

        duplicate.getXrefs().removeAll(refsToRemove);

        factory.getProteinDao().update((ProteinImpl) duplicate);
        factory.getProteinDao().update((ProteinImpl) original);
    }

    public static void updateProteinTranscripts(DaoFactory factory, Protein originalProt, Protein duplicate) {
        final List<ProteinImpl> isoforms = factory.getProteinDao().getSpliceVariants( duplicate );
        for ( ProteinImpl isoform : isoforms ) {
            // each isoform should now point to the original protein
            final Collection<InteractorXref> isoformParents =
                    AnnotatedObjectUtils.searchXrefs( isoform,
                            CvDatabase.INTACT_MI_REF,
                            CvXrefQualifier.ISOFORM_PARENT_MI_REF );

            remapTranscriptParent(originalProt, duplicate.getAc(), isoformParents, factory);
        }

        final List<ProteinImpl> proteinChains = factory.getProteinDao().getProteinChains( duplicate );
        for ( ProteinImpl chain : proteinChains ) {
            // each chain should now point to the original protein
            final Collection<InteractorXref> chainParents =
                    AnnotatedObjectUtils.searchXrefs(chain,
                            CvDatabase.INTACT_MI_REF,
                            CvXrefQualifier.CHAIN_PARENT_MI_REF );

            remapTranscriptParent(originalProt, duplicate.getAc(), chainParents, factory);
        }
    }

    /**
     * Remap the transcripts attached to this duplicate to the original protein
     * @param originalProt
     * @param transcriptParents
     */
    private static void remapTranscriptParent(Protein originalProt, String duplicateAc, Collection<InteractorXref> transcriptParents, DaoFactory factory) {

        for (InteractorXref xref : transcriptParents){

            if (xref.getPrimaryId().equals(duplicateAc)){
                xref.setPrimaryId( originalProt.getAc() );
                factory.getXrefDao(InteractorXref.class).update(xref);
                break;
            }
        }
    }

    /**
     *
     * @param sequenceDuplicate : sequence of the duplicate
     * @param sequenceUniprot : sequence of the protein in uniprot
     * @return true if the sequence in uniprot is different from the sequence of the duplicate.
     */
    public static boolean isSequenceChanged(String sequenceDuplicate, String sequenceUniprot){
        if ( (sequenceDuplicate == null && sequenceUniprot != null)) {
            return true;
        }
        else if (sequenceDuplicate != null && sequenceUniprot != null){
            if (!sequenceUniprot.equals( sequenceDuplicate ) ){
                return true;
            }
        }

        return false;
    }
}
