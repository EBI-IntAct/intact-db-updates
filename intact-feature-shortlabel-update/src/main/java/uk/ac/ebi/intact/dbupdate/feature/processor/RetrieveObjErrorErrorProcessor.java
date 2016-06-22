package uk.ac.ebi.intact.dbupdate.feature.processor;

import uk.ac.ebi.intact.dbupdate.feature.GeneratorError;
import uk.ac.ebi.intact.dbupdate.feature.writer.generatorLineWritter;
import uk.ac.ebi.intact.dbupdate.feature.writer.generatorLineWritterImpl;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class RetrieveObjErrorErrorProcessor implements Proccesor {
    @Override
    public void process(String featureAc, String interactorAc, Object errorType, String message) {
        GeneratorError generatorError = new GeneratorError(featureAc, interactorAc, errorType.toString(), message);
        generatorLineWritter generatorLineWritter = new generatorLineWritterImpl();
        try {
            generatorLineWritter.writeErrorLine(generatorError);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    @Override
    public void process(IntactFeatureEvidence featureEvidence, String featureAc, String interactorAc, String originalShortlabel) {

    }
}
