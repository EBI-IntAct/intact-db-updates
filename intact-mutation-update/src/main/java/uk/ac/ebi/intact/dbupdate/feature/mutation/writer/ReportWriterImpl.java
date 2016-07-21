package uk.ac.ebi.intact.dbupdate.feature.mutation.writer;

import java.io.IOException;
import java.io.Writer;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ReportWriterImpl implements ReportWriter {

    private static final String COL_SEPARATOR = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final char HEADER_CHAR = '#';
    private Writer writer;
    private boolean isContentWritten;
    private int headerCols;

    public ReportWriterImpl(Writer writer) throws IOException {
        this.writer = writer;
    }

    public void writeHeaderIfNecessary(String... colHeaderTexts) throws IOException {
        if (!isContentWritten) {
            getWriter().write(HEADER_CHAR);
            writeColumnValues(colHeaderTexts);
            headerCols = colHeaderTexts.length;
        }

    }

    public void writeColumnValues(String... colValues) throws IOException {
        if (headerCols > 0 && colValues.length != headerCols) {
            throw new IllegalArgumentException("Unexpected number of values, as the header contains " + headerCols + " columns and the values provided were: " + colValues.length);
        }

        StringBuilder sb = new StringBuilder();

        for (int i = 0; i < colValues.length; i++) {
            if (i > 0) {
                sb.append(COL_SEPARATOR);
            }
            sb.append(colValues[i]);
        }

        sb.append(NEW_LINE);

        getWriter().write(sb.toString());

        isContentWritten = true;
    }

    public void writeLine(String str) throws IOException {
        getWriter().write(str + NEW_LINE);
    }

    public Writer getWriter() {
        return writer;
    }

    public void flush() throws IOException {
        getWriter().flush();
    }

    public void close() throws IOException {
        getWriter().close();
    }
}