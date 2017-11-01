package uk.ac.ebi.intact.dbupdate.feature.mutation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.intact.dbupdate.feature.mutation.processor.MutationUpdateProcessorConfig;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateContext {

    private static MutationUpdateContext ourInstance = new MutationUpdateContext();
    private MutationUpdateProcessorConfig config;

    private MutationUpdateContext() {
        // initialize here default configuration
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/feature-update-spring.xml", "META-INF/shortlabel-generator-config.xml");
        this.config = context.getBean(MutationUpdateProcessorConfig.class);
    }

    public static MutationUpdateContext getInstance() {
        return ourInstance;
    }

    public MutationUpdateProcessorConfig getConfig() {
        return config;
    }

    public void setConfig(MutationUpdateProcessorConfig config) {
        this.config = config;
    }
}
