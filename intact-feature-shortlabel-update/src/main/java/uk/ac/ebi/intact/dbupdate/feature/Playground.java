package uk.ac.ebi.intact.dbupdate.feature;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.intact.dbupdate.feature.listener.ShortlabelGeneratorListener;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGenerator;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.exception.FeatureShortlabelGenerationException;

import java.util.Set;

public class Playground {
    public static Set<String[]> issuedFeatureAcs;

    public static void main(String[] args) {
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/shortlabel-generator-config.xml");
        ShortlabelGenerator shortlabelGenerator = context.getBean(ShortlabelGenerator.class);


        String ac = "EBI-6262055";

        shortlabelGenerator.subscribeToEvents(new ShortlabelGeneratorListener());
        IntactFeatureEvidence featureEvidence = shortlabelGenerator.getFeatureEvidence(ac, 3);

        try {
            shortlabelGenerator.generateNewShortLabel(featureEvidence);
        } catch (FeatureShortlabelGenerationException ignored) {
        }
    }
}
