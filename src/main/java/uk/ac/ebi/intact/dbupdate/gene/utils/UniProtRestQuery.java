package uk.ac.ebi.intact.dbupdate.gene.utils;

import au.com.bytecode.opencsv.CSVReader;
import au.com.bytecode.opencsv.bean.ColumnPositionMappingStrategy;
import au.com.bytecode.opencsv.bean.CsvToBean;
import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.gene.importer.GeneServiceException;

import java.io.BufferedReader;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.Reader;
import java.net.HttpURLConnection;
import java.net.URL;
import java.net.URLConnection;
import java.util.ArrayList;
import java.util.Arrays;
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

    private static final String UNIPROT_SERVER = "http://www.uniprot.org/";

    //This code follow the Uniprot example to retrive the information
    public static List<UniProtResult> queryUniProt(String tool, ParameterNameValue[] params) throws GeneServiceException {

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
                log.info("OK reply to query: " +location);
                InputStream is = conn.getInputStream();
                URLConnection.guessContentTypeFromStream(is);

                try {
                    BufferedReader reader = new BufferedReader(new InputStreamReader(is));
                    list = parseUniProtQuery(reader);


                } finally {
                    if (is != null){
                        is.close();
                    }
                }

            } else
                log.error("Failed, got " + conn.getResponseMessage() + " for " + location);

        } catch (Exception e) {
            throw new GeneServiceException("Error retrieving information from UniProt please try later. " +
                    "Query: " + location + "Error message: " + e.getMessage(), e);
        } finally {
            if (conn != null) {
                conn.disconnect();
            }
        }

        return list;
    }

    protected static String queryURLGenerator(String tool, ParameterNameValue[] params) {

        StringBuilder locationBuilder = new StringBuilder(UNIPROT_SERVER + tool + "/?");
        for (int i = 0; i < params.length; i++) {
            if (i > 0)
                locationBuilder.append('&');
            locationBuilder.append(params[i].getName()).append('=').append(params[i].getValue());
        }

        return locationBuilder.toString();
    }

    protected static List<UniProtResult> parseUniProtQuery(Reader result) {

        //We skip the header before start to read
        CSVReader reader = new CSVReader(result, '\t', '\"', 1);

        ColumnPositionMappingStrategy strat = new ColumnPositionMappingStrategy();

        String[] columns = new String[]{
                "entryName",
                "geneNames",
                "organism",
                "organismId",
                "entry",
                "status",
                "proteinNames"
        }; // the fields to bind do in your JavaBean

        strat.setColumnMapping(columns);
        strat.setType(UniProtResult.class);

        CsvToBean csv = new CsvToBean();
        List<UniProtResult> list = csv.parse(strat, reader);

        //We complement the result

        for (UniProtResult element : list) {
            element.setSynonyms(generateSynonyms(element));
            element.setRecommendedName(generateRecommendedName(element));
            element.setAlternativeNames(generateAlternativeNames(element));
        }

        return list;
    }


    protected static List<String> generateSynonyms(UniProtResult element) {

        List<String> synonyms = null;
        String aux = element.getGeneNames();

        if(aux!=null && !aux.isEmpty()){
            String[] geneNamesArray = aux.split("\\s");
            synonyms = Arrays.asList(geneNamesArray);
        }

        log.debug("Synonyms: " + (synonyms != null ? synonyms.toString() : null));
        return synonyms;
    }


    protected static String generateRecommendedName(UniProtResult element) {

        String aux = element.getProteinNames();
        String recommendedName = null;

        if (aux != null && !aux.isEmpty()) {
            int index = aux.indexOf("(");
            if (index > 0) {
                recommendedName = aux.substring(0, index-1).trim();
            } else { //If we do not have any whitespace we only have one name or any
                recommendedName = aux;
            }
        }

        log.debug("Recommended Name: " + recommendedName);
        return recommendedName;
    }

    protected static List<String> generateAlternativeNames(UniProtResult element) {

        String aux = element.getProteinNames();
        List<String> alternativeNamesList = null;

        if (aux != null && !aux.isEmpty()) {
            int index = aux.indexOf("(");
            if (index > 0) {
                String alternativeNames = aux.substring(index, aux.length());

                String[] alternativeNamesArray = alternativeNames.split("\\)");
                if (alternativeNamesArray != null && alternativeNamesArray.length > 0) {
                    alternativeNamesList = new ArrayList<String>(alternativeNamesArray.length);
                    for (String name : alternativeNamesArray) {
                        //We need to remove the parenthesis
                        alternativeNamesList.add(name.substring(name.indexOf("(")+1, name.length()));
                    }
                }
            }
        }

        log.debug("Alternative Names: " + (alternativeNamesList != null ? alternativeNamesList.toString() : null));
        return alternativeNamesList;  //To change body of created methods use File | Settings | File Templates.
    }

}
