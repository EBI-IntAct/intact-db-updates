package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event for participants having feature conflicts when trying to update the proteinAc sequence
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorValue("OutOfDateParticipantEvent")
public class OutOfDateParticipantEvent extends ProteinEvent{

    private Collection<String> componentsWithFeatureConflicts;
    private String remapped_protein;

    public OutOfDateParticipantEvent(){
        super();
        this.componentsWithFeatureConflicts = new ArrayList<String>();
        this.remapped_protein = null;

    }

    public OutOfDateParticipantEvent(UpdateProcess updateProcess, Protein protein, int index, Protein fixedProtein){
        super(updateProcess, EventName.participant_with_feature_conflicts, protein, index);
        this.componentsWithFeatureConflicts = new ArrayList<String>();
        this.remapped_protein = fixedProtein != null ? fixedProtein.getAc() : null;
    }

    @ElementCollection
    @CollectionTable(name="ia_component_conflicts", joinColumns=@JoinColumn(name="event_id"))
    @Column(name="component_ac")
    public Collection<String> getComponentsWithFeatureConflicts() {
        return componentsWithFeatureConflicts;
    }

    public void setComponentsWithFeatureConflicts(Collection<String> componentsWithFeatureConflicts) {
        if (componentsWithFeatureConflicts != null){
            this.componentsWithFeatureConflicts = componentsWithFeatureConflicts;
        }
    }

    @Column(name="remapped_protein_ac", nullable = true)
    public String getRemapped_protein() {
        return remapped_protein;
    }

    public void setRemapped_protein(String remapped_protein) {
        this.remapped_protein = remapped_protein;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remapped_protein != null ) {
            if (!remapped_protein.equals( event.getRemapped_protein())){
                return false;
            }
        }
        else if (event.getRemapped_protein()!= null){
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

        if ( remapped_protein != null ) {
            code = 29 * code + remapped_protein.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final OutOfDateParticipantEvent event = ( OutOfDateParticipantEvent ) o;

        if ( remapped_protein != null ) {
            if (!remapped_protein.equals( event.getRemapped_protein())){
                return false;
            }
        }
        else if (event.getRemapped_protein()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(componentsWithFeatureConflicts, event.getComponentsWithFeatureConflicts());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Out of date participant event : [Remapped protein ac = " + remapped_protein != null ? remapped_protein : "none");
        buffer.append("] \n");

        if (!componentsWithFeatureConflicts.isEmpty()){
            buffer.append("Components having feature conflicts : ");

            for (String p : componentsWithFeatureConflicts){
                buffer.append(p + ", ");
            }
        }

        return buffer.toString();
    }
}
