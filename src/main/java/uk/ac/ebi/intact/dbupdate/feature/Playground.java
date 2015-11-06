package uk.ac.ebi.intact.dbupdate.feature;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.intact.dbupdate.feature.controller.ShortlabelGenerator;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.HashSet;
import java.util.Set;

public class Playground {
    public static Set<String> issuedFeatureAcs;

    public static void main(String[] args) {
        ShortlabelGenerator shortlabelGenerator;
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/shortlabel-generator-config.xml");
        shortlabelGenerator = context.getBean(ShortlabelGenerator.class);
        initIssuedFeatureAcs();
        Set<IntactFeatureEvidence> intactFeatureEvidences = new HashSet<IntactFeatureEvidence>();
        for (String ac : issuedFeatureAcs) {
            IntactFeatureEvidence intactFeatureEvidence;
            try {
                intactFeatureEvidence = shortlabelGenerator.getIntactFeatureEvidence(ac, 3);
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
                continue;
            }
            try {
                intactFeatureEvidences.add(shortlabelGenerator.generateNewShortLabel(intactFeatureEvidence));
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
            }
        }
        for (IntactFeatureEvidence intactFeatureEvidence : intactFeatureEvidences) {
            try {
                shortlabelGenerator.doUpdate(intactFeatureEvidence);
            } catch (PersisterException e) {
                e.printStackTrace();
            } catch (FinderException e) {
                e.printStackTrace();
            } catch (SynchronizerException e) {
                e.printStackTrace();
            }
            if (intactFeatureEvidence == null) {
                continue;
            }
            System.out.println(intactFeatureEvidence.getAc() + "\t" + intactFeatureEvidence.getShortName());
        }
    }

    public static void initIssuedFeatureAcs() {
        issuedFeatureAcs = new HashSet<String>();
        issuedFeatureAcs.add("EBI-10817815");
        issuedFeatureAcs.add("EBI-10690857");
        issuedFeatureAcs.add("EBI-6313362");
        issuedFeatureAcs.add("EBI-10690859");
        issuedFeatureAcs.add("EBI-10687976");
        issuedFeatureAcs.add("EBI-10898247");
        issuedFeatureAcs.add("EBI-10898249");
        issuedFeatureAcs.add("EBI-10898251");
        issuedFeatureAcs.add("EBI-6313362");
        issuedFeatureAcs.add("EBI-6313357");
        issuedFeatureAcs.add("EBI-8840858");
        issuedFeatureAcs.add("EBI-6285153");
        issuedFeatureAcs.add("EBI-6285304");
        issuedFeatureAcs.add("EBI-6285446");
        issuedFeatureAcs.add("EBI-6285534");
        issuedFeatureAcs.add("EBI-6280340");
        issuedFeatureAcs.add("EBI-6280702");
        issuedFeatureAcs.add("EBI-9979362");
    }
}
