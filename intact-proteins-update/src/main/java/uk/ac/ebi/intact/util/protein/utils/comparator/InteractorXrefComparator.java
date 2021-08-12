package uk.ac.ebi.intact.util.protein.utils.comparator;

import uk.ac.ebi.intact.model.*;

import java.util.Comparator;

/**
 * Comparator for interactor xrefs
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/11/11</pre>
 */

public class InteractorXrefComparator implements Comparator<InteractorXref>{

    private final static Comparator<String> nullSafeStringComparator = Comparator
            .nullsFirst(String::compareToIgnoreCase);

    private final static Comparator<CvDatabase> cvDatabaseComparator = Comparator
            .comparing(CvDatabase::getIdentifier, nullSafeStringComparator);

    private final static Comparator<CvXrefQualifier> cvQualifierComparator = Comparator
            .comparing(CvXrefQualifier::getIdentifier, nullSafeStringComparator);

    private final static Comparator<InteractorXref> interactorXrefComparator = Comparator
            .comparing(InteractorXref::getCvDatabase, cvDatabaseComparator)
            .thenComparing(InteractorXref::getPrimaryId, nullSafeStringComparator)
            .thenComparing(InteractorXref::getSecondaryId, nullSafeStringComparator)
            .thenComparing(InteractorXref::getCvXrefQualifier, cvQualifierComparator);

    @Override
    public int compare(InteractorXref o1, InteractorXref o2) {
        return interactorXrefComparator.compare(o1, o2);
    }
}