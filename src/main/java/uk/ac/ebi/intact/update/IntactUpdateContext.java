package uk.ac.ebi.intact.update;

import org.apache.commons.logging.Log;
import org.apache.commons.logging.LogFactory;
import org.springframework.beans.factory.DisposableBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.ApplicationContext;
import org.springframework.context.ConfigurableApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import org.springframework.stereotype.Component;
import uk.ac.ebi.intact.core.config.ConfigurationException;
import uk.ac.ebi.intact.core.context.IntactInitializationError;
import uk.ac.ebi.intact.dbupdate.prot.listener.ProteinUpdateProcessorListener;
import uk.ac.ebi.intact.update.model.protein.update.listener.EventPersisterListener;
import uk.ac.ebi.intact.update.persistence.UpdateDaoFactory;

import javax.annotation.PostConstruct;
import javax.annotation.Resource;
import java.io.Serializable;
import java.util.Arrays;
import java.util.LinkedList;
import java.util.List;

/**
 * The context of intact-update-model
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28/06/11</pre>
 */
@Component
public class IntactUpdateContext implements DisposableBean, Serializable {

    private static final Log log = LogFactory.getLog(IntactUpdateContext.class);

    private static IntactUpdateContext instance;

    @Autowired
    private ApplicationContext springContext;

    /**
     * The dao factory for update model persistence unit
     */
    @Autowired
    private UpdateDaoFactory updateFactory;

    @Resource(name = "eventPersisterListener")
    private ProteinUpdateProcessorListener proteinPersisterListener;

    public IntactUpdateContext() {

    }

    @PostConstruct
    public void init() {

        //configurator.initIntact( new StandaloneSession() );
        instance = this;
    }

    /**
     * Gets the current (ThreadLocal) instance of {@code IntactUpdateContext}. If no such instance exist,
     * we will be automatically initialized using JPA configurations in the classpath, configured
     * DataConfigs and, if these are not found, using a temporary database.
     *
     * @return the IntactContext instance
     */
    public static IntactUpdateContext getCurrentInstance() {
        if (!currentInstanceExists()) {

            log.warn("Current instance of IntactUpdateContext is null. Initializing a context in memory.");

            initStandaloneContextInMemory();
        }

        return instance;
    }

    /**
     * Checks if an instance already exists.
     *
     * @return True if an instance of IntactUpdateContext exist.
     */
    public static boolean currentInstanceExists() {
        return instance != null;
    }

    /**
     * Initializes a standalone {@code IntactUpdateContext} using a memory database.
     */
    public static void initStandaloneContextInMemory() {
        initStandaloneContextInMemory(null);
    }

    public static void initStandaloneContextInMemory(ApplicationContext parent) {
        initContext(new String[]{"classpath*:/META-INF/update-jpa.spring.xml"}, parent);
    }

    /**
     * Initializes a standalone context.
     */
    public static void initContext(String[] configurationResourcePaths) {
        initContext(configurationResourcePaths, null);
    }

    /**
     * Initializes a standalone context.
     */
    public static void initContext(String[] configurationResourcePaths, ApplicationContext parent) {
        // check for overflow initialization
        for (int i = 5; i < Thread.currentThread().getStackTrace().length; i++) {
            StackTraceElement stackTraceElement = Thread.currentThread().getStackTrace()[i];

            if (stackTraceElement.getClassName().equals(IntactUpdateContext.class.getName())
                    && stackTraceElement.getMethodName().equals("initContext")) {
                throw new IntactInitializationError("Infinite recursive invocation to IntactUpdateContext.initContext(). This" +
                        " may be due to an illegal invocation of IntactUpdateContext.getCurrentInstance() during bean instantiation.");
            }
        }

        // the order of the resources matters when overriding beans, so we add the intact first,
        // so the user can override the default beans.
        List<String> resourcesList = new LinkedList<String>();
        resourcesList.add("classpath*:/META-INF/update-jpa.spring.xml");
        resourcesList.addAll(Arrays.asList(configurationResourcePaths));

        configurationResourcePaths = resourcesList.toArray(new String[resourcesList.size()]);

        if (log.isDebugEnabled()) {
            log.debug("Loading Spring XML config:");
            for (String configurationResourcePath : configurationResourcePaths) {
                log.debug(" - " + configurationResourcePath);
            }
        }

        // init Spring
        ClassPathXmlApplicationContext springContext = new ClassPathXmlApplicationContext(configurationResourcePaths, parent);
        springContext.registerShutdownHook();

        instance = (IntactUpdateContext) springContext.getBean("intactUpdateContext");
    }

    public ConfigurableApplicationContext getSpringContext() {
        return (ConfigurableApplicationContext) springContext;
    }

    public UpdateDaoFactory getUpdateFactory() {
        if (updateFactory == null) {
            throw new ConfigurationException("No bean of type " + UpdateDaoFactory.class.getName() + " found. One is expected");
        }
        return updateFactory;
    }

    public ProteinUpdateProcessorListener getProteinPersisterListener() {
        if (proteinPersisterListener == null) {
            throw new ConfigurationException("No bean of type " + EventPersisterListener.class.getName() + " found. One is expected");
        }
        return proteinPersisterListener;
    }

    public void destroy() throws Exception {
        if (log.isDebugEnabled()) log.debug("Releasing LogFactory");
        LogFactory.release(Thread.currentThread().getContextClassLoader());

        if (log.isInfoEnabled()) log.debug("Destroying IntactUpdateContext");
        instance = null;
    }
}
