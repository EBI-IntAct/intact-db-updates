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
package uk.ac.ebi.intact.dbupdate.prot.listeners;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.context.DataContext;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.DuplicatesFoundEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.persistence.dao.ProteinDao;

import java.util.ArrayList;
import java.util.List;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFinder extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        DataContext dataContext = evt.getDataContext();
        Protein protein = evt.getProtein();

        if (protein.getAc() == null) {
            throw new IllegalArgumentException("We need an AC to check for duplicates: "+protInfo(protein));
        }

        if (log.isDebugEnabled()) log.debug("Searching for duplicates in the database for: "+protInfo(protein));

        // ignore proteins that cannot be updated from uniprot
        if (!ProteinUtils.isFromUniprot(protein)) {
            if (log.isDebugEnabled()) log.debug("Duplicate search ignored for proteins that do not exist in UniprotKb: "+protInfo(protein));
            return;
        }

        // ignore splice variants for now, as they follow additional criteria to distinguish between them
        if (ProteinUtils.isSpliceVariant(protein)) {
            if (log.isDebugEnabled()) log.debug("Duplicate search ignored for splice variant: "+protInfo(protein));
            return;
        }

        ProteinDao proteinDao = dataContext.getDaoFactory().getProteinDao();

        // param checks
        if (protein.getCrc64() == null) {
            if (log.isErrorEnabled()) log.error("Protein without CRC64 found: "+protInfo(protein));
            return;
        }

        if (protein.getBioSource() == null) {
            if (log.isErrorEnabled()) log.error("Protein without BioSource: "+protInfo(protein));
            return;
        }

        // create a list of possible duplicates, by retrieving from the database
        // those proteins with the same CRC64 and organism - then remove the processed protein
        // from the results
        List<ProteinImpl> possibleDuplicates = proteinDao.getByCrcAndTaxId(protein.getCrc64(), protein.getBioSource().getTaxId());

        // if there are possible duplicates (more than 1 result), check and fix when necessary
        if (possibleDuplicates.size() > 1) {
            checkAndFixDuplication(possibleDuplicates, evt);
        } else {
            if (log.isDebugEnabled()) log.debug("No duplicates found for: "+protInfo(protein));
        }
    }

    private void checkAndFixDuplication(List<? extends Protein> possibleDuplicates, ProteinEvent evt) {
        List<Protein> realDuplicates = new ArrayList<Protein>();

        Protein firstProtein = possibleDuplicates.get(0);

        for (int i = 1; i < possibleDuplicates.size(); i++) {
            Protein possibleDuplicate =  possibleDuplicates.get(i);

            if (ProteinUtils.containTheSameIdentities(firstProtein, possibleDuplicate)) {
                if (realDuplicates.isEmpty()) realDuplicates.add(firstProtein);
                realDuplicates.add(possibleDuplicate);
            }
        }

        if (!realDuplicates.isEmpty()) {
            // fire a duplication event
            final ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnProteinDuplicationFound(new DuplicatesFoundEvent(processor, evt.getDataContext(), realDuplicates));
        }
    }
}