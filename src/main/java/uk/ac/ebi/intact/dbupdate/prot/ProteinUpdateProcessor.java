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
package uk.ac.ebi.intact.dbupdate.prot;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.transaction.TransactionStatus;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DbInfoDao;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.listener.*;
import uk.ac.ebi.intact.model.meta.DbInfo;
import uk.ac.ebi.kraken.uuw.services.remoting.UniProtJAPI;

import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * Updates the database proteins using the latest information from UniProt.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ProteinUpdateProcessor extends ProteinProcessor {

    private static final Log log = LogFactory.getLog( ProteinUpdateProcessor.class );


    public ProteinUpdateProcessor(){
        super();
    }

    public ProteinUpdateProcessor(ProteinUpdateProcessorConfig configUpdate){
        this();
        ProteinUpdateContext.getInstance().setConfig( configUpdate );
    }

    @Override
    public void updateAll() throws ProcessorException {

        boolean global = ProteinUpdateContext.getInstance().getConfig().isGlobalProteinUpdate();
        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( true );
        
        super.updateAll();

        ProteinUpdateContext.getInstance().getConfig().setGlobalProteinUpdate( global );

        // update db info accordingly
        String lastProtUpdate = new SimpleDateFormat("dd-MMM-yy").format(new Date());

        saveOrUpdateDbInfo("last_protein_update", lastProtUpdate);
        saveOrUpdateDbInfo("uniprotkb.version", UniProtJAPI.factory.getVersion());
    }

    private void saveOrUpdateDbInfo(String key, String value) {
        final TransactionStatus transactionStatus = IntactContext.getCurrentInstance().getDataContext().beginTransaction();

        final IntactContext context = IntactContext.getCurrentInstance();
        DbInfoDao dbInfoDao = context.getDaoFactory().getDbInfoDao();
        DbInfo dbInfo = dbInfoDao.get(key);

        if (dbInfo == null) {
            dbInfo = new DbInfo(key, value);
            dbInfoDao.persist(dbInfo);
        } else {
            dbInfo.setValue(value);
            context.getDaoFactory().getEntityManager().merge(dbInfo);
        }
        
        IntactContext.getCurrentInstance().getDataContext().commitTransaction(transactionStatus);
    }

    protected void registerListeners() {
        addListener(new LoggingProcessorListener());

        boolean forceDeleteOfProteins = false;

        final ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();

        if (config.isFixDuplicates()) {
            addListener(new DuplicatesFinder());
            addListener(new DuplicatesFixer());
            forceDeleteOfProteins = true;
        }

        if (config.isDeleteProtsWithoutInteractions()) {
            ProtWithoutInteractionDeleter deleter = new ProtWithoutInteractionDeleter();
            deleter.setDeleteProteinTranscriptsWithoutInteractions(config.isDeleteProteinTranscriptWithoutInteractions());
            addListener(deleter);
            forceDeleteOfProteins = true;
        }

        if (forceDeleteOfProteins) {
            addListener(new ProteinDeleter());
        }
        
        addListener(new UniprotProteinUpdater(config.getUniprotService(), config.getTaxonomyService()));

        if (config.getReportHandler() != null) {
            addListener(new ReportWriterListener(config.getReportHandler()));
        }

        //addListener(new SequenceChangedListener());
        addListener(new RangeFixer());
    }

    public void fireOnDelete(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onDelete(evt);
        }
    }

    public void fireOnProteinDuplicationFound(DuplicatesFoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinDuplicationFound(evt);
        }
    }

    public void fireOnProteinSequenceChanged(ProteinSequenceChangeEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinSequenceChanged(evt);
        }
    }

    public void fireOnProteinCreated(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onProteinCreated(evt);
        }
    }

    public void fireNonUniprotProteinFound(ProteinEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onNonUniprotProteinFound(evt);
        }
    }

    public void fireOnUpdateCase(UpdateCaseEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onUpdateCase(evt);
        }
    }

    public void fireOnRangeChange(RangeChangedEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onRangeChanged(evt);
        }
    }

    public void fireOnRangeOutOfBound(RangeOutOfBoundEvent evt) {
        for (ProteinUpdateProcessorListener listener : getListeners(ProteinUpdateProcessorListener.class)) {
            listener.onRangeOutOfBound(evt);
        }
    }
}