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
import uk.ac.ebi.intact.dbupdate.prot.DuplicateReport;
import uk.ac.ebi.intact.dbupdate.prot.util.AdditionalInfoMap;
import uk.ac.ebi.intact.model.Protein;

import java.util.Collection;

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

    private DuplicateReport duplicateReport;

    /**
     * An event involving a list of proteins.
     */
    public DuplicatesFoundEvent(Object source, DataContext dataContext, Collection<Protein> proteins, String uniprotSequence, String uniprotCrc64) {
        super(source, dataContext, proteins);

        this.originalActiveInstancesCount = new AdditionalInfoMap<Integer>();

        for (Protein prot : proteins) {
            originalActiveInstancesCount.put(prot.getAc(), prot.getActiveInstances().size());
        }
        this.uniprotSequence = uniprotSequence;
        this.uniprotCrc64 = uniprotCrc64;
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

    public String getUniprotSequence() {
        return uniprotSequence;
    }

    public String getUniprotCrc64() {
        return uniprotCrc64;
    }

    public DuplicateReport getDuplicateReport() {
        return duplicateReport;
    }

    public void setDuplicateReport(DuplicateReport duplicateReport) {
        this.duplicateReport = duplicateReport;
    }
}
