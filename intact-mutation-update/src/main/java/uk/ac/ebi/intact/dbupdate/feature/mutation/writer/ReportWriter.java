package uk.ac.ebi.intact.dbupdate.feature.mutation.writer;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public interface ReportWriter extends Flushable, Closeable {
    void writeHeaderIfNecessary(String ... colHeaderTexts) throws IOException;

    void writeColumnValues(String ... colValues) throws IOException;

    void writeLine(String str) throws IOException;
}
