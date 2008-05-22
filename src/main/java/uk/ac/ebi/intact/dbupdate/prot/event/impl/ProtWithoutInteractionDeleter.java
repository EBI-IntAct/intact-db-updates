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
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.AbstractProteinProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.persistence.dao.ProteinDao;

import java.util.List;

/**
 * Will check if the protein participate in any interaction and if not it will be deleted.
 *
 * If a protein has splice variants, and any of the splice variants have interactions, none
 * of the proteins will be deleted unless <code>deleteSpliceVariantsWithoutInteractions</code> is true, which would remove
 * the splice vars (without interactions) as well.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProtWithoutInteractionDeleter extends AbstractProteinProcessorListener {

    private static final Log log = LogFactory.getLog( ProtWithoutInteractionDeleter.class );

    private boolean deleteSpliceVariantsWithoutInteractions;

    @Override
    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        if (log.isDebugEnabled()) {

            log.debug("Checking if the protein has interactions: "+ protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
        }

        if (protein.getAc() == null) {
            log.debug("Protein without AC, cannot be deleted");
            return;
        }

        ProteinDao proteinDao = evt.getDataContext().getDaoFactory().getProteinDao();

        // check the number of interactions where this protein can be found. If none are found,
        // check if the protein has splice variants as then it cannot be removed
        if (proteinDao.countInteractionsForInteractorWithAc(protein.getAc()) == 0) {

            boolean isSpliceVariant = ProteinUtils.isSpliceVariant(protein);
            boolean containsSpliceVarsWithInteractions = false;

            List<ProteinImpl> spliceVars = proteinDao.getSpliceVariants(protein);

            if (!isSpliceVariant) {
                for (Protein spliceVar : spliceVars) {
                    if (proteinDao.countInteractionsForInteractorWithAc(spliceVar.getAc()) > 0) {
                        containsSpliceVarsWithInteractions = true;
                    } else if (isDeleteSpliceVariantsWithoutInteractions()) {
                        if (log.isDebugEnabled()) log.debug("Splice variant for protein '"+protein.getShortLabel()+"' will be deleted: "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
                        deleteProtein(spliceVar, evt);
                    }
                }
            }
            // if any of the splice variants does not contain interactions, delete the splice variant

            if (!containsSpliceVarsWithInteractions) {
                if (!isSpliceVariant) {
                    if (log.isDebugEnabled()) log.debug("Protein will be deleted (no interactions, no splice variants): "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
                    deleteProtein(protein, evt);

                    // if the master protein does not have interactions, nor any of the splice variants, we remove all of them.
                    // note that the previous algorithm above that iterates spliceVariants only removes the ones without interactions
                    // if the corresponding configuration is set. This iteration, removes all the splice variants for the master prot
                    for (Protein spliceVar : spliceVars) {
                        deleteProtein(spliceVar, evt);
                    }
                    
                    evt.requestFinalization();

                } else if (isDeleteSpliceVariantsWithoutInteractions()) {
                    if (log.isDebugEnabled()) log.debug("Splice variant will be deleted: "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
                    deleteProtein(protein, evt);
                    evt.requestFinalization();
                } else {
                    if (log.isDebugEnabled()) log.debug("Protein is a splice variant (without interactions) and won't be deleted: "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
                }

            } else {
                if (log.isTraceEnabled())
                    log.trace("Protein contains splice variants with interactions, so it won't be deleted: "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
            }
        } else {
            if (log.isTraceEnabled())
                log.trace("Protein contains interactions, so it won't be deleted: "+protein.getShortLabel()+" ("+evt.getProtein().getAc()+")");
        }

    }

    private void deleteProtein(Protein protein, ProteinEvent evt) {
        ProteinProcessor processor = (ProteinProcessor) evt.getSource();
        processor.fireOnDelete(new ProteinEvent(processor, evt.getDataContext(), protein));
    }

    public boolean isDeleteSpliceVariantsWithoutInteractions() {
        return deleteSpliceVariantsWithoutInteractions;
    }

    public void setDeleteSpliceVariantsWithoutInteractions(boolean deleteSpliceVariantsWithoutInteractions) {
        this.deleteSpliceVariantsWithoutInteractions = deleteSpliceVariantsWithoutInteractions;
    }
}
