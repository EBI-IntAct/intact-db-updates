package uk.ac.ebi.intact.dbupdate.feature.writer;


import uk.ac.ebi.intact.dbupdate.feature.GeneratorError;

import java.io.IOException;
import java.util.List;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface generatorLineWritter {

    /**
     * append the ErrorLine to a file (which can be the property of the writer)
     * @param error : contains parameters of the CCLine
     * @throws java.io.IOException
     */
    public void writeErrorLine(GeneratorError error) throws IOException;

    /**
     * Write a list of Error lines
     * @param errors : a list of CC lines
     * @throws IOException
     */
    public void writeErrorLines(List<GeneratorError> errors) throws IOException;

    /**
     * Close the current writer
     * @throws IOException
     */
    public void close() throws IOException;


}
