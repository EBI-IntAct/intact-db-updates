package uk.ac.ebi.intact.dbupdate.feature.mutation;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import uk.ac.ebi.intact.dbupdate.feature.mutation.processor.MutationUpdateProcessor;
import uk.ac.ebi.intact.dbupdate.feature.mutation.processor.MutationUpdateProcessorConfig;
import uk.ac.ebi.intact.dbupdate.feature.mutation.writer.FileReportHandler;

import java.io.File;
import java.io.IOException;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class GlobalMutationUpdate {

    private static final Log log = LogFactory.getLog(GlobalMutationUpdate.class);

    public static void main(String[] args) {
        if (args.length != 2 ) {
            System.err.println("Usage: GlobalUpdate <database> <folder>");
            System.exit(1);
        }

        //TODO Review configuration in lsf
        final String database = args[0];
        final String filename = args[1];

        log.info("folder where are the log files = " + filename);
        log.info("database = " + database);

        MutationUpdateProcessorConfig config = MutationUpdateContext.getInstance().getConfig();

        try {
            MutationUpdateProcessor mutationUpdateProcessor = new MutationUpdateProcessor();
            config.setFileReportHandler(new FileReportHandler(new File(filename)));
            log.info("Starting the global update");
            mutationUpdateProcessor.updateAll();
        } catch (IOException e) {
            log.error("The repository " + filename + " cannot be found. We cannot write log files and so we cannot run a global mutation update.");
            e.printStackTrace();
        }
    }
}
