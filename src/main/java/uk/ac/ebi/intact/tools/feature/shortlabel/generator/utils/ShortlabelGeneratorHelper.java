package uk.ac.ebi.intact.tools.feature.shortlabel.generator.utils;

import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Polymer;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.model.AminoAcids;

import java.util.Objects;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorHelper {

    public boolean orgSeqWrong(String originalSequence, String newGeneratedOriginalSequence) {
        return !originalSequence.equals(newGeneratedOriginalSequence);
    }

    public boolean containsLowerCaseLetters(String resultingSequence) {
        return !resultingSequence.equals(resultingSequence.toUpperCase());
    }

    public boolean isSingleAminoAcidChange(Long startPosition, Long endPosition) {
        return startPosition.equals(endPosition);
    }

    public boolean resultingSeqDescreased(String originalSequence, String resultingSequence) {
        return originalSequence.length() > resultingSequence.length();
    }

    public boolean resultingSeqIncreased(String originalSequence, String resultingSequence) {
        return originalSequence.length() < resultingSequence.length();
    }

    public String generateNonSequentialRange(Long startingPosition) {
        //Non sequential position are like: ile345thr
        return String.valueOf(startingPosition);
    }

    public String generateSequentialRange(Long startingPosition, Long endPosition) {
        //sequential position are like: pro_thr_leu12-14ala_ala_pro
        return startingPosition + "-" + endPosition;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public String generateOrgSeq(String interactorSequence, Long startingPosition, Long endPosition) {
        //Check if the displayed original sequence of a feature, still matches with the whole sequence.
        String originalSequence = "";
        while (startingPosition.intValue() <= endPosition.intValue()) {
            originalSequence += interactorSequence.charAt(startingPosition.intValue() - 1);
            startingPosition++;
        }
        return originalSequence;
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public String getInteractorSeqByInteractor(IntactInteractor intactInteractor) {
        //A polymer is an interactor with a sequence
        try {
            return ((Polymer) intactInteractor).getSequence();
        } catch (ClassCastException e) {
            return null;
        }
    }

    @Transactional(readOnly = true, propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public IntactInteractor getInteractorByFeatureEvidence(IntactFeatureEvidence intactFeatureEvidence) {
        //To get the whole sequence of a feature, we need to get the interactor
        return (IntactInteractor) intactFeatureEvidence.getParticipant().getInteractor();
    }

    public String seq2ThreeLetterCodeOnDefault(String sequence) {
        String sequenceAsThreeLetterCode = "";
        for (int i = 0; i < sequence.length(); i++) {
            sequenceAsThreeLetterCode += AminoAcids.getThreeLetterCodeByOneLetterCode(sequence.charAt(i));
            if (i < sequence.length() - 1) {
                sequenceAsThreeLetterCode += "_";
            }
        }
        return sequenceAsThreeLetterCode;
    }

    public String newSequence2ThreeLetterCodeOnDelete(String originalSequence) {
        String sequenceAsThreeLetterCode = AminoAcids.getThreeLetterCodeByOneLetterCode(originalSequence.charAt(0)) + "_";
        for (int i = 0; i < originalSequence.length() - 2; i++) {
            sequenceAsThreeLetterCode += "del_";
        }
        sequenceAsThreeLetterCode += AminoAcids.getThreeLetterCodeByOneLetterCode(originalSequence.charAt(originalSequence.length() - 1));
        return sequenceAsThreeLetterCode;
    }

    public boolean isDeletion(String originalSequence, String newSequence) {
        return newSequence.length() == 2 &&
                originalSequence.charAt(0) == newSequence.charAt(0) &&
                originalSequence.charAt(originalSequence.length() - 1) == newSequence.charAt(newSequence.length() - 1);
    }

    public boolean isUndefinedMutation(IntactFeatureEvidence featureEvidence) {
        return Objects.equals(featureEvidence.getShortName(), "undefined mutation");
    }
}
