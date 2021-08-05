package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.Xref;

import java.util.Collection;

/**
 * Contains a summary of the xrefs update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public final class XrefUpdaterReport {

    private final String protein;
    private final Collection<Xref> addedXrefs;
    private final Collection<Xref> removedXrefs;

    public XrefUpdaterReport(Protein protein, Collection<Xref> addedXrefs, Collection<Xref> removedXrefs) {
        this.protein = protein != null ? protein.getAc() : null;

        this.addedXrefs = addedXrefs;
        this.removedXrefs = removedXrefs;
    }

    public String getProtein() {
        return protein;
    }

    public boolean isUpdated() {
        return (addedXrefs != null && addedXrefs.size() > 0) ||
               (removedXrefs != null && removedXrefs.size() > 0);
    }

    public Collection<Xref> getAddedXrefs() {
        return addedXrefs;
    }

    public Collection<Xref> getRemovedXrefs() {
        return removedXrefs;
    }

    @Override
    public String toString() {
        return protein+" [Added xrefs:"+ xrefsToString(addedXrefs)+" / Removed xrefs: "+ xrefsToString(removedXrefs)+"]";
    }

    public String addedXrefsToString() {
        return xrefsToString(addedXrefs);
    }

    public String removedXrefsToString() {
        return xrefsToString(removedXrefs);
    }

    protected static String xrefsToString(Collection<Xref> xrefs) {
        StringBuilder sb = new StringBuilder();
         int i = 1;
        for (Xref xref : xrefs) {

            if (i < xrefs.size()) {
                sb.append(", ");
            }

            String qual = (xref.getCvXrefQualifier() != null)? "("+xref.getCvXrefQualifier().getShortLabel()+")" : "";

            sb.append(xref.getCvDatabase().getShortLabel()).append(":").append(xref.getPrimaryId()).append(qual);

            i++;
        }

        return sb.toString();
    }
}