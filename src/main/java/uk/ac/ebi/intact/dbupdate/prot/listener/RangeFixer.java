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

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.core.persistence.dao.DaoFactory;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.event.InvalidRangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.event.RangeChangedEvent;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.RangeChecker;
import uk.ac.ebi.intact.dbupdate.prot.rangefix.UpdatedRange;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;

import java.util.Collection;

/**
 * Listens for sequence changes and updates the ranges of the features.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class RangeFixer extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( RangeFixer.class );

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        RangeChecker rangeChecker = new RangeChecker();

        if (evt.getOldSequence() != null && evt.getProtein().getSequence() != null) {

            for (Component component : evt.getProtein().getActiveInstances()) {
                for (Feature feature : component.getBindingDomains()) {

                    Collection<UpdatedRange> updatedRanges = rangeChecker.shiftFeatureRanges(feature, evt.getOldSequence(), evt.getProtein().getSequence(), (ProteinUpdateProcessor) evt.getSource());

                    // fire the events for the range changes
                    for (UpdatedRange updatedRange : updatedRanges) {
                        ProteinUpdateProcessor processor = (ProteinUpdateProcessor) evt.getSource();
                        processor.fireOnRangeChange(new RangeChangedEvent(evt.getDataContext(), updatedRange));
                    }
                }
            }
        }
    }

    @Override
    public void onInvalidRange(InvalidRangeEvent evt){
        Range range = evt.getInvalidRange().getInvalidRange();
        String message = evt.getInvalidRange().getMessage();

        if (range != null){
            Feature feature = range.getFeature();

            if (feature != null){
                // get the caution from the DB or create it and persist it
                final DaoFactory daoFactory = IntactContext.getCurrentInstance().getDaoFactory();
                CvTopic caution = daoFactory
                        .getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.INVALID_RANGE_ID);

                if (caution == null) {
                    caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
                    IntactContext.getCurrentInstance().getCorePersister().saveOrUpdate(caution);
                }

                Collection<Annotation> annotations = feature.getAnnotations();

                for (Annotation a : annotations){
                    if (caution.equals(a.getCvTopic())){
                        if (message != null){
                            if (message.equals(a.getAnnotationText())){
                                if (log.isDebugEnabled()) {
                                    log.debug("Feature object already contains an invalid-range annotation. Not adding another one: "+a);
                                }
                                return;
                            }
                        }
                        else if (message == null && a.getAnnotationText() == null){
                              if (log.isDebugEnabled()) {
                                  log.debug("Feature object already contains an invalid-range annotation. Not adding another one: "+a);
                                }
                                return;
                        }
                    }
                }

                Annotation cautionRange = new Annotation(caution, message);
                daoFactory.getAnnotationDao().persist(cautionRange);

                feature.addAnnotation(cautionRange);
                daoFactory.getFeatureDao().update(feature);
            }
        }
    }
}
