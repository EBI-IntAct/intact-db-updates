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

import java.util.ArrayList;
import java.util.Collection;

/**
 * Contains a summary of the xrefs update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public final class XrefUpdaterReport {

    private String protein;
    private Collection<Xref> addedXrefs;
    private Collection<Xref> removedXrefs;

    public XrefUpdaterReport(Protein protein, Xref[] addedXrefs, Xref[] removedXrefs) {
        this.protein = protein != null ? protein.getAc() : null;

        this.addedXrefs  = new ArrayList<Xref>();
        this.removedXrefs = new ArrayList<Xref>();

        for (Xref ref : addedXrefs){
            this.addedXrefs.add(ref);
        }
        for (Xref ref : removedXrefs){
            this.removedXrefs.add(ref);
        }
    }

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

            sb.append(xref.getCvDatabase().getShortLabel()+":"+xref.getPrimaryId()+qual);

            i++;
        }

        return sb.toString();
    }
}
