package uk.ac.ebi.intact.dbupdate.prot.util;

import uk.ac.ebi.intact.commons.util.DiffUtils;
import uk.ac.ebi.intact.commons.util.diff.Diff;
import uk.ac.ebi.intact.commons.util.diff.Operation;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.model.*;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Helper containing methods for handling proteins
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinTools {

    private ProteinTools() {}

    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Collection<? extends Protein> sourceProteins) {
        for (Protein sourceProtein : sourceProteins) {
            moveInteractionsBetweenProteins(destinationProtein, sourceProtein);
        }
    }

    /**
     * Move the interactions attached to the source protein to the destination protein
     * @param destinationProtein : protein where to move the interactions
     * @param sourceProtein : the protein for what we want to move the interactions
     */
    public static void moveInteractionsBetweenProteins(Protein destinationProtein, Protein sourceProtein) {
        for (Component component : sourceProtein.getActiveInstances()) {
            component.setInteractor(destinationProtein);
            IntactContext.getCurrentInstance().getDaoFactory().getComponentDao().update(component);
        }
        destinationProtein.getActiveInstances().addAll(sourceProtein.getActiveInstances());
        sourceProtein.getActiveInstances().clear();
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
}
