package uk.ac.ebi.intact.dbupdate.feature.utils;

import java.io.BufferedReader;
import java.io.FileReader;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class InputReaderHelper {

    public static Set<String> readAcsIntoCollection(String path) {
        Set<String> issuedFeatureAcs = new HashSet<String>();
        return reader(path, issuedFeatureAcs);

    }

    private static Set<String> reader(String path, Set<String> issuedFeatureAcs) {
        BufferedReader bufferedReader = null;

        try {

            String line;

            bufferedReader = new BufferedReader(new FileReader(path));

            while ((line = bufferedReader.readLine()) != null) {
                issuedFeatureAcs.add(line);
            }

        } catch (IOException e) {
            e.printStackTrace();
        } finally {
            try {
                if (bufferedReader != null) bufferedReader.close();
            } catch (IOException ex) {
                ex.printStackTrace();
            }
        }
        return issuedFeatureAcs;
    }
}
