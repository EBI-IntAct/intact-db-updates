package uk.ac.ebi.intact.dbupdate.feature.writer;

import uk.ac.ebi.intact.dbupdate.feature.GeneratorError;

import java.io.IOException;
import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class generatorLineWritterImpl implements generatorLineWritter {

    @Override
    public void writeErrorLine(GeneratorError error) throws IOException {
        System.out.println(error.toString());
    }

    @Override
    public void writeErrorLines(List<GeneratorError> errors) throws IOException {

    }

    @Override
    public void close() throws IOException {

    }
}
