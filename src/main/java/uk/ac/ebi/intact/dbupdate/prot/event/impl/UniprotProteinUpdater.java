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
package uk.ac.ebi.intact.dbupdate.prot.event.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.prot.event.AbstractProteinProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.service.UniprotRemoteService;

/**
 * Updates the current protein in the database, using information from uniprot
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UniprotProteinUpdater extends AbstractProteinProcessorListener {
    
    private static final Log log = LogFactory.getLog( UniprotProteinUpdater.class );
    
    @Override
    public void onPreProcess(ProteinEvent evt) {
        Protein protein = evt.getProtein();

        if (log.isTraceEnabled()) log.trace("Checking if the protein can be updated using UniProt information: "+ protein.getShortLabel()+" ("+protein.getAc()+")");

        if (!ProteinUtils.isFromUniprot(protein)) {
            if (log.isTraceEnabled()) log.trace("Request finalization, as this protein cannot be updated using UniProt");
            evt.requestFinalization();
        }
    }

    @Override
    public void onProcess(ProteinEvent evt) {
        Protein protToUpdate = evt.getProtein();

        UniprotRemoteService uniprotService = new UniprotRemoteService();
        //uniprotService.retrieve();

        // TODO detect dupes now or before?
    }
}
