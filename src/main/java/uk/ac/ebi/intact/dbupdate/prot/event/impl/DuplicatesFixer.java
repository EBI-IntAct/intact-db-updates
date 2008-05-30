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
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.core.persister.PersisterHelper;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.AbstractProteinUpdateProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.MultiProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.XrefUtils;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.util.DebugUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Duplicate detection for proteins.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class DuplicatesFixer extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( DuplicatesFixer.class );

    @Override
    public void onProteinDuplicationFound(MultiProteinEvent evt) throws ProcessorException {
        mergeDuplicates(evt.getProteins(), evt);
    }

    protected void mergeDuplicates(Collection<Protein> duplicates, MultiProteinEvent evt) {
        if (log.isDebugEnabled()) log.debug("Merging duplicates: "+ DebugUtil.acList(duplicates));

        // add the interactions from the duplicated proteins to the protein
        // that was created first in the database

        // calculate the original protein
        Protein originalProt = calculateOriginalProtein(new ArrayList<Protein>(duplicates));
        evt.setReferenceProtein(originalProt);

        // move the interactions from the rest of proteins to the original
        for (Protein duplicate : duplicates) {
            // don't process the original protein with itself
            if (!duplicate.getAc().equals(originalProt.getAc())) {
                ProteinTools.moveInteractionsBetweenProteins(originalProt, duplicate);
                List<InteractorXref> copiedXrefs = ProteinTools.copyNonIdentityXrefs(originalProt, duplicate);

                for (InteractorXref copiedXref : copiedXrefs) {
                    DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
                    daoFactory.getXrefDao(InteractorXref.class).persist(copiedXref);
                }

                // create an "intact-secondary" xref to the protein to be kept.
                // This will allow the user to search using old ACs
                Institution owner = duplicate.getOwner();
                InstitutionXref psiMiXref = XrefUtils.getPsiMiIdentityXref(owner);
                if (psiMiXref != null) {
                    CvDatabase db = psiMiXref.getCvDatabase();

                    CvXrefQualifier intactSecondary = CvObjectUtils.createCvObject(owner, CvXrefQualifier.class, null, "intact-secondary");
                    PersisterHelper.saveOrUpdate(intactSecondary);
                    InteractorXref xref = new InteractorXref(owner, db, duplicate.getAc(), intactSecondary);
                    originalProt.addXref(xref);
                }

                // and delete the duplicate
                if (duplicate.getActiveInstances().isEmpty()) {
                    deleteProtein(duplicate, new ProteinEvent(evt.getSource(), evt.getDataContext(), duplicate, "Duplicate of "+originalProt.getAc()));
                } else {
                    throw new IllegalStateException("Attempt to delete a duplicate that still contains interactions: "+protInfo(duplicate));
                }
            }
        }

    }

    protected static Protein calculateOriginalProtein(List<? extends Protein> duplicates) {
        Protein originalProt = duplicates.get(0);

        for (int i = 1; i < duplicates.size(); i++) {
            Protein duplicate =  duplicates.get(i);

            if (duplicate.getCreated().before(originalProt.getCreated())) {
                originalProt = duplicate;
            }
        }
        
        return originalProt;
    }

    private void deleteProtein(Protein protein, ProteinEvent evt) {
        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
        processor.fireOnDelete(new ProteinEvent(evt.getSource(), evt.getDataContext(), protein, evt.getMessage()));
    }
}
