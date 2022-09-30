package uk.ac.ebi.intact.dbupdate.gene.utils;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;
import uk.ac.ebi.intact.dbupdate.gene.parser.UniProtParser;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.List;

/**
 * Created with IntelliJ IDEA.
 * User: ntoro
 * Date: 11/07/2013
 * Time: 16:36
 * To change this template use File | Settings | File Templates.
 */
public class UniProtRestQuery {

	public static final Log log = LogFactory.getLog(UniProtRestQuery.class);
	private static final String UNIPROT_SERVER = "https://rest.uniprot.org/";

	private UniProtParser uniProtParser;

    public UniProtRestQuery(UniProtParser uniProtParser) {
        this.uniProtParser = uniProtParser;
    }

    //This code follow the Uniprot example to retrive the information
	public List<UniProtResult> queryUniProt(String tool, ParameterNameValue[] params) throws GeneServiceException {

		List<UniProtResult> list = null;
		String location = queryURLGenerator(tool, params);
		HttpURLConnection conn = null;

		try {
			URL url = new URL(location);
			log.info("Submitting query: " + location);

			conn = (HttpURLConnection) url.openConnection();
			HttpURLConnection.setFollowRedirects(true);
			conn.setDoInput(true);
			conn.connect();
			int status = conn.getResponseCode();

			if (status == HttpURLConnection.HTTP_OK) {
				log.info("OK reply to query: " + location);
				InputStream is = conn.getInputStream();
				URLConnection.guessContentTypeFromStream(is);

				try {
					BufferedReader reader = new BufferedReader(new InputStreamReader(is));
					list = uniProtParser.parseUniProtQuery(reader);
                }
                finally {
                        //No need to check for null any exceptions thrown here will be caught by
                        //the outer catch block

                        //Close the top level stream
						is.close();
				}

			} else
				log.error("Failed, got " + conn.getResponseMessage() + " for " + location);

		} catch (IOException e) {
			throw new GeneServiceException("Error retrieving information from UniProt please try later. " +
					"Query: " + location + " Error message: " + e.getMessage(), e);
		} finally {
			if (conn != null) {
				conn.disconnect();
			}
		}

		return list;
	}

	protected static String queryURLGenerator(String tool, ParameterNameValue[] params) {

		StringBuilder locationBuilder = new StringBuilder(UNIPROT_SERVER + tool + "/stream?");
		for (int i = 0; i < params.length; i++) {
			if (i > 0)
				locationBuilder.append('&');
			locationBuilder.append(params[i].getName()).append('=').append(params[i].getValue());
		}

		return locationBuilder.toString();
	}

}
