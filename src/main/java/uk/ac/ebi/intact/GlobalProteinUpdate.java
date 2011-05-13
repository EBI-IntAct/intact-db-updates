package uk.ac.ebi.intact;

import uk.ac.ebi.intact.core.context.IntactContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateContext;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.prot.ProteinUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.prot.report.FileReportHandler;

import java.io.File;
import java.io.IOException;

/**
 * Main class for global protein update
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>03-Dec-2010</pre>
 */

public class GlobalProteinUpdate {

    public static void main(String [] args){

        // three possible arguments
        if( args.length != 2 && args.length != 3 ) {
            System.err.println( "Usage: GlobalUpdate <database> <folder> <blast>" );
            System.exit( 1 );
        }
        final String database = args[0];
        final String filename = args[1];

        boolean isBlastEnabled = false;

        if (args.length == 3){
            isBlastEnabled = Boolean.parseBoolean(args[2]);
        }

        System.out.println( "folder where are the log files = " + filename );
        System.out.println( "database = " + database );
        System.out.println( "Blast enabled = " + isBlastEnabled );

        IntactContext.initContext(new String[] {"/META-INF/"+database+".spring.xml"});

        ProteinUpdateProcessorConfig config = ProteinUpdateContext.getInstance().getConfig();
        config.setDeleteProteinTranscriptWithoutInteractions(true);
        config.setDeleteProtsWithoutInteractions(true);
        config.setGlobalProteinUpdate(true);
        config.setFixDuplicates(true);
        config.setProcessProteinNotFoundInUniprot(true);
        config.setBlastEnabled(isBlastEnabled);
        try {
            config.setReportHandler(new FileReportHandler(new File(filename)));

            ProteinUpdateProcessor updateProcessor = new ProteinUpdateProcessor();
            System.out.println("Starting the global update");
            updateProcessor.updateAll();
            //List<Protein> proteins = updateProcessor.retrieveAndUpdateProteinFromUniprot("Q9XYZ4");
            //List<String> acs = new ArrayList<String>();
            //acs.add("EBI-3044019");
            //updateProcessor.updateByACs(acs);

        } catch (IOException e) {
            System.err.println("The repository " + filename + " cannot be found. We cannot write log files and so we cannot run a global protein update.");
            e.printStackTrace();
        }
    }
}
