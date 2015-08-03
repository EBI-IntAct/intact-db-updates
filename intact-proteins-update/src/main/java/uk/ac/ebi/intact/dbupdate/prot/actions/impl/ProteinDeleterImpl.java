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
package uk.ac.ebi.intact.dbupdate.prot.actions.impl;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.persistence.dao.ProteinDao;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.actions.ProteinDeleter;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;

/**
 * Removes a protein from the database
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinDeleterImpl implements ProteinDeleter{

    /**
     * The logger of this class
     */
    private static final Log log = LogFactory.getLog( ProteinDeleterImpl.class );

    /**
     * Delete the protein from the database
     * @param evt : contains the protein to delete
     * @return true if the protein is deleted from the database, false otherwise
     * @throws ProcessorException
     */
    public boolean delete(ProteinEvent evt) throws ProcessorException {
        boolean isDeleted = deleteProtein(evt.getProtein(), evt);

        if (isDeleted && evt.getSource() instanceof ProteinUpdateProcessor){
            // log in 'deleted.csv'
            ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
            processor.fireOnDelete(evt);
        }

        // delete the protein
        return isDeleted;
    }

    /**
     * Delete the protein if it is not already done
     * @param protein
     * @param evt
     * @return true if the protein is deleted from the database, false otherwise
     */
    private boolean deleteProtein(Protein protein, ProteinEvent evt) {
        if (log.isDebugEnabled()) log.debug("Deleting protein: "+protein.getAc());

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        if (protein.getAc() != null) {

            if (!proteinDao.isTransient((ProteinImpl) protein)) {

                proteinDao.delete((ProteinImpl) protein);
            } else {
                proteinDao.deleteByAc(protein.getAc());
            }
            return true;
        }
        return false;
    }
}
