package uk.ac.ebi.intact.update.model.protein.update.events;

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
@DiscriminatorValue("IntactTranscriptUpdateEvent")
public class IntactTranscriptUpdateEvent extends ProteinEventWithMessage{

    Collection<String> oldParentAcs;
    String newParentAc;

    String message;

    public IntactTranscriptUpdateEvent(){
        super();
    }

    public IntactTranscriptUpdateEvent(UpdateProcess updateProcess, Protein transcript, String newParentAc, String message, int index){
        super(updateProcess, EventName.transcript_parent_update, transcript, message, index);
        this.newParentAc = newParentAc;
        this.message = message;
        this.oldParentAcs = new ArrayList<String>();
    }

    @ElementCollection
    @JoinTable(name = "ia_event2old_parent", joinColumns = @JoinColumn(name="event_id"))
    @Column(name = "old_parent", nullable = true)
    public Collection<String> getOldParentAcs() {
        return oldParentAcs;
    }

    public void setOldParentAcs(Collection<String> oldParentAcs) {
        this.oldParentAcs = oldParentAcs;
    }

    @Column(name = "new_parent", nullable = true)
    public String getNewParentAc() {
        return newParentAc;
    }

    public void setNewParentAc(String newParentAc) {
        this.newParentAc = newParentAc;
    }
}
