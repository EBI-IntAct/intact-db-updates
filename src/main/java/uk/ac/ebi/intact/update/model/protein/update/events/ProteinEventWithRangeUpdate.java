package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Annotation;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.feature.FeatureUpdatedAnnotation;
import uk.ac.ebi.intact.update.model.protein.range.PersistentUpdatedRange;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
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
@DiscriminatorFormula("objclass")
@DiscriminatorValue("ProteinEventWithRangeUpdate")
public abstract class ProteinEventWithRangeUpdate extends PersistentProteinEvent {

    private Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations = new ArrayList<FeatureUpdatedAnnotation>();
    private Collection<PersistentUpdatedRange> updatedRanges = new ArrayList<PersistentUpdatedRange>();

    public ProteinEventWithRangeUpdate() {
        super();
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, ProteinEventName name, Protein protein) {
        super(process, name, protein);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, ProteinEventName name, Protein protein, String uniprotAc) {
        super(process, name, protein, uniprotAc);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, ProteinEventName name, String proteinAc) {
        super(process, name, proteinAc);
    }

    public ProteinEventWithRangeUpdate(ProteinUpdateProcess process, ProteinEventName name, String proteinAc, String uniprotAc) {
        super(process, name, proteinAc, uniprotAc);
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<FeatureUpdatedAnnotation> getUpdatedFeatureAnnotations(){
        return updatedFeatureAnnotations;
    }

    public void addUpdatedFeatureAnnotationFromAnnotation(String featureAc, Collection<Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            FeatureUpdatedAnnotation annotation = new FeatureUpdatedAnnotation(featureAc, a, status);
            this.updatedFeatureAnnotations.add(annotation);
        }
    }

    public void setUpdatedFeatureAnnotations(Collection<FeatureUpdatedAnnotation> updatedFeatureAnnotations) {
        this.updatedFeatureAnnotations = updatedFeatureAnnotations;
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<PersistentUpdatedRange> getUpdatedRanges(){
        return updatedRanges;
    }

    public void setUpdatedRanges(Collection<PersistentUpdatedRange> updatedRanges) {
        this.updatedRanges = updatedRanges;
    }

    public boolean addRangeUpdate(PersistentUpdatedRange up){
        if (updatedRanges.add(up)){
            up.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeRangeUpdate(PersistentUpdatedRange up){
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
