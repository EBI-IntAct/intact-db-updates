package uk.ac.ebi.intact.dbupdate.gene.utils;

import java.io.Reader;
import java.util.List;

/**
 * Created by IntelliJ IDEA.
 * User: noedelta
 * Date: 19/07/2013
 * Time: 08:48
 */
public interface UniProtParser {

	public List<UniProtResult> parseUniProtQuery(Reader reader);
}
