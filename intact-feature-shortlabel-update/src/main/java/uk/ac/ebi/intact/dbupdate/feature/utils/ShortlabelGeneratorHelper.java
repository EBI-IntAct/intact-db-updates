package uk.ac.ebi.intact.dbupdate.feature.utils;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Polymer;
import uk.ac.ebi.intact.dbupdate.feature.model.AminoAcids;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorHelper {

    public static boolean originalSequenceIsWrong(String originalSequence, String newGeneratedOriginalSequence) {
        return !originalSequence.equals(newGeneratedOriginalSequence);
    }

    public static boolean containsLowerCaseLetters(String resultingSequence) {
        return !resultingSequence.equals(resultingSequence.toUpperCase());
    }

    public static boolean isSingleAminoAcidChange(Long startPosition, Long endPosition) {
        return startPosition.equals(endPosition);
    }

    public static boolean resultingSequenceHasDescreased(String originalSequence, String resultingSequence) {
        return originalSequence.length() > resultingSequence.length();
    }

    public static boolean resultingSequenceHasIncreased(String originalSequence, String resultingSequence) {
        return originalSequence.length() < resultingSequence.length();
    }

    public static String generateNonSequentialRange(Long startingPosition) {
        //Non sequential position are like: ile345thr
        return String.valueOf(startingPosition);
    }

    public static String generateSequentialRange(Long startingPosition, Long endPosition) {
        //sequential position are like: pro_thr_leu12-14ala_ala_pro
        return startingPosition + "-" + endPosition;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public static String generateOriginalSequence(String interactorSequence, Long startingPosition, Long endPosition) {
        //Check if the displayed original sequence of a feature, still matches with the whole sequence.
        String originalSequence = "";
        while (startingPosition.intValue() <= endPosition.intValue()) {
            originalSequence += interactorSequence.charAt(startingPosition.intValue() - 1);
            startingPosition++;
        }
        return originalSequence;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public static String getInteractorSequenceByInteractor(IntactInteractor intactInteractor) {
        //A polymer is an interactor with a sequence
        try {
            return ((Polymer) intactInteractor).getSequence();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public static IntactInteractor getInteractorByFeatureEvidence(IntactFeatureEvidence intactFeatureEvidence) {
        //To get the whole sequence of a feature, we need to get the interactor
        return (IntactInteractor) intactFeatureEvidence.getParticipant().getInteractor();
    }

    public static String originalSequence2ThreeLetterCode(String originalSequence) {
        String sequenceAsThreeLetterCode = "";
        for (int i = 0; i < originalSequence.length(); i++) {
            sequenceAsThreeLetterCode += AminoAcids.getThreeLetterCodeByOneLetterCode(originalSequence.charAt(i));
            if (i < originalSequence.length() - 1) {
                sequenceAsThreeLetterCode += "_";
            }
        }
        return sequenceAsThreeLetterCode;
    }

    public static String newSequence2ThreeLetterCodeOnDefault(String resultingSequence) {
        String sequenceAsThreeLetterCode = "";
        for (int i = 0; i < resultingSequence.length(); i++) {
            sequenceAsThreeLetterCode += AminoAcids.getThreeLetterCodeByOneLetterCode(resultingSequence.charAt(i));
            if (i < resultingSequence.length() - 1) {
                sequenceAsThreeLetterCode += "_";
            }
        }
        return sequenceAsThreeLetterCode;
    }

    public static String newSequence2ThreeLetterCodeOnDelete(String originalSequence) {
        String sequenceAsThreeLetterCode = AminoAcids.getThreeLetterCodeByOneLetterCode(originalSequence.charAt(0)) + "_";
        for (int i = 0; i < originalSequence.length() - 2; i++) {
            sequenceAsThreeLetterCode += "del_";
        }
        sequenceAsThreeLetterCode += AminoAcids.getThreeLetterCodeByOneLetterCode(originalSequence.charAt(originalSequence.length() - 1));
        return sequenceAsThreeLetterCode;
    }

    public static boolean isDeletion(String originalSequence, String newSequence) {
        return newSequence.length() == 2 &&
                originalSequence.charAt(0) == newSequence.charAt(0) &&
                originalSequence.charAt(originalSequence.length() - 1) == newSequence.charAt(newSequence.length() - 1);
    }
}
