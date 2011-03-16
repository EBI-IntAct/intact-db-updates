package uk.ac.ebi.intact.update.model.protein.update.events.range;

import uk.ac.ebi.intact.update.model.HibernatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.update.UpdatedAnnotation;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Represents an update of feature ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objClass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("UpdatedRange")
@Table(name = "ia_updated_range")
public class UpdatedRange extends HibernatePersistentImpl {
    private String oldPositions;
    private String newPositions;
    private String oldSequence;
    private String newSequence;

    private String rangeAc;

    private String componentAc;
    UpdateProcess updateProcess;

    Collection<UpdatedAnnotation> featureAnnotations;

    public UpdatedRange (){
        super();
        oldPositions = null;
        newPositions = null;
        rangeAc = null;
        this.componentAc = null;
        this.oldSequence = null;
        this.newSequence = null;

        this.featureAnnotations = new ArrayList<UpdatedAnnotation>();
    }

    public UpdatedRange(UpdateProcess updateProcess, String componentAc, String rangeAc, String oldSequence, String newSequence, String oldRangePositions, String newRangePositions){
        super();
        this.componentAc = componentAc;
        this.rangeAc = rangeAc;
        this.oldPositions = oldRangePositions;
        this.newPositions = newRangePositions;
        this.updateProcess = updateProcess;
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
        this.featureAnnotations = new ArrayList<UpdatedAnnotation>();
    }

    @Column(name="component_ac", nullable=false)
    public String getComponentAc() {
        return componentAc;
    }

    public void setComponentAc(String componentAc) {
        this.componentAc = componentAc;
    }

    @ManyToOne
    @JoinColumn(name="parent_id", nullable=false)
    public UpdateProcess getParent() {
        return this.updateProcess;
    }

    public void setParent(UpdateProcess updateProcess) {
        this.updateProcess = updateProcess;
    }

    @Column( name = "old_positions", nullable = false)
    public String getOldPositions() {
        return oldPositions;
    }

    public void setOldPositions(String oldPositions) {
        this.oldPositions = oldPositions;
    }

    @Column( name = "new_positions", nullable = true)
    public String getNewPositions() {
        return newPositions;
    }

    public void setNewPositions(String newPositions) {
        this.newPositions = newPositions;
    }

    @Column(name = "range_ac", nullable = false)
    public String getRangeAc() {
        return rangeAc;
    }

    public void setRangeAc(String rangeAc) {
        this.rangeAc = rangeAc;
    }

    @Lob
    @Column(name = "old_sequence", nullable = true)
    public String getOldSequence() {
        return oldSequence;
    }

    public void setOldSequence(String currentSequence) {
        this.oldSequence = currentSequence;
    }

    @Lob
    @Column(name = "new_sequence", nullable = true)
    public String getNewSequence() {
        return newSequence;
    }

    public void setNewSequence(String updatedSequence) {
        this.newSequence = updatedSequence;
    }

    @ManyToMany
    @JoinTable(
            name = "ia_event2updated_annotations",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "updated_annotation_id" )}
    )
    public Collection<UpdatedAnnotation> getFeatureAnnotations() {
        return featureAnnotations;
    }

    public void setFeatureAnnotations(Collection<UpdatedAnnotation> updatedAnnotations) {
        if (updatedAnnotations != null){
            this.featureAnnotations = updatedAnnotations;
        }
    }

    public void addUpdatedAnnotationFromFeature(Collection<uk.ac.ebi.intact.model.Annotation> updatedAnn, UpdateStatus status){
        for (uk.ac.ebi.intact.model.Annotation a : updatedAnn){

            UpdatedAnnotation annotation = new UpdatedAnnotation(a, status);
            this.featureAnnotations.add(annotation);
        }
    }
}
