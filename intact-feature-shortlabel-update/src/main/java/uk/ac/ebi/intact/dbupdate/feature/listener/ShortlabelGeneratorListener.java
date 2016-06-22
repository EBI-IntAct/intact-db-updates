package uk.ac.ebi.intact.dbupdate.feature.listener;

import uk.ac.ebi.intact.dbupdate.feature.processor.*;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.ShortlabelGeneratorObserver;
import uk.ac.ebi.intact.tools.feature.shortlabel.generator.events.*;

/**
 * Created by Maximilian Koch (mkoch@ebi.ac.uk).
 */
public class ShortlabelGeneratorListener implements ShortlabelGeneratorObserver {

    private RangeErrorErrorProcessor rangeErrorProcessor;
    private SuccessfulGeneratedErrorProcessor successfulGeneratedProcessor;
    private FeatureAnnotationFoundErrorProcessor featureAnnotationFoundProcessor;
    private ResSeqChangedErrorProcessor resSeqChangedProcessor;
    private RetrieveObjErrorErrorProcessor retrieveObjErrorProcessor;
    private TypeErrorErrorProcessor typeErrorProcessor;
    private SequenceErrorErrorProcessor sequenceErrorProcessor;

    public ShortlabelGeneratorListener() {
        this.rangeErrorProcessor = new RangeErrorErrorProcessor();
        this.successfulGeneratedProcessor = new SuccessfulGeneratedErrorProcessor();
        this.featureAnnotationFoundProcessor = new FeatureAnnotationFoundErrorProcessor();
        this.resSeqChangedProcessor = new ResSeqChangedErrorProcessor();
        this.retrieveObjErrorProcessor = new RetrieveObjErrorErrorProcessor();
        this.typeErrorProcessor = new TypeErrorErrorProcessor();
        this.sequenceErrorProcessor = new SequenceErrorErrorProcessor();
    }

    @Override
    public void onRangeErrorEvent(RangeErrorEvent event) {
        this.rangeErrorProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getErrorType(), event.getMessage());
    }

    @Override
    public void onSuccessfulGeneratedEvent(SuccessfulGeneratedEvent event) {
        this.successfulGeneratedProcessor.process(event.getFeatureEvidence(), event.getFeatureAc(), event.getInteractorAc(), event.getOriginalShortlabel());
    }

    @Override
    public void onRetrieveObjErrorEvent(ObjRetrieveErrorEvent event) {
        this.retrieveObjErrorProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getErrorType(), event.getMessage());
    }

    @Override
    public void onFeatureAnnotationFoundEvent(FeatureAnnotationFoundEvent event) {
        this.featureAnnotationFoundProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getType(), event.getMessage());
    }

    @Override
    public void onSeqErrorEvent(SequenceErrorEvent event) {
        this.sequenceErrorProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getErrorType(), event.getMessage());
    }

    @Override
    public void onResSeqChangedEvent(ResultingSequenceChangedEvent event) {
        this.resSeqChangedProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getChangeType(), event.getMessage());
    }

    @Override
    public void onTypeErrorEvent(TypeErrorEvent event) {
        this.typeErrorProcessor.process(event.getFeatureAc(), event.getInteractorAc(), event.getErrorType(), event.getMessage());
    }
}