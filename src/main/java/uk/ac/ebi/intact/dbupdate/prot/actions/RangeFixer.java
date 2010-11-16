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
package uk.ac.ebi.intact.dbupdate.prot.actions;

import org.apache.commons.collections.CollectionUtils;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.*;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.InvalidRange;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.*;

/**
 * Listens for sequence changes and updates the ranges of the features.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeFixer {

    private static final Log log = LogFactory.getLog( RangeFixer.class );
    private RangeChecker checker;

    public RangeFixer(){
        this.checker = new RangeChecker();
    }

    private void shiftRanges(String oldSequence, String newSequence, Collection<Component> componentsToUpdate, ProteinUpdateProcessor processor) throws ProcessorException {
        RangeChecker rangeChecker = new RangeChecker();

        if (oldSequence != null && newSequence != null) {

            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

                    Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, oldSequence, newSequence);

                    // fire the events for the range changes
                    for (UpdatedRange updatedRange : updatedRanges) {
                        processor.fireOnRangeChange(new RangeChangedEvent(IntactContext.getCurrentInstance().getDataContext(), updatedRange));
                    }
                }
            }
        }
        else if (oldSequence == null && newSequence != null){
            for (Component component : componentsToUpdate) {
                for (Feature feature : component.getBindingDomains()) {

                    Collection<UpdatedRange> updatedRanges = rangeChecker.prepareFeatureSequences(feature, newSequence);

                    // fire the events for the range changes
                    for (UpdatedRange updatedRange : updatedRanges) {
                        processor.fireOnRangeChange(new RangeChangedEvent(IntactContext.getCurrentInstance().getDataContext(), updatedRange));
                    }
                }
            }
        }
    }

    public void fixInvalidRanges(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String message = evt.getInvalidRange().getMessage();

        if (range != null){
            Feature feature = range.getFeature();

            if (feature != null){
                // get the invalid_caution from the DB or create it and persist it
                final DaoFactory daoFactory = evt.getDataContext().getDaoFactory();
                CvTopic invalid_caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByShortLabel(CvTopic.INVALID_RANGE);

                if (invalid_caution == null) {
                    invalid_caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, null, CvTopic.INVALID_RANGE);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(invalid_caution);
                }

                CvTopic caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

                if (caution == null) {
                    caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
                }

                Collection<Annotation> annotations = feature.getAnnotations();
                boolean hasInvalid = false;
                boolean hasCaution = false;

                for (Annotation a : annotations){
                    if (invalid_caution.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(message, a)){
                            hasInvalid = true;
                        }
                    }
                    else if (caution.equals(a.getCvTopic())){
                        if (hasAnnotationMessage(message, a)){
                            hasCaution = true;
                        }
                    }
                }

                if (!hasInvalid){
                    Annotation cautionRange = new Annotation(invalid_caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }

                if (!hasCaution){
                    Annotation cautionRange = new Annotation(caution, message);
                    daoFactory.getAnnotationDao().persist(cautionRange);

                    feature.addAnnotation(cautionRange);
                }

                daoFactory.getFeatureDao().update(feature);
            }
        }
    }

    private boolean hasAnnotationMessage(String message, Annotation a) {

        if (message != null){
            if (message.equals(a.getAnnotationText())){
                if (log.isDebugEnabled()) {
                    log.debug("Feature object already contains this annotation. Not adding another one: "+a);
                }
                return true;
            }
        }
        else if (message == null && a.getAnnotationText() == null){
            if (log.isDebugEnabled()) {
                log.debug("Feature object already contains this annotation. Not adding another one: "+a);
            }
            return true;
        }

        return false;
    }

    public Collection<Component> updateRanges(Protein protein, String uniprotSequence, ProteinUpdateProcessor processor){
        boolean sequenceToBeUpdated = false;

        String oldSequence = protein.getSequence();
        String sequence = uniprotSequence;
        if ( (oldSequence == null && sequence != null)) {
            if ( log.isDebugEnabled() ) {
                log.debug( "Sequence requires update." );
            }
            sequenceToBeUpdated = true;
        }
        else if (oldSequence != null && sequence != null){
            if (!sequence.equals( oldSequence ) ){
                if ( log.isDebugEnabled() ) {
                    log.debug( "Sequence requires update." );
                }
                sequenceToBeUpdated = true;
            }
        }

        Set<String> interactionAcsWithBadFeatures = new HashSet<String>();

        Collection<Component> components = protein.getActiveInstances();

        if ( sequenceToBeUpdated) {

            for (Component component : components){
                Interaction interaction = component.getInteraction();

                Collection<Feature> features = component.getBindingDomains();
                for (Feature feature : features){
                    Collection<InvalidRange> invalidRanges = checker.collectRangesImpossibleToShift(feature, oldSequence, sequence);

                    if (!invalidRanges.isEmpty()){
                        interactionAcsWithBadFeatures.add(interaction.getAc());

                        for (InvalidRange invalid : invalidRanges){
                            // range is bad from the beginning, not after the range shifting
                            if (!ProteinTools.isSequenceChanged(oldSequence, invalid.getSequence())){
                                InvalidRangeEvent invalidEvent = new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), invalid);
                                processor.fireOnInvalidRange(invalidEvent);
                                fixInvalidRanges(invalidEvent);
                            }
                        }
                    }
                }
            }

            if (!interactionAcsWithBadFeatures.isEmpty()){
                Collection<Component> componentsToFix = new ArrayList<Component>();
                for (Component c : components){
                    if (interactionAcsWithBadFeatures.contains(c.getInteraction().getAc())){
                        componentsToFix.add(c);
                    }
                }

                Collection<Component> componentsToUpdate = CollectionUtils.subtract(components, componentsToFix);

                if (!componentsToUpdate.isEmpty()){
                    shiftRanges(oldSequence, uniprotSequence, componentsToUpdate, processor);
                }

                return componentsToFix;
            }
            else {
                shiftRanges(oldSequence, uniprotSequence, components, processor);
            }
        }
        else {
            for (Component component : components){
                Interaction interaction = component.getInteraction();

                Collection<Feature> features = component.getBindingDomains();
                for (Feature feature : features){
                    Collection<InvalidRange> invalidRanges = checker.collectRangesImpossibleToShift(feature, oldSequence, sequence);

                    if (!invalidRanges.isEmpty()){
                        interactionAcsWithBadFeatures.add(interaction.getAc());

                        for (InvalidRange invalid : invalidRanges){
                            // range is bad from the beginning, not after the range shifting
                            InvalidRangeEvent invalidEvent = new InvalidRangeEvent(IntactContext.getCurrentInstance().getDataContext(), invalid);
                            processor.fireOnInvalidRange(invalidEvent);
                            fixInvalidRanges(invalidEvent);
                        }
                    }
                }
            }

            if (!interactionAcsWithBadFeatures.isEmpty()){
                Collection<Component> componentsToFix = new ArrayList<Component>();
                for (Component c : components){
                    if (interactionAcsWithBadFeatures.contains(c.getInteraction().getAc())){
                        componentsToFix.add(c);
                    }
                }

                return componentsToFix;
            }
        }

        return Collections.EMPTY_LIST;
    }
}
