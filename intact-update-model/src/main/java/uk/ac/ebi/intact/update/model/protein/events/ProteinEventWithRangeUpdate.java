package uk.ac.ebi.intact.update.model.protein.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.feature.FeatureUpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.range.AbstractUpdatedRange;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * this class contains range updates
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>22/07/11</pre>
 */
@Entity
public abstract class ProteinEventWithRangeUpdate<T extends AbstractUpdatedRange> extends PersistentProteinEvent {

    /**
     * The collection of updated feature annotations
     */
    private Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations = new ArrayList<FeatureUpdatedAnnotation>();

    /**
     * The collection of updated ranges
     */
    private Collection<T> updatedRanges = new ArrayList<T>();

    public ProteinEventWithRangeUpdate() {
        super();
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, Protein protein) {
        super(process, protein);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, protein, uniprotAc);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, String proteinAc) {
        super(process, proteinAc);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, proteinAc, uniprotAc);
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<FeatureUpdatedAnnotation> getUpdatedFeatureAnnotations(){
        return updatedFeatureAnnotations;
    }

    public void addUpdatedFeatureAnnotationFromAnnotation(String featureAc, Collection<Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            FeatureUpdatedAnnotation annotation = new FeatureUpdatedAnnotation(featureAc, a, status);
            annotation.setParent(this);
            this.updatedFeatureAnnotations.add(annotation);
        }
    }

    public void setUpdatedFeatureAnnotations(Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations) {
        this.updatedFeatureAnnotations = updatedFeatureAnnotations;
    }

    @Transient
    public Collection<T> getUpdatedRanges(){
        return updatedRanges;
    }

    public void setUpdatedRanges(Collection<T> updatedRanges) {
        this.updatedRanges = updatedRanges;
    }

    public boolean addRangeUpdate(T up){
        if (updatedRanges.add(up)){
            up.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeRangeUpdate(T up){
        if (updatedRanges.remove(up)){
            up.setParent(null);
            return true;
        }

        return false;
    }

        @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final ProteinEventWithRangeUpdate event = ( ProteinEventWithRangeUpdate ) o;

        if (!CollectionUtils.isEqualCollection(this.updatedFeatureAnnotations, event.getUpdatedFeatureAnnotations())){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.updatedRanges, event.getUpdatedRanges());
    }
}
