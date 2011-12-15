package uk.ac.ebi.intact;

import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.report.FileReportHandler;

import java.io.File;
import java.io.IOException;
import java.util.ArrayList;
import java.util.List;

/**
 * This class will read a file containing a list of protein acs and update each protein.
 * It needs the database, the folder where to put the log files, the boolean value to set the blast to true or false and the file containing one protein ac per line
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>13/05/11</pre>
 */

public class SelectionProteinUpdate {

        public static void main(String [] args){

        // three possible arguments
        /*if( args.length != 4 ) {
            System.err.println( "Usage: GlobalUpdate <database> <folder> <blast> <inputFile>" );
            System.exit( 1 );
        } */
        final String database = "enzpro";
        final String filename = "/home/marine/Desktop/update-test";
            //final String fileInputName = args[3];

        boolean isBlastEnabled = false;

        System.out.println( "folder where are the log files = " + filename );
        System.out.println( "database = " + database );
        System.out.println( "Blast enabled = " + isBlastEnabled );
        //System.out.println( "File containing protein acs to update = " + fileInputName );

        IntactContext.initContext(new String[]{"/META-INF/" + database + ".spring.xml"});

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setDeleteProtsWithoutInteractions(true);
        config.setGlobalProteinUpdate(true);
        config.setFixDuplicates(true);
        config.setProcessProteinNotFoundInUniprot(true);
        config.setBlastEnabled(isBlastEnabled);
        try {
            System.out.println("Reading file containing protein acs to update...");
            List<String> proteinAcs = new ArrayList<String>();

            /*File inputFile = new File(fileInputName);
            BufferedReader reader = new BufferedReader(new FileReader(inputFile));

            String line = reader.readLine();

            while (line != null){
                proteinAcs.add(line);
                line = reader.readLine();
            }

            reader.close();*/
            proteinAcs.add("EBI-78738");

            config.setReportHandler(new FileReportHandler(new File(filename)));

            ProteinUpdateProcessor updateProcessor = new ProteinUpdateProcessor();
            System.out.println("Starting the protein update for a selection of "+proteinAcs.size()+" proteins");
            updateProcessor.updateByACs(proteinAcs);

        } catch (IOException e) {
            System.err.println("The repository " + filename + " cannot be found. We cannot write log files and so we cannot run a global protein update.");
            e.printStackTrace();
        }
    }
}
