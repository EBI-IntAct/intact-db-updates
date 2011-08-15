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
package uk.ac.ebi.intact.dbupdate.prot.event;

import uk.ac.ebi.intact.core.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.RangeUpdateReport;
import uk.ac.ebi.intact.dbupdate.prot.util.AdditionalInfoMap;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;
import java.util.HashMap;
import java.util.Map;
import java.util.Set;

/**
 * Fired when duplicates are found
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFoundEvent extends MultiProteinEvent {

    /**
     * The following map stores the number of interactions for all the proteins (using ACs as key)
     */
    private AdditionalInfoMap<Integer> originalActiveInstancesCount;

    private String uniprotSequence;

    private String uniprotCrc64;

    private String primaryUniprotAc;

    private String uniprotTaxId;

    private Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts = new HashMap<Protein, RangeUpdateReport>();
    private boolean hasShiftedRanges;

    private Map<String, Set<String>> movedInteractions = new HashMap<String, Set<String>>();
    private Map<String, Collection<String>> updatedTranscripts = new HashMap<String, Collection<String>>();
    private Map<String, Collection<InteractorXref>> movedXrefs = new HashMap<String, Collection<InteractorXref>>();
    private Map<String, Collection<Annotation>> addedAnnotations = new HashMap<String, Collection<Annotation>>();

    private Map<String, RangeUpdateReport> updatedRanges = new HashMap<String, RangeUpdateReport>();

    /**
     * An event involving a list of proteins.
     */
    public DuplicatesFoundEvent(Object source, DataContext dataContext, Collection<Protein> proteins, String uniprotSequence, String uniprotCrc64, String primaryAc, String uniprotTaxId) {
        super(source, dataContext, proteins);

        this.originalActiveInstancesCount = new AdditionalInfoMap<Integer>();

        for (Protein prot : proteins) {
            originalActiveInstancesCount.put(prot.getAc(), prot.getActiveInstances().size());
        }
        this.uniprotSequence = uniprotSequence;
        this.uniprotCrc64 = uniprotCrc64;
        this.primaryUniprotAc = primaryAc;
        this.uniprotTaxId = uniprotTaxId;
    }

    public AdditionalInfoMap<Integer> getOriginalActiveInstancesCount() {
        return originalActiveInstancesCount;
    }

    public int getInteractionCountForProtein(String ac) {
        int count = 0;

        if (originalActiveInstancesCount.containsKey(ac)) {
            count = originalActiveInstancesCount.get(ac);
        }

        return count;
    }

    public String getUniprotTaxId() {
        return uniprotTaxId;
    }

    public String getUniprotSequence() {
        return uniprotSequence;
    }

    public String getUniprotCrc64() {
        return uniprotCrc64;
    }

    public String getPrimaryUniprotAc() {
        return primaryUniprotAc;
    }

    public Map<Protein, RangeUpdateReport> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public void setComponentsWithFeatureConflicts(Map<Protein, RangeUpdateReport> componentsWithFeatureConflicts) {
        this.componentsWithFeatureConflicts = componentsWithFeatureConflicts;
    }

    public boolean hasShiftedRanges() {
        return hasShiftedRanges;
    }

    public void setHasShiftedRanges(boolean hasShiftedRanges) {
        this.hasShiftedRanges = hasShiftedRanges;
    }

    public Map<String, Set<String>> getMovedInteractions() {
        return movedInteractions;
    }

    public void setMovedInteractions(Map<String, Set<String>> movedInteractions) {
        this.movedInteractions = movedInteractions;
    }

    public Map<String, Collection<String>> getUpdatedTranscripts() {
        return updatedTranscripts;
    }

    public void setUpdatedTranscripts(Map<String, Collection<String>> updatedTranscripts) {
        this.updatedTranscripts = updatedTranscripts;
    }

    public Map<String, Collection<InteractorXref>> getMovedXrefs() {
        return movedXrefs;
    }

    public void setMovedXrefs(Map<String, Collection<InteractorXref>> movedXrefs) {
        this.movedXrefs = movedXrefs;
    }

    public Map<String, Collection<Annotation>> getAddedAnnotations() {
        return addedAnnotations;
    }

    public void setAddedAnnotations(Map<String, Collection<Annotation>> addedAnnotations) {
        this.addedAnnotations = addedAnnotations;
    }

    public Map<String, RangeUpdateReport> getUpdatedRanges() {
        return updatedRanges;
    }
}
