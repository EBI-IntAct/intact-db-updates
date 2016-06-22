package uk.ac.ebi.intact.dbupdate.feature.processor;

import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.ResultingSequenceChangedEvent.ChangeType;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ResSeqChangedErrorProcessor implements Proccesor {

    @Override
    public void process(String featureAc, String interactorAc, Object changeType, String message) {
        if(changeType instanceof ChangeType){

        } else {

        }
    }

    @Override
    public void process(IntactFeatureEvidence featureEvidence, String featureAc, String interactorAc, String originalShortlabel) {

    }
}
