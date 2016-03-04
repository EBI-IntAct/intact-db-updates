package uk.ac.ebi.intact.dbupdate.feature;

import org.springframework.context.ApplicationContext;
import org.springframework.context.support.ClassPathXmlApplicationContext;
import psidev.psi.mi.jami.model.Range;
import uk.ac.ebi.intact.dbupdate.feature.controller.ShortlabelGenerator;
import uk.ac.ebi.intact.dbupdate.feature.utils.InputReaderHelper;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;

import java.io.BufferedWriter;
import java.io.File;
import java.io.FileWriter;
import java.io.IOException;
import java.util.HashSet;
import java.util.Set;

public class Playground {
    public static Set<String> issuedFeatureAcs;

    public static void main(String[] args) throws IOException {
        ShortlabelGenerator shortlabelGenerator;
        ApplicationContext context = new ClassPathXmlApplicationContext("META-INF/shortlabel-generator-config.xml");
        shortlabelGenerator = context.getBean(ShortlabelGenerator.class);
//        initIssuedFeatureAcs();
        issuedFeatureAcs = InputReaderHelper.readAcsIntoCollection(args[0]);
        Set<IntactFeatureEvidence> intactFeatureEvidences = new HashSet<IntactFeatureEvidence>();
        File file = new File("/Users/maximiliankoch/feature_export_file.txt");

        // if file doesnt exists, then create it
        if (!file.exists()) {
            file.createNewFile();
        }

        FileWriter fw = new FileWriter(file.getAbsoluteFile());
        BufferedWriter bw = new BufferedWriter(fw);

        String header = "";
        header += "FEATURE_AC";
        header += "\t";
        header += "OLD_SHORTLABEL";
        header += "\t";
        header += "NEW_SHORTLABEL";
        header += "\t";
        header += "ORG_SEQ";
        header += "\t";
        header += "RES_SEQ";
        header += "\t";
        header += "START_POS";
        header += "\t";
        header += "END_POSE";
        header += "\t";
        header += "FEATURE_TYPE";
        header += "\t";
        header += "CREATED";
        header += "\t";
        header += "LAST_UPDATED";
        header += "\t";
        header += "COMMENT";
        header += "\n";
        bw.write(header);

        for (String ac : issuedFeatureAcs) {
            IntactFeatureEvidence intactFeatureEvidence;
            try {
                intactFeatureEvidence = shortlabelGenerator.getIntactFeatureEvidence(ac, 3);
            } catch (NullPointerException e) {
                System.out.println(e.getMessage());
                continue;
            }
            boolean success = true;
            String oldShortLable = intactFeatureEvidence.getShortName();
            try {
                intactFeatureEvidences.add(shortlabelGenerator.generateNewShortLabel(intactFeatureEvidence));
            } catch (Exception e) {
//                System.out.println(e.getMessage());
                success = false;
                for(Range range : intactFeatureEvidence.getRanges()) {
                    String x = "";
                    x += intactFeatureEvidence.getAc();
                    x += "\t";
                    x += oldShortLable;
                    x += "\t";
                    x += intactFeatureEvidence.getShortName();
                    x += "\t";
                    x += range.getResultingSequence().getOriginalSequence();
                    x += "\t";
                    x += range.getResultingSequence().getNewSequence();
                    x += "\t";
                    x += range.getStart().getStart();
                    x += "\t";
                    x += range.getEnd().getEnd();
                    x += "\t";
                    x += intactFeatureEvidence.getType().getMIIdentifier() + "("+ intactFeatureEvidence.getType().getShortName() +")";
                    x += "\t";
                    x += intactFeatureEvidence.getCreated();
                    x += "\t";
                    x += intactFeatureEvidence.getUpdated();
                    x += "\t";
                    x += e.getMessage();
                    x += "\n";
                    try {
                        bw.write(x);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }

            }
            if(success){
                for (Range range : intactFeatureEvidence.getRanges()) {
                    String x = "";
                    x += intactFeatureEvidence.getAc();
                    x += "\t";
                    x += oldShortLable;
                    x += "\t";
                    x += intactFeatureEvidence.getShortName();
                    x += "\t";
                    x += range.getResultingSequence().getOriginalSequence();
                    x += "\t";
                    x += range.getResultingSequence().getNewSequence();
                    x += "\t";
                    x += range.getStart().getStart();
                    x += "\t";
                    x += range.getEnd().getEnd();
                    x += "\t";
                    x += intactFeatureEvidence.getType().getMIIdentifier() + "(" + intactFeatureEvidence.getType().getShortName() + ")";
                    x += "\t";
                    x += intactFeatureEvidence.getCreated();
                    x += "\t";
                    x += intactFeatureEvidence.getUpdated();
                    x += "\t";
                    if(!oldShortLable.equals(intactFeatureEvidence.getShortName())) {
                        x += "OK. Shortlabel has been updated.";
                    } else {
                        x += "OK.";
                    }
                    x += "\n";
                    try {
                        bw.write(x);
                    } catch (IOException e1) {
                        e1.printStackTrace();
                    }
                }
            }

        }
//
//        for (IntactFeatureEvidence intactFeatureEvidence : intactFeatureEvidences) {
////            Commented to avoid writing into the database.
////            try {
////                shortlabelGenerator.doUpdate(intactFeatureEvidence);
////            } catch (PersisterException e) {
////                e.printStackTrace();
////            } catch (FinderException e) {
////                e.printStackTrace();
////            } catch (SynchronizerException e) {
////                e.printStackTrace();
////            }
////            if (intactFeatureEvidence == null) {
////                continue;
////            }
//            System.out.println(intactFeatureEvidence.getAc() + "\t" + intactFeatureEvidence.getShortName());
//        }
        try {
            bw.close();
        } catch (IOException e) {
            e.printStackTrace();
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
