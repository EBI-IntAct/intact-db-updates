package uk.ac.ebi.intact.dbupdate.feature.controller;

import org.springframework.beans.factory.annotation.Required;
import org.springframework.transaction.annotation.Propagation;
import org.springframework.transaction.annotation.Transactional;
import psidev.psi.mi.jami.model.Range;
import uk.ac.ebi.intact.dbupdate.feature.exception.FeatureTypeException;
import uk.ac.ebi.intact.dbupdate.feature.exception.InteractorTypeException;
import uk.ac.ebi.intact.dbupdate.feature.exception.RangeException;
import uk.ac.ebi.intact.dbupdate.feature.exception.SequenceException;
import uk.ac.ebi.intact.dbupdate.feature.utils.OntologyServiceHelper;
import uk.ac.ebi.intact.dbupdate.feature.utils.ShortlabelGeneratorHelper;
import uk.ac.ebi.intact.jami.dao.IntactDao;
import uk.ac.ebi.intact.jami.model.extension.IntactFeatureEvidence;
import uk.ac.ebi.intact.jami.model.extension.IntactInteractor;
import uk.ac.ebi.intact.jami.synchronizer.FinderException;
import uk.ac.ebi.intact.jami.synchronizer.PersisterException;
import uk.ac.ebi.intact.jami.synchronizer.SynchronizerException;

import java.util.HashSet;
import java.util.Iterator;
import java.util.List;
import java.util.Set;

public class ShortlabelGenerator {

    private final static String MUTATION_MI_ID = "MI:0118";
    private final static int OLS_SEARCHING_DEPTH = 10;
    private final String TAB = "\t";
    private Set<String> allowedFeatureTypes = new HashSet<String>();
    private IntactDao intactDao;

    public ShortlabelGenerator() {
        initAllowedFeatureTypes();
    }

    private void initAllowedFeatureTypes() {
        allowedFeatureTypes.addAll(OntologyServiceHelper.getOntologyServiceHelper().getAssociatedMITerms(MUTATION_MI_ID, OLS_SEARCHING_DEPTH));
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public IntactFeatureEvidence getIntactFeatureEvidence(String ac, int tries) {
        IntactFeatureEvidence intactFeatureEvidence = intactDao.getFeatureEvidenceDao().getByAc(ac);
        if (intactFeatureEvidence == null && tries > 0) {
            tries--;
            intactFeatureEvidence = getIntactFeatureEvidence(ac, tries);
        } else if (intactFeatureEvidence == null && tries == 0) {
            throw new NullPointerException(ac + TAB + "Can not receive IntactFeatureEvidence object (Please check if feature still exists)");
        }
        return intactFeatureEvidence;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public IntactFeatureEvidence generateNewShortLabel(IntactFeatureEvidence intactFeatureEvidence) {
        IntactInteractor intactInteractor = ShortlabelGeneratorHelper.getInteractorByFeatureEvidence(intactFeatureEvidence);
        if (intactInteractor == null) {
            throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "Interactor is null");
        }
        //todo
        if (!intactInteractor.getInteractorType().getShortName().equals("protein") &&
                !intactInteractor.getInteractorType().getShortName().equals("peptide")) {
            throw new InteractorTypeException(intactFeatureEvidence.getAc() + TAB + "Interactor type is not protein or peptide");
        }

        String interactorSequence = ShortlabelGeneratorHelper.getInteractorSequenceByInteractor(intactInteractor);

        if (interactorSequence == null) {
            throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "Interactor sequence is null");
        }

        if (!allowedFeatureTypes.contains(intactFeatureEvidence.getType().getMIIdentifier())) {
            throw new FeatureTypeException(intactFeatureEvidence.getAc() + TAB + "Is not of type mutation (MI:0118)");
        }
        List<Range> ranges = (List<Range>) intactFeatureEvidence.getRanges();
        for (Range range : ranges) {
            if (range.getResultingSequence().getNewSequence() == null) {
                throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "Could not find resulting sequence");
            }
        }
        intactFeatureEvidence.setShortName("");
        Iterator<Range> rangeIterator = ranges.iterator();
        while (rangeIterator.hasNext()) {
            Range range = rangeIterator.next();
            String newShortlabel = "";
            if (range == null) {
                throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "No ranges could be found");
            }
            if (range.getStart().getStart() == 0) {
                throw new RangeException(intactFeatureEvidence.getAc() + TAB + "Starting position is 0");
            }
            if (range.getResultingSequence().getOriginalSequence() == null) {
                throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "Original sequence is null");
            }
            String newGeneratedOriginalSequence = ShortlabelGeneratorHelper.generateOriginalSequence(interactorSequence,
                    range.getStart().getStart(), range.getEnd().getEnd());
            if (newGeneratedOriginalSequence == null) {
                throw new NullPointerException(intactFeatureEvidence.getAc() + TAB + "Couldn't calculate original sequence from whole sequence");
            }
            if (ShortlabelGeneratorHelper.originalSequenceIsWrong(range.getResultingSequence().getOriginalSequence(),
                    newGeneratedOriginalSequence)) {
                throw new SequenceException(intactFeatureEvidence.getAc() + TAB + " Original sequence does not match interactor sequence. Is "
                        + range.getResultingSequence().getOriginalSequence()
                        + " should be " + newGeneratedOriginalSequence + " Range: (" + range.getStart().getStart() + "-" + range.getEnd().getEnd() + ")");
            }
            if (ShortlabelGeneratorHelper.containsLowerCaseLetters(range.getResultingSequence().getNewSequence())) {
                throw new SequenceException(intactFeatureEvidence.getAc() + TAB + "Resulting sequence contains lower case letters");
            }

            newShortlabel += ShortlabelGeneratorHelper.originalSequence2ThreeLetterCode(range.getResultingSequence().getOriginalSequence());

            if (ShortlabelGeneratorHelper.isSingleAminoAcidChange(range.getStart().getStart(), range.getEnd().getEnd())) {
                newShortlabel += ShortlabelGeneratorHelper.generateNonSequentialRange(range.getStart().getStart());
            } else {
                newShortlabel += ShortlabelGeneratorHelper.generateSequentialRange(range.getStart().getStart(), range.getEnd().getEnd());
            }

            if (ShortlabelGeneratorHelper.resultingSequenceHasDescreased(range.getResultingSequence().getOriginalSequence(),
                    range.getResultingSequence().getNewSequence())) {
                if (ShortlabelGeneratorHelper.isDeletion(range.getResultingSequence().getOriginalSequence(),
                        range.getResultingSequence().getNewSequence())) {
                    newShortlabel += ShortlabelGeneratorHelper.newSequence2ThreeLetterCodeOnDelete(range.getResultingSequence().getOriginalSequence());
                } else {
                    newShortlabel += ShortlabelGeneratorHelper.newSequence2ThreeLetterCodeOnDefault(range.getResultingSequence().getNewSequence());
                }
            } else {
                newShortlabel += ShortlabelGeneratorHelper.newSequence2ThreeLetterCodeOnDefault(range.getResultingSequence().getNewSequence());
            }

            intactFeatureEvidence.setShortName(intactFeatureEvidence.getShortName() + newShortlabel
                    + (rangeIterator.hasNext() ? "," : ""));
        }
        return intactFeatureEvidence;
    }

    @Transactional(propagation = Propagation.REQUIRED, value = "jamiTransactionManager")
    public void doUpdate(IntactFeatureEvidence intactFeatureEvidence) throws PersisterException, FinderException, SynchronizerException {
        intactDao.getEntityManager().clear();
        try {
            intactDao.getFeatureEvidenceDao().update(intactFeatureEvidence);
        } catch (SynchronizerException e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            throw e;
        } catch (FinderException e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            throw e;
        } catch (PersisterException e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            throw e;
        } catch (Throwable e) {
            intactDao.getSynchronizerContext().clearCache();
            intactDao.getEntityManager().clear();
            throw new PersisterException(e.getMessage(), e);
        }
        intactDao.getSynchronizerContext().clearCache();
    }

    @Required
    public void setIntactDao(IntactDao intactDao) {
        this.intactDao = intactDao;
    }
}