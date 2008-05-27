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
package uk.ac.ebi.intact.dbupdate.prot.report;

import java.io.IOException;
import java.io.Writer;

/**
 * Writer decorator, to allow methods to write column-formatted data.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class ReportWriterImpl implements ReportWriter {

    private Writer writer;
    private boolean isContentWritten;
    private int headerCols;

    private static final String COL_SEPARATOR = "\t";
    private static final String NEW_LINE = System.getProperty("line.separator");
    private static final char HEADER_CHAR = '#';

    public ReportWriterImpl(Writer writer) throws IOException {
        this.writer = writer;
    }

    public void writeHeaderIfNecessary(String ... colHeaderTexts) throws IOException {
        if (!isContentWritten) {
            getWriter().write(HEADER_CHAR);
            writeColumnValues(colHeaderTexts);
            headerCols = colHeaderTexts.length;
        }

    }

    public void writeColumnValues(String ... colValues) throws IOException {
        if (headerCols > 0 && colValues.length != headerCols) {
            throw new IllegalArgumentException("Unexpected number of values, as the header contains "+headerCols+" columns and the values provided were: "+colValues.length);
        }

        StringBuilder sb = new StringBuilder();

        for (int i=0; i<colValues.length; i++) {
            if (i > 0) {
                sb.append(COL_SEPARATOR);
            }
            sb.append(colValues[i]);
        }

        sb.append(NEW_LINE);

        getWriter().write(sb.toString());

        isContentWritten = true;
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
