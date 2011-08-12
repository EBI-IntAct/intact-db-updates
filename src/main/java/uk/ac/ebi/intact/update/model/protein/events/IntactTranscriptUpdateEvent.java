package uk.ac.ebi.intact.update.model.protein.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.Column;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;

/**
 * Event for isoforms and feature chains with necessity to update the updateProcess cross reference
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>11/03/11</pre>
 */
@Entity
@DiscriminatorValue("transcript_update")
public class IntactTranscriptUpdateEvent extends PersistentProteinEvent{

    /**
     * The old updateProcess ac which has been replaced
     */
    private String oldParentAc;

    /**
     * The new updateProcess ac
     */
    private String newParentAc;

    public IntactTranscriptUpdateEvent(){
        super();
        this.newParentAc = null;
        this.oldParentAc = null;
    }

    public IntactTranscriptUpdateEvent(ProteinUpdateProcess updateProcess, Protein transcript, String uniprotAc, String oldParentAc, String newParentAc ){
        super(updateProcess, transcript, uniprotAc);
        this.newParentAc = newParentAc;
        this.oldParentAc = oldParentAc;
    }

    @Column(name = "old_parent")
    public String getOldParentAc() {
        return oldParentAc;
    }

    public void setOldParentAc(String oldParentAc) {
        this.oldParentAc = oldParentAc;
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

        if ( oldParentAc != null ) {
            if (!oldParentAc.equals( event.getOldParentAc() )){
                return false;
            }
        }
        else if (event.getOldParentAc()!= null){
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

        if ( oldParentAc != null ) {
            code = 29 * code + oldParentAc.hashCode();
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

        if ( oldParentAc != null ) {
            if (!oldParentAc.equals( event.getOldParentAc() )){
                return false;
            }
        }
        else if (event.getOldParentAc()!= null){
            return false;
        }

        return true;
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Intact transcript update event : [New updateProcess ac = " + (newParentAc != null ? newParentAc : "none") + "Old updateProcess ac = " + (oldParentAc != null ? oldParentAc : "none"));
        buffer.append("] \n");

        return buffer.toString();
    }
}
