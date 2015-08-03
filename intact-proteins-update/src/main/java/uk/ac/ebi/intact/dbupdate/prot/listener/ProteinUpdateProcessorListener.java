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
package uk.ac.ebi.intact.dbupdate.prot.listener;

import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.*;

import java.util.EventListener;

/**
 * Listener for ProteinProcessors
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface ProteinUpdateProcessorListener extends EventListener {

    void onDelete(ProteinEvent evt) throws ProcessorException;

    void onProteinDuplicationFound(DuplicatesFoundEvent evt) throws ProcessorException;

    void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException;

    void onProteinCreated(ProteinEvent evt) throws ProcessorException;

    void onUpdateCase(UpdateCaseEvent evt) throws ProcessorException;

    void onNonUniprotProteinFound(ProteinEvent evt) throws ProcessorException;

    @Deprecated
    void onRangeChanged(RangeChangedEvent evt) throws ProcessorException;

    void onInvalidRange(InvalidRangeEvent evt) throws ProcessorException;

    void onOutOfDateRange(InvalidRangeEvent evt) throws ProcessorException;

    void onDeadProteinFound(DeadUniprotEvent evt) throws ProcessorException;

    void onOutOfDateParticipantFound(OutOfDateParticipantFoundEvent evt) throws ProcessorException;

    void onProcessErrorFound(UpdateErrorEvent evt) throws ProcessorException;

    void onSecondaryAcsFound(UpdateCaseEvent evt) throws ProcessorException;

    void onProteinTranscriptWithSameSequence(ProteinTranscriptWithSameSequenceEvent evt) throws ProcessorException;

    void onInvalidIntactParent(InvalidIntactParentFoundEvent evt) throws ProcessorException;

    void onProteinRemapping(ProteinRemappingEvent evt) throws ProcessorException;

    void onProteinSequenceCaution(ProteinSequenceChangeEvent evt) throws ProcessorException;

    void onDeletedComponent(DeletedComponentEvent evt) throws ProcessorException;
}