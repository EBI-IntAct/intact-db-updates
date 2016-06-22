package uk.ac.ebi.intact.dbupdate.feature.processor;

import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface ErrorProcessor {

    public void process(String featureAc, String interactorAc, Object type, String message) throws IOException;
}
