package uk.ac.ebi.intact.dbupdate.prot.rangefix;

import uk.ac.ebi.intact.model.*;

/**
 * Represents an updated range and contains the old version and the new version of the Range.
 *
 * @author Bruno Aranda (baranda@ebi.ac.uk)
 * @version $Id$
 */
public class UpdatedRange {

    private Range oldRange;
    private Range newRange;
    private String featureAc;
    private String componentAc;
    private String proteinAc;
    private String interactionAc;
    private String rangeAc;

    public UpdatedRange(Range oldRange, Range newRange) {
        this.oldRange = oldRange;
        this.newRange = newRange;

        // the current range to look at is the old range by default but it can happen if shifting ranges was successful that the current range is the new range
        Range currentRange = oldRange;

        if (oldRange == null){
            currentRange = newRange;
        }
        else if (oldRange.getAc() == null){
            currentRange = newRange;
        }

        this.rangeAc = currentRange.getAc();

        Feature feature = currentRange.getFeature();

        if (feature != null){
            this.featureAc = feature.getAc();

            Component component = feature.getComponent();

            if (component != null){
                this.componentAc = component.getAc();

                Interactor interactor = component.getInteractor();
                Interaction interaction = component.getInteraction();

                if (interactor != null){
                    this.proteinAc = interactor.getAc();
                }
                else {
                    this.proteinAc = null;
                }

                if (interaction != null){
                    this.interactionAc = interaction.getAc();
                }
                else {
                    this.interactionAc = null;
                }
            }
            else {
                this.componentAc = null;
                this.proteinAc = null;
                this.interactionAc = null;
            }
        }
        else {
            this.featureAc = null;
            this.componentAc = null;
            this.proteinAc = null;
            this.interactionAc = null;
        }
    }

    public Range getOldRange() {
        return oldRange;
    }

    public Range getNewRange() {
        return newRange;
    }

    public boolean isSequenceChanged() {
        if (isRangeLengthChanged()) {
            return true;
        }

        return !(rangeSequence(oldRange).equals(rangeSequence(newRange)));
    }

    public boolean isRangeLengthChanged() {
        return rangeLength(oldRange) != rangeLength(newRange);
    }

    protected int rangeLength(Range range) {
        if (range == null) {
            return -1;
        }

        if (range.getToIntervalEnd() == -1) {
            return -1;
        }

        return range.getToIntervalEnd()+1 - range.getFromIntervalStart();
    }

    protected String rangeSequence(Range range) {
        int length = rangeLength(range);

        String rseq = "";

        final String rangeSequence = range.getFullSequence();

        if (length <= 0 || rangeSequence == null) {
            return rseq;
        }

        final int rangeLength = rangeLength(range);

        if (rangeSequence.length() < rangeLength) {
            rseq = rangeSequence;
        } else {
            rseq = rangeSequence.substring(0, rangeLength);
        }

        return rseq;
    }

    public String getFeatureAc() {
        return featureAc;
    }

    public String getComponentAc() {
        return componentAc;
    }

    public String getInteractionAc() {
        return interactionAc;
    }

    public String getProteinAc() {
        return proteinAc;
    }

    public String getRangeAc() {
        return rangeAc;
    }
}