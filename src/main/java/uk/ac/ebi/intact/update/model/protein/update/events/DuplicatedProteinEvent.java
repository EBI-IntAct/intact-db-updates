package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.DiscriminatorFormula;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.update.ProteinEventName;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event for duplicated proteins
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>25-Nov-2010</pre>
 */
@Entity
@DiscriminatorFormula("objclass")
@DiscriminatorValue("DuplicatedProteinEvent")
public class DuplicatedProteinEvent extends ProteinEventWithRangeUpdate {

    private String originalProtein;
    private boolean neededSequenceUpdate;
    private boolean wasMergeSuccessful;

    private Collection<String> movedInteractions;

    private Collection<String> updatedTranscripts;

    public DuplicatedProteinEvent(){
        super();
        this.originalProtein = null;
        neededSequenceUpdate = false;
        wasMergeSuccessful = false;

        movedInteractions = new ArrayList<String>();
        updatedTranscripts = new ArrayList<String>();
    }

    public DuplicatedProteinEvent(ProteinUpdateProcess updateProcess, Protein duplicatedProtein, Protein originalProtein, boolean neededSequenceUpdate, boolean wasMergeSuccessful){
        super(updateProcess, ProteinEventName.protein_duplicate, duplicatedProtein);
        this.originalProtein = originalProtein != null ? originalProtein.getAc() : null;
        this.neededSequenceUpdate = neededSequenceUpdate;
        this.wasMergeSuccessful = wasMergeSuccessful;

        movedInteractions = new ArrayList<String>();
        updatedTranscripts = new ArrayList<String>();
    }

    @Column(name="original_protein_ac")
    public String getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(String originalProtein) {
        this.originalProtein = originalProtein;
    }

    @Column(name = "updated_sequence")
    public boolean isSequenceUpdate() {
        return neededSequenceUpdate;
    }

    public void setSequenceUpdate(boolean neededSequenceUpdate) {
        this.neededSequenceUpdate = neededSequenceUpdate;
    }

    @Column(name = "merge")
    public boolean isMergeSuccessful() {
        return wasMergeSuccessful;
    }

    public void setMergeSuccessful(boolean wasMergeSuccessful) {
        this.wasMergeSuccessful = wasMergeSuccessful;
    }

    @ElementCollection
    @CollectionTable(name="ia_moved_interactions", joinColumns=@JoinColumn(name="event_id"))
    @Column(name="interaction_ac")
    public Collection<String> getMovedInteractions() {
        return movedInteractions;
    }

    public void setMovedInteractions(Collection<String> movedInteractions) {
        if (movedInteractions != null){
            this.movedInteractions = movedInteractions;
        }
    }

    @ElementCollection
    @CollectionTable(name="ia_updated_transcripts", joinColumns=@JoinColumn(name="event_id"))
    @Column(name="transcript_ac")
    public Collection<String> getUpdatedTranscripts() {
        return updatedTranscripts;
    }

    public void setUpdatedTranscripts(Collection<String> updatedTranscripts) {
        if (updatedTranscripts != null){
            this.updatedTranscripts = updatedTranscripts;
        }
    }

    @Override
    public boolean equals( Object o ) {
        if ( !super.equals(o) ) {
            return false;
        }

        final DuplicatedProteinEvent event = ( DuplicatedProteinEvent ) o;

        if ( originalProtein != null ) {
            if (!originalProtein.equals( event.getOriginalProtein() )){
                return false;
            }
        }
        else if (event.getOriginalProtein()!= null){
            return false;
        }

        if (neededSequenceUpdate != event.isSequenceUpdate()){
            return false;
        }

        if (isMergeSuccessful() != event.isMergeSuccessful()){
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

        if ( originalProtein != null ) {
            code = 29 * code + originalProtein.hashCode();
        }

        code = 29 * code + Boolean.toString(isSequenceUpdate()).hashCode();
        code = 29 * code + Boolean.toString(isMergeSuccessful()).hashCode();

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final DuplicatedProteinEvent event = ( DuplicatedProteinEvent ) o;

        if ( originalProtein != null ) {
            if (!originalProtein.equals( event.getOriginalProtein() )){
                return false;
            }
        }
        else if (event.getOriginalProtein()!= null){
            return false;
        }

        if (neededSequenceUpdate != event.isSequenceUpdate()){
            return false;
        }

        if (isMergeSuccessful() != event.isMergeSuccessful()){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(this.movedInteractions, event.getMovedInteractions())){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.updatedTranscripts, event.getUpdatedTranscripts());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Duplicate event : [Original proteinAc = " + originalProtein != null ? originalProtein : "none");
        buffer.append("sequence update = "+isSequenceUpdate()+", merge successful = "+isMergeSuccessful()+"] \n");

        return buffer.toString();
    }
}
