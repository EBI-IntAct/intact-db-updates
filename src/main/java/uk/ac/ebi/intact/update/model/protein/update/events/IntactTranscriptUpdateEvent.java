package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event for isoforms and feature chains with necessity to update the parent cross reference
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/03/11</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("IntactTranscriptUpdateEvent")
public class IntactTranscriptUpdateEvent extends ProteinEventWithMessage{

    Collection<String> oldParentAcs;
    String newParentAc;

    public IntactTranscriptUpdateEvent(){
        super();
    }

    public IntactTranscriptUpdateEvent(UpdateProcess updateProcess, Protein transcript, String message, String newParentAc ){
        super(updateProcess, EventName.transcript_parent_update, transcript, message);
        this.newParentAc = newParentAc;
        this.message = message;
        this.oldParentAcs = new ArrayList<String>();
    }

    @ElementCollection
    @JoinTable(name = "ia_event2old_parent", joinColumns = @JoinColumn(name="event_id"))
    @Column(name = "old_parent")
    public Collection<String> getOldParentAcs() {
        return oldParentAcs;
    }

    public void setOldParentAcs(Collection<String> oldParentAcs) {
        this.oldParentAcs = oldParentAcs;
    }

    @Column(name = "new_parent")
    public String getNewParentAc() {
        return newParentAc;
    }

    public void setNewParentAc(String newParentAc) {
        this.newParentAc = newParentAc;
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final IntactTranscriptUpdateEvent event = ( IntactTranscriptUpdateEvent ) o;

        if ( newParentAc != null ) {
            if (!newParentAc.equals( event.getNewParentAc() )){
                return false;
            }
        }
        else if (event.getNewParentAc()!= null){
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

        if ( newParentAc != null ) {
            code = 29 * code + newParentAc.hashCode();
        }

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final IntactTranscriptUpdateEvent event = ( IntactTranscriptUpdateEvent ) o;

        if ( newParentAc != null ) {
            if (!newParentAc.equals( event.getNewParentAc() )){
                return false;
            }
        }
        else if (event.getNewParentAc()!= null){
            return false;
        }

        return CollectionUtils.isEqualCollection(oldParentAcs, event.getOldParentAcs());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Intact transcript update event : [New parent ac = " + newParentAc != null ? newParentAc : "none");
        buffer.append("] \n");

        return buffer.toString();
    }
}
