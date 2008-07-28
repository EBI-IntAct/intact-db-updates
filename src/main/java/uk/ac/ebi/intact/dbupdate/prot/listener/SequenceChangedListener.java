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
import uk.ac.ebi.intact.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProcessorException;
import uk.ac.ebi.intact.dbupdate.prot.event.ProteinSequenceChangeEvent;
import uk.ac.ebi.intact.dbupdate.prot.util.ProteinTools;
import uk.ac.ebi.intact.model.*;
import uk.ac.ebi.intact.model.util.CvObjectUtils;
import uk.ac.ebi.intact.model.util.ProteinUtils;
import uk.ac.ebi.intact.persistence.dao.DaoFactory;

/**
 * Checks the protein sequence updates in order to assess the change and add additional
 * information about the change (e.g. Add cautions for proteins and interactions if the
 * sequence changes considerably).
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class SequenceChangedListener extends AbstractProteinUpdateProcessorListener {

    private static final Log log = LogFactory.getLog( SequenceChangedListener.class );

    private static final String SEQCHANGED_CAUTION = "Protein [uniprotkb] has undergone a significant sequence change since this"  +
                                                     "entry was originally annotated which may effect the results shown. For " +
                                                     "further information, please access the IntAct curation manual and for " +
                                                     "details of the sequence change go to [unisave]";

    private double conservationThreshold = 0.35;

    public SequenceChangedListener() {
    }

    public SequenceChangedListener(double conservationThreshold) {
        this.conservationThreshold = conservationThreshold;
    }

    @Override
    public void onProteinSequenceChanged(ProteinSequenceChangeEvent evt) throws ProcessorException {
        String oldSeq = evt.getOldSequence();
        String newSeq = evt.getProtein().getSequence();

        double relativeConservation = ProteinTools.calculateSequenceConservation(oldSeq, newSeq);

        if (log.isDebugEnabled()) {
            log.debug("After sequence update, the relative sequence conservation is "+relativeConservation);
        }

        // if the sequences are considerably different, create a caution for the protein and the interactions
        if ( relativeConservation <= conservationThreshold) {
                    final InteractorXref xref = ProteinUtils.getUniprotXref(evt.getProtein());
            String uniprotAc = null;

            if (xref != null) {
                uniprotAc = xref.getPrimaryId();
            }

            String message = SEQCHANGED_CAUTION.replaceAll("\\[uniprotkb\\]", "[uniprotkb:"+ uniprotAc+"]");

            if (log.isWarnEnabled()) log.warn("Sequence has changed considerably during update for protein: "+protInfo(evt.getProtein()));

            addCaution(evt.getProtein(), message);

            for (Component comp : evt.getProtein().getActiveInstances()) {
                addCaution(comp.getInteraction(), message);
            }
        }
    }

    protected void addCaution(AnnotatedObject<?, ?> ao, String cautionMessage) {
        // check if the annotated object already contains a caution for the sequence change
        for (Annotation annot : ao.getAnnotations()) {
            if (CvTopic.CAUTION_MI_REF.equals(annot.getCvTopic().getIdentifier()) &&
                    annot.getAnnotationText().equals(cautionMessage)) {
                if (log.isDebugEnabled()) {
                    log.debug("Annotated object already contains a caution. Not adding another one: "+ao);
                }
                return;
            }
        }

        // get the caution from the DB or create it and persist it
        final DaoFactory daoFactory = IntactContext.getCurrentInstance().getDataContext().getDaoFactory();
        CvTopic caution = daoFactory
                .getCvObjectDao(CvTopic.class).getByPsiMiRef(CvTopic.CAUTION_MI_REF);

        if (caution == null) {
            caution = CvObjectUtils.createCvObject(IntactContext.getCurrentInstance().getInstitution(), CvTopic.class, CvTopic.CAUTION_MI_REF, CvTopic.CAUTION);
            daoFactory.getCvObjectDao(CvTopic.class).persist(caution);
        }

        // add the caution to the annotated object
        Annotation annotation = new Annotation(IntactContext.getCurrentInstance().getInstitution(), caution, cautionMessage);
        ao.addAnnotation(annotation);
    }
}
