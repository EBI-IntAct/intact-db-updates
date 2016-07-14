import impl.FeatureListener;
import org.junit.Test;
import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGenerator;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorTest {

    private ShortlabelGenerator getShortlabelGenerator(){
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/shortlabel-generator-config.xml");
        return context.getBean(ShortlabelGenerator.class);
    }
    
    @Test
    public void ShortlabelGeneratorTest_1(){
        ShortlabelGenerator shortlabelGenerator = getShortlabelGenerator();
        shortlabelGenerator.addListener(new FeatureListener());
            shortlabelGenerator.generateNewShortLabel("EBI-10769146");

    }
}
