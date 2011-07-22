package uk.ac.ebi.intact.update.model.protein.range;

import uk.ac.ebi.intact.update.model.HibernateUpdatePersistentImpl;
import uk.ac.ebi.intact.update.model.protein.update.events.ProteinEventWithRangeUpdate;

import javax.persistence.*;

/**
 * Represents an update of feature ranges
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>19-Oct-2010</pre>
 */
@Entity
@Inheritance(strategy=InheritanceType.SINGLE_TABLE)
@DiscriminatorColumn(name="objclass", discriminatorType=DiscriminatorType.STRING)
@DiscriminatorValue("PersistentUpdatedRange")
@Table(name = "ia_updated_range")
public class PersistentUpdatedRange extends HibernateUpdatePersistentImpl {
    private String oldPositions;
    private String newPositions;
    private String oldSequence;
    private String newSequence;

    private String rangeAc;

    private String featureAc;
    private String componentAc;
    private String interactionAc;

    private ProteinEventWithRangeUpdate parent;

    public PersistentUpdatedRange(){
        super();
        oldPositions = null;
        newPositions = null;
        rangeAc = null;
        this.componentAc = null;
        this.oldSequence = null;
        this.newSequence = null;
        this.featureAc = null;
        this.interactionAc = null;
    }

    public PersistentUpdatedRange(ProteinEventWithRangeUpdate proteinEvent, String componentAc, String featureAc, String interactionAc, String rangeAc, String oldSequence, String newSequence, String oldRangePositions, String newRangePositions){
        super();
        this.componentAc = componentAc;
        this.rangeAc = rangeAc;
        this.featureAc = featureAc;
        this.interactionAc = interactionAc;
        this.oldPositions = oldRangePositions;
        this.newPositions = newRangePositions;
        this.parent = proteinEvent;
        this.oldSequence = oldSequence;
        this.newSequence = newSequence;
    }

    @Column(name="component_ac", nullable=false)
    public String getComponentAc() {
        return componentAc;
    }

    public void setComponentAc(String componentAc) {
        this.componentAc = componentAc;
    }

    @Column(name="feature_ac", nullable=false)
    public String getFeatureAc() {
        return featureAc;
    }

    public void setFeatureAc(String featureAc) {
        this.featureAc = featureAc;
    }

    @Column(name="interaction_ac", nullable=false)
    public String getInteractionAc() {
        return interactionAc;
    }

    public void setInteractionAc(String interactionAc) {
        this.interactionAc = interactionAc;
    }

    @ManyToOne
    @JoinColumn(name="parent_id")
    public ProteinEventWithRangeUpdate getParent() {
        return this.parent;
    }

    public void setParent(ProteinEventWithRangeUpdate proteinEvent) {
        this.parent = proteinEvent;
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

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final PersistentUpdatedRange range = (PersistentUpdatedRange) o;

        if ( rangeAc != null ) {
            if (!rangeAc.equals( range.getRangeAc() )){
                return false;
            }
        }
        else if (range.getRangeAc()!= null){
            return false;
        }

        if ( componentAc != null ) {
            if (!componentAc.equals( range.getComponentAc() )){
                return false;
            }
        }
        else if (range.getComponentAc()!= null){
            return false;
        }

        if ( featureAc != null ) {
            if (!featureAc.equals( range.getFeatureAc() )){
                return false;
            }
        }
        else if (range.getFeatureAc()!= null){
            return false;
        }

        if ( interactionAc != null ) {
            if (!interactionAc.equals( range.getInteractionAc())){
                return false;
            }
        }
        else if (range.getComponentAc()!= null){
            return false;
        }

        if ( oldPositions != null ) {
            if (!oldPositions.equals( range.getOldPositions()) ){
                return false;
            }
        }
        else if (range.getOldPositions()!= null){
            return false;
        }

        if ( newPositions != null ) {
            if (!newPositions.equals( range.getNewPositions() )){
                return false;
            }
        }
        else if (range.getNewPositions()!= null){
            return false;
        }

        if ( oldSequence != null ) {
            if (!oldSequence.equals( range.getOldSequence() )){
                return false;
            }
        }
        else if (range.getOldSequence()!= null){
            return false;
        }

        if ( newSequence != null ) {
            if (!newSequence.equals( range.getNewSequence() )){
                return false;
            }
        }
        else if (range.getNewSequence()!= null){
            return false;
        }

        return true;
    }

    /**
     * This class overwrites equals. To ensure proper functioning of HashTable,
     * hashCode must be overwritten, too.
     *
     * @return hash code of the object.
     */
    @Override
    public int hashCode() {

        int code = 29;

        code = 29 * code + super.hashCode();

        if ( rangeAc != null ) {
            code = 29 * code + rangeAc.hashCode();
        }

        if ( componentAc != null ) {
            code = 29 * code + componentAc.hashCode();
        }

        if ( featureAc != null ) {
            code = 29 * code + featureAc.hashCode();
        }

        if ( interactionAc != null ) {
            code = 29 * code + interactionAc.hashCode();
        }

        if ( oldPositions != null ) {
            code = 29 * code + oldPositions.hashCode();
        }

        if ( newPositions != null ) {
            code = 29 * code + newPositions.hashCode();
        }

        if ( oldSequence != null ) {
            code = 29 * code + oldSequence.hashCode();
        }

        if ( newSequence != null ) {
            code = 29 * code + newSequence.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final PersistentUpdatedRange range = (PersistentUpdatedRange) o;

        if ( rangeAc != null ) {
            if (!rangeAc.equals( range.getRangeAc() )){
                return false;
            }
        }
        else if (range.getRangeAc()!= null){
            return false;
        }

        if ( componentAc != null ) {
            if (!componentAc.equals( range.getComponentAc() )){
                return false;
            }
        }
        else if (range.getComponentAc()!= null){
            return false;
        }

        if ( featureAc != null ) {
            if (!featureAc.equals( range.getFeatureAc() )){
                return false;
            }
        }
        else if (range.getFeatureAc()!= null){
            return false;
        }

        if ( interactionAc != null ) {
            if (!interactionAc.equals( range.getInteractionAc())){
                return false;
            }
        }
        else if (range.getComponentAc()!= null){
            return false;
        }

        if ( oldPositions != null ) {
            if (!oldPositions.equals( range.getOldPositions()) ){
                return false;
            }
        }
        else if (range.getOldPositions()!= null){
            return false;
        }

        if ( newPositions != null ) {
            if (!newPositions.equals(range.getNewPositions())){
                return false;
            }
        }
        else if (range.getNewPositions()!= null){
            return false;
        }

        if ( oldSequence != null ) {
            if (!oldSequence.equals( range.getOldSequence() )){
                return false;
            }
        }
        else if (range.getOldSequence()!= null){
            return false;
        }

        if ( newSequence != null ) {
            if (!newSequence.equals( range.getNewSequence() )){
                return false;
            }
        }
        else if (range.getNewSequence()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append("Range : [" + rangeAc != null ? rangeAc : "");
        buffer.append("] \n");

        buffer.append("Component : [" + componentAc != null ? componentAc : "");
        buffer.append("] \n");

        buffer.append("Feature : [" + featureAc != null ? featureAc : "");
        buffer.append("] \n");

        buffer.append("Interaction : [" + interactionAc != null ? interactionAc : "");
        buffer.append("] \n");

        if (oldPositions != null){
            buffer.append("Old positions : " + oldPositions);
            buffer.append("\n");
        }

        if (oldSequence != null){
            buffer.append("Old feature sequence : " + oldSequence);
            buffer.append("\n");
        }

        if (newPositions != null){
            buffer.append("New positions : " + newPositions);
            buffer.append("\n");
        }

        if (newSequence != null){
            buffer.append("New feature sequence : " + newSequence);
            buffer.append("\n");
        }

        return buffer.toString();
    }
}
