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
import uk.ac.ebi.intact.dbupdate.prot.event.MultiProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinProcessorListener;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.util.DebugUtil;

/**
 * TODO comment that class header
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class LoggingProcessorListener implements ProteinProcessorListener {

    private static final Log log = LogFactory.getLog( LoggingProcessorListener.class );

    public void onPreProcess(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Pre-processing protein: "+evt.getProtein().getShortLabel());
    }

    public void onProcess(ProteinEvent evt) throws ProcessorException {
       if (log.isDebugEnabled()) log.debug("Processing protein: "+evt.getProtein().getShortLabel());
    }

    public void onDelete(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Deleted protein: "+evt.getProtein().getShortLabel());
    }

    public void onProteinDuplicationFound(MultiProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Duplicated proteins: "+ DebugUtil.acList(evt.getProteins()));
    }

    public void onDeadProteinFound(ProteinEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Dead protein found: "+evt.getProtein().getShortLabel());
    }

    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        if (log.isDebugEnabled()) log.debug("Sequence for protein has changed: "+evt.getProtein().getShortLabel());
    }
}
