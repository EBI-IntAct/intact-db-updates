package uk.ac.ebi.intact.dbupdate.feature.processor;

import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.FeatureAnnotationFoundEvent;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class FeatureAnnotationFoundErrorProcessor implements Proccesor {

    @Override
    public void process(String featureAc, String interactorAc, Object errorType, String message) {

    }

    @Override
    public void process(IntactFeatureEvidence featureEvidence, String featureAc, String interactorAc, String originalShortlabel) {

    }

    public class FeatureAnnotation {
        private String featureAc;
        private String interactorAc;
        private FeatureAnnotationFoundEvent.AnnotationType type;
        private String message;

        public FeatureAnnotation(String featureAc, String interactorAc, FeatureAnnotationFoundEvent.AnnotationType type, String message) {
            this.featureAc = featureAc;
            this.interactorAc = interactorAc;
            this.type = type;
            this.message = message;
        }

        public String getFeatureAc() {
            return featureAc;
        }

        public String getInteractorAc() {
            return interactorAc;
        }

        public FeatureAnnotationFoundEvent.AnnotationType getType() {
            return type;
        }

        public String getMessage() {
            return message;
        }
    }
}
