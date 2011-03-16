package uk.ac.ebi.intact.update.model.protein.update.events;

import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.update.UpdateProcess;

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
@DiscriminatorValue("DuplicatedProteinEvent")
public class DuplicatedProteinEvent extends ProteinEvent{

    private String originalProtein;
    private boolean neededSequenceUpdate;
    private boolean wasMergeSuccessful;

    private Collection<String> movedInteractions;

    private Collection<String> deletedComponents;

    public DuplicatedProteinEvent(){
        super();
        this.originalProtein = null;
        neededSequenceUpdate = false;
        wasMergeSuccessful = false;

        movedInteractions = new ArrayList<String>();
        deletedComponents = new ArrayList<String>();
    }

    public DuplicatedProteinEvent(UpdateProcess updateProcess, Protein duplicatedProtein, int index, Protein originalProtein, boolean neededSequenceUpdate, boolean wasMergeSuccessful){
        super(updateProcess, EventName.protein_duplicate, duplicatedProtein, index);
        this.originalProtein = originalProtein != null ? originalProtein.getAc() : null;
        this.neededSequenceUpdate = neededSequenceUpdate;
        this.wasMergeSuccessful = wasMergeSuccessful;

        movedInteractions = new ArrayList<String>();
        deletedComponents = new ArrayList<String>();
    }

    @Column(name="original_protein_ac", nullable = false)
    public String getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(String originalProtein) {
        this.originalProtein = originalProtein;
    }

    @Column(name = "updated_sequence", nullable = false)
    public boolean isNeededSequenceUpdate() {
        return neededSequenceUpdate;
    }

    public void setNeededSequenceUpdate(boolean neededSequenceUpdate) {
        this.neededSequenceUpdate = neededSequenceUpdate;
    }

    @Column(name = "merge", nullable = false)
    public boolean isWasMergeSuccessful() {
        return wasMergeSuccessful;
    }

    public void setWasMergeSuccessful(boolean wasMergeSuccessful) {
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
    @CollectionTable(name="ia_duplicated_components", joinColumns=@JoinColumn(name="event_id"))
    @Column(name="component_ac")
    public Collection<String> getDeletedComponents() {
        return deletedComponents;
    }

    public void setDeletedComponents(Collection<String> deletedComponents) {
        if (deletedComponents != null){
            this.deletedComponents = deletedComponents;
        }
    }
}
