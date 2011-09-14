package uk.ac.ebi.intact.update.model.protein.events;

import org.apache.commons.collections.CollectionUtils;
import org.hibernate.annotations.Type;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.Xref;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;

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
@DiscriminatorValue("duplicated_protein")
public class DuplicatedProteinEvent extends ProteinEventWithShiftedRanges {

    /**
     * The intact ac of the original protein
     */
    private String originalProtein;

    /**
     * Boolean value to know if the merge was successful
     */
    private boolean wasMergeSuccessful;

    /**
     * The list of interaction acs which have been moved to the original protein
     */
    private Collection<String> movedInteractions = new ArrayList<String>();

    /**
     * The list of intact acs of updated transcripts
     */
    private Collection<String> updatedTranscripts = new ArrayList<String>();

    /**
     * The list of cross reference which have been moved to the updateProcess protein
     */
    private Collection<UpdatedCrossReference> movedXrefs = new ArrayList<UpdatedCrossReference>();

    public DuplicatedProteinEvent(){
        super();
        this.originalProtein = null;
        wasMergeSuccessful = false;
    }

    public DuplicatedProteinEvent(ProteinUpdateProcess updateProcess, Protein duplicatedProtein, Protein originalProtein, String uniprotAc, boolean wasMergeSuccessful){
        super(updateProcess, duplicatedProtein, uniprotAc);
        this.originalProtein = originalProtein != null ? originalProtein.getAc() : null;
        this.wasMergeSuccessful = wasMergeSuccessful;
    }

    @Column(name="original_protein")
    public String getOriginalProtein() {
        return originalProtein;
    }

    public void setOriginalProtein(String originalProtein) {
        this.originalProtein = originalProtein;
    }

    @Column(name = "merged")
    @Type(type="yes_no")
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

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedCrossReference> getMovedXrefs() {
        return this.movedXrefs;
    }

    public void addMovedReferencesFromXref(Collection<InteractorXref> updatedRef){
        for (Xref ref : updatedRef){

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, UpdateStatus.deleted);
            if (this.movedXrefs.add(reference)){
                reference.setParent(this);
            }
        }
    }

    public void setMovedXrefs(Collection<UpdatedCrossReference> updatedReferences) {
        if (updatedReferences != null){
            this.movedXrefs = updatedReferences;
        }
    }

    public boolean addMovedXRef(UpdatedCrossReference xref){
        if (this.movedXrefs.add(xref)){
            xref.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeMovedXRef(UpdatedCrossReference xref){
        if (this.movedXrefs.remove(xref)){
            xref.setParent(null);
            return true;
        }

        return false;
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

        if (isMergeSuccessful() != event.isMergeSuccessful()){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(this.movedInteractions, event.getMovedInteractions())){
            return false;
        }

        if (!CollectionUtils.isEqualCollection(this.movedXrefs, event.getMovedXrefs())){
            return false;
        }

        return CollectionUtils.isEqualCollection(this.updatedTranscripts, event.getUpdatedTranscripts());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Duplicate event : [Original proteinAc = " + originalProtein != null ? originalProtein : "none");
        buffer.append("merge successful = "+isMergeSuccessful()+"] \n");

        return buffer.toString();
    }
}
