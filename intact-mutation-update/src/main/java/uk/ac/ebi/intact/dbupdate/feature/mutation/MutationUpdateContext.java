package uk.ac.ebi.intact.dbupdate.feature.mutation;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class MutationUpdateContext {

    private static MutationUpdateContext context = new MutationUpdateContext();
    private MutationUpdateConfig config;

    private MutationUpdateContext() {
        // initialize here default configuration
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/feature-update-spring.xml", "META-INF/shortlabel-generator-config.xml");
        this.config = context.getBean(MutationUpdateConfig.class);
    }

    public static MutationUpdateContext getInstance() {
        return context;
    }

    public MutationUpdateConfig getConfig() {
        return config;
    }

    public void setConfig(MutationUpdateConfig config) {
        this.config = config;
    }
}
