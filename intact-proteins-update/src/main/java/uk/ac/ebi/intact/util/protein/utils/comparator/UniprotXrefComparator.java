package uk.ac.ebi.intact.util.protein.utils.comparator;

import uk.ac.ebi.intact.uniprot.model.UniprotXref;

import java.util.Comparator;

/**
 * Comparator for uniprot xref
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>18/11/11</pre>
 */

public class UniprotXrefComparator implements Comparator<UniprotXref>{

    private final static Comparator<String> nullSafeStringComparator = Comparator
            .nullsFirst(String::compareToIgnoreCase);

    private final static Comparator<UniprotXref> uniprotXrefComparator = Comparator
            .comparing(UniprotXref::getDatabase, nullSafeStringComparator)
            .thenComparing(UniprotXref::getAccession, nullSafeStringComparator)
            .thenComparing(UniprotXref::getDescription, nullSafeStringComparator)
            .thenComparing(UniprotXref::getQualifier, nullSafeStringComparator)
            .thenComparing(UniprotXref::getIsoformId, nullSafeStringComparator);

    @Override
    public int compare(UniprotXref o1, UniprotXref o2) {
        return uniprotXrefComparator.compare(o1, o2);
    }
}