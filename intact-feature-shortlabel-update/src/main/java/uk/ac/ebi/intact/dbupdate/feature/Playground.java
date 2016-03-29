package uk.ac.ebi.intact.dbupdate.feature;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import uk.ac.ebi.intact.dbupdate.feature.controller.ShortlabelGenerator;
import uk.ac.ebi.intact.jami.model.extension.FeatureEvidenceAnnotation;
import uk.ac.ebi.intact.jami.model.extension.IntactCvTerm;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.io.BufferedReader;
import java.io.FileReader;
import java.util.HashSet;
import java.util.Set;

public class Playground {
    public static Set<String[]> issuedFeatureAcs;

    public static void main(String[] args) {
        ShortlabelGenerator shortlabelGenerator;
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/shortlabel-generator-config.xml");
        shortlabelGenerator = context.getBean(ShortlabelGenerator.class);
        initIssuedFeatureAcs(args[0]);
        
        Set<IntactFeatureEvidence> intactFeatureEvidences = new HashSet<IntactFeatureEvidence>();
        IntactCvTerm REMARK_INTERNAL = shortlabelGenerator.getIntActCVTermRemarkInternal(3);
        int count = 0;
        for (String[] row : issuedFeatureAcs) {
            IntactFeatureEvidence intactFeatureEvidence;

            try {
                intactFeatureEvidence = shortlabelGenerator.getIntactFeatureEvidence(row[0], 3);
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
                continue;
            }
            if (row[1].equals("add wrong shortlabel, add remark-internal")) {
                intactFeatureEvidence.getAnnotations().add(new FeatureEvidenceAnnotation(REMARK_INTERNAL, "Sequence change details about this feature cannot be ascertained or do not fit with the current version of the referenced protein, so they have been deleted as a result of our quality control procedures. The original label was '" + intactFeatureEvidence.getShortName() + "'"));
                intactFeatureEvidence.setShortName("undefined mutation");
//                System.out.println(intactFeatureEvidence.getAc() + " " + intactFeatureEvidence.getShortName());
            }
            if (row[1].equals("update shortlabel, add remark-internal")) {
                intactFeatureEvidence.getAnnotations().add(new FeatureEvidenceAnnotation(REMARK_INTERNAL, "This feature label has been corrected as a result of our quality control procedures. The original label was '" + intactFeatureEvidence.getShortName() + "'"));
                try {
                    intactFeatureEvidences.add(shortlabelGenerator.generateNewShortLabel(intactFeatureEvidence));
                } catch (Exception e) {
                    System.out.println(e.getMessage() + " >>>>>> " + row[0]);
                }
//                System.out.println(intactFeatureEvidence.getAc() + " " + intactFeatureEvidence.getShortName());
            }
        }
        for (IntactFeatureEvidence intactFeatureEvidence : intactFeatureEvidences) {
//            Commented to avoid writing into the database.
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

    public static void initIssuedFeatureAcs(String path) {
        try {
            BufferedReader buf = new BufferedReader(new FileReader(path));
            String lineJustFetched = null;
            issuedFeatureAcs = new HashSet<String[]>();
            
            while (true) {
                lineJustFetched = buf.readLine();
                if (lineJustFetched == null) {
                    break;
                } else {
                    issuedFeatureAcs.add(lineJustFetched.split("\t"));
                }
            }
            buf.close();

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}
