package uk.ac.ebi.intact.dbupdate.prot.report;

import java.io.Closeable;
import java.io.Flushable;
import java.io.IOException;

/**
 * The interface to implement for the writers during a protein update
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public interface ReportWriter extends Flushable, Closeable{
    void writeHeaderIfNecessary(String ... colHeaderTexts) throws IOException;

    void writeColumnValues(String ... colValues) throws IOException;

    void writeLine(String str) throws IOException;
}
