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
import org.springframework.remoting.RemoteConnectFailureException;
import uk.ac.ebi.intact.bridges.taxonomy.TaxonomyService;
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.ProteinImpl;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.uniprot.model.UniprotProtein;
import uk.ac.ebi.intact.uniprot.service.UniprotService;
import uk.ac.ebi.intact.util.biosource.BioSourceServiceFactory;
import uk.ac.ebi.intact.util.protein.ProteinServiceException;
import uk.ac.ebi.intact.util.protein.ProteinServiceImpl;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;

/**
 * Updates the current protein in the database, using information from uniprot
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UniprotProteinUpdater extends ProteinServiceImpl implements ProteinProcessorListener {
    
    private static final Log log = LogFactory.getLog( UniprotProteinUpdater.class );

    private final int MAX_RETRY_ATTEMPTS = 100;
    private int retryAttempt = 0;

    private ProteinUpdateProcessor proteinProcessor;

    public UniprotProteinUpdater(UniprotService uniprotService, TaxonomyService taxonomyService) {
        super(uniprotService);
        setBioSourceService(BioSourceServiceFactory.getInstance().buildBioSourceService(taxonomyService));
    }

    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        Protein protein = evt.getProtein();

        if (log.isTraceEnabled()) log.trace("Checking if the protein can be updated using UniProt information: "+ protein.getShortLabel()+" ("+protein.getAc()+")");

        if (!ProteinUtils.isFromUniprot(protein)) {

            if (evt.getSource() instanceof ProteinUpdateProcessor) {
                final ProteinUpdateProcessor updateProcessor = (ProteinUpdateProcessor) evt.getSource();
                updateProcessor.fireNonUniprotProteinFound(evt);
            }
            
            if (log.isTraceEnabled()) log.debug("Request finalization, as this protein cannot be updated using UniProt");
                ((ProteinProcessor)evt.getSource()).finalizeAfterCurrentPhase();
        }
    }

    public void onProcess(ProteinEvent evt) throws ProcessorException {
        this.proteinProcessor = (ProteinUpdateProcessor) evt.getSource();

        Protein protToUpdate = evt.getProtein();

        InteractorXref uniprotXref = ProteinUtils.getUniprotXref(protToUpdate);
        safeRetrieve(uniprotXref);

    }

    /**
     * Will retrieve the entry
     * @param uniprotXref
     */
    private void safeRetrieve(InteractorXref uniprotXref) {
        try {
            // retrieve the entry from uniprot
            super.retrieve(uniprotXref.getPrimaryId());
        } catch (RemoteConnectFailureException ce) {
            if (retryAttempt >= MAX_RETRY_ATTEMPTS) {
                throw new ProcessorException("Maximum number of retry attempts reached ("+MAX_RETRY_ATTEMPTS+") for: "+uniprotXref.getPrimaryId());
            }
            retryAttempt++;

            if (log.isErrorEnabled()) log.error("Couldn't connect to Uniprot. Will wait 60 seconds before retrying. (Retry: "+retryAttempt+")");
            try {
                Thread.sleep(60*1000);
                safeRetrieve(uniprotXref);
            } catch (InterruptedException e) {
                throw new ProcessorException("Problem while waiting before retrying for "+uniprotXref.getPrimaryId(), e);
            }

        } catch (Exception e) {
            throw new ProcessorException("Problem trying to update proteins using Uniprot AC: "+uniprotXref.getPrimaryId(), e);
        }
        retryAttempt = 0;
    }

    @Override
    protected void deleteProtein(Protein protein) {
        proteinProcessor.fireOnDelete(new ProteinEvent(proteinProcessor, IntactContext.getCurrentInstance().getDataContext(), protein));
    }

    @Override
    protected void sequenceChanged(Protein protein, String oldSequence) {
        proteinProcessor.fireOnProteinSequenceChanged(new ProteinSequenceChangeEvent(proteinProcessor, IntactContext.getCurrentInstance().getDataContext(), protein, oldSequence));
    }

    @Override
    protected void proteinCreated(Protein protein) {
         proteinProcessor.fireOnProteinCreated(new ProteinEvent(proteinProcessor, IntactContext.getCurrentInstance().getDataContext(), protein));
    }

    @Override
    protected Protein processDuplication(UniprotProtein uniprotProtein, Collection<ProteinImpl> primaryProteins, Collection<ProteinImpl> secondaryProteins) throws ProteinServiceException {
        List<Protein> proteins = new ArrayList<Protein>(primaryProteins.size()+secondaryProteins.size());
        proteins.addAll(primaryProteins);
        proteins.addAll(secondaryProteins);
        DuplicatesFoundEvent event = new DuplicatesFoundEvent(proteinProcessor, IntactContext.getCurrentInstance().getDataContext(), proteins);
        proteinProcessor.fireOnProteinDuplicationFound(event);

        return event.getReferenceProtein();
    }

    @Override
    protected Collection<Protein> processCase(UniprotProtein uniprotProtein, Collection<ProteinImpl> primaryProteins, Collection<ProteinImpl> secondaryProteins) throws ProteinServiceException {
        UpdateCaseEvent event = new UpdateCaseEvent(proteinProcessor, IntactContext.getCurrentInstance().getDataContext(),
                                                    uniprotProtein, primaryProteins, secondaryProteins);

        Collection<Protein> updatedProts = super.processCase(uniprotProtein, primaryProteins, secondaryProteins);
        event.setUniprotServiceResult(getUniprotServiceResult());

        proteinProcessor.fireOnUpdateCase(event);

        return updatedProts;
    }
}
