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
package uk.ac.ebi.intact.util.protein.utils;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.Xref;

/**
 * Contains a summary of the xrefs update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public final class XrefUpdaterReport {

    private Protein protein;
    private Xref[] addedXrefs;
    private Xref[] removedXrefs;

    public XrefUpdaterReport(Protein protein, Xref[] addedXrefs, Xref[] removedXrefs) {
        this.protein = protein;
        this.addedXrefs = addedXrefs;
        this.removedXrefs = removedXrefs;
    }

    public Protein getProtein() {
        return protein;
    }

    public boolean isUpdated() {
        return (addedXrefs != null && addedXrefs.length > 0) ||
               (removedXrefs != null && removedXrefs.length > 0);
    }

    public Xref[] getAddedXrefs() {
        return addedXrefs;
    }

    public Xref[] getRemovedXrefs() {
        return removedXrefs;
    }

    @Override
    public String toString() {
        return protein.getAc()+" [Added xrefs:"+ xrefsToString(addedXrefs)+" / Removed xrefs: "+ xrefsToString(removedXrefs)+"]";
    }

    public String addedXrefsToString() {
        return xrefsToString(addedXrefs);
    }

    public String removedXrefsToString() {
        return xrefsToString(addedXrefs);
    }

    protected static String xrefsToString(Xref[] xrefs) {
        StringBuilder sb = new StringBuilder();

        for (int i=0; i<xrefs.length; i++) {
            Xref xref = xrefs[i];

            if (i > 0) {
                sb.append(", ");
            }

            String qual = (xref.getCvXrefQualifier() != null)? "("+xref.getCvXrefQualifier().getShortLabel()+")" : "";

            sb.append(xref.getCvDatabase().getShortLabel()+":"+xref.getPrimaryId()+qual);
        }

        return sb.toString();
    }
}
