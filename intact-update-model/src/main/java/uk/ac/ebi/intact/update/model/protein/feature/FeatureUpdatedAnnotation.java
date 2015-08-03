package uk.ac.ebi.intact.update.model.protein.feature;

import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.update.model.UpdateEventImpl;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.UpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.events.ProteinEventWithRangeUpdate;

import javax.persistence.*;

/**
 * An updated annotations attached to a feature
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>20/07/11</pre>
 */
@Entity
@Table(name = "ia_feature_updated_annot")
public class FeatureUpdatedAnnotation extends UpdatedAnnotation {

    /**
     * The ac of the feature which has been updated
     */
    String featureAc;

    public FeatureUpdatedAnnotation(){
        super();
        this.featureAc = null;
    }

    public FeatureUpdatedAnnotation(String featureAc, String topic, String text, UpdateStatus status){
        super(topic, text, status);
        this.featureAc = featureAc;
    }

    public FeatureUpdatedAnnotation(String featureAc, Annotation annotation, UpdateStatus status){
        super(annotation, status);
        this.featureAc = featureAc;
    }

    @Column(name = "feature_ac", nullable = false)
    public String getFeatureAc() {
        return featureAc;
    }

    public void setFeatureAc(String featureAc) {
        this.featureAc = featureAc;
    }

    @Override
    @ManyToOne( targetEntity = ProteinEventWithRangeUpdate.class )
    @JoinColumn( name = "event_id")
    public UpdateEventImpl getParent() {
        return super.getParent();
    }
}
