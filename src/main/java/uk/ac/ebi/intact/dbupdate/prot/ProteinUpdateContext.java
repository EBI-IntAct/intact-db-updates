package uk.ac.ebi.intact.dbupdate.prot;

/**
 * A thread local access to protein update related information.
 *
 * @author Samuel Kerrien (skerrien@ebi.ac.uk)
 * @version $Id$
 * @since 2.1.1
 */
public class ProteinUpdateContext {

    private ProteinUpdateProcessorConfig config;

    private static ThreadLocal<ProteinUpdateContext> instance = new ThreadLocal<ProteinUpdateContext>() {
        @Override
        protected ProteinUpdateContext initialValue() {
            return new ProteinUpdateContext();
        }
    };

    public static ProteinUpdateContext getInstance() {
        return instance.get();
    }

    private ProteinUpdateContext() {
        // initialize here default configuration
        this.config = new ProteinUpdateProcessorConfig();
    }

    public ProteinUpdateProcessorConfig getConfig() {
        return config;
    }

    public void setConfig( ProteinUpdateProcessorConfig config ) {
        this.config = config;
    }
}
