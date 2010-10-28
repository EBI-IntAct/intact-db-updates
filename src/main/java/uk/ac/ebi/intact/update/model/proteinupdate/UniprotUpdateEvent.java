package uk.ac.ebi.intact.update.model.proteinupdate;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.CrossReference;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.IntactProtein;
import uk.ac.ebi.intact.update.model.proteinupdate.protein.UpdateError;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.OneToMany;
import java.util.*;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>28-Oct-2010</pre>
 */

public class UniprotUpdateEvent extends ProteinEvent{

    private Collection<CrossReference> deletedReferences;
    private Collection<CrossReference> createdReferences;
    private Collection<String> messages = new ArrayList<String>();
    private Collection<UpdateError> errors = new ArrayList<UpdateError>();

    public UniprotUpdateEvent(){
        super();
        deletedReferences = new ArrayList<CrossReference>();
        createdReferences = new ArrayList<CrossReference>();
    }

    public UniprotUpdateEvent(Collection<InteractorXref> deletedRefs, Collection<InteractorXref> addedRefs, IntactProtein intactProtein, Date created){
        super();
        setCreated(created);
        setDeletedReferencesFromInteractor(deletedRefs, intactProtein);
        setCreatedReferencesFromInteractor(addedRefs);
    }

    @OneToMany
    @JoinTable(
            name = "ia_deadprotein2deletedxref",
            joinColumns = {@JoinColumn( name = "dead_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "deleted_xref_id" )}
    )
    public Collection<CrossReference> getDeletedReferences() {
        return deletedReferences;
    }

    public void setDeletedReferences(Collection<CrossReference> deletedReferences) {
        this.deletedReferences = deletedReferences;
    }

    public void setDeletedReferencesFromInteractor(Collection<InteractorXref> deletedReferences, IntactProtein intactProtein){
        for (InteractorXref ref : deletedReferences){
            String refAc = ref.getAc();

            for (CrossReference crossRef : intactProtein.getCrossReferences()){
                if (crossRef.getXRefAc().equalsIgnoreCase(refAc)){
                    this.deletedReferences.add(crossRef);
                    break;
                }
            }
        }
    }

    @OneToMany
    @JoinTable(
            name = "ia_deadprotein2createdxref",
            joinColumns = {@JoinColumn( name = "dead_event_id" )},
            inverseJoinColumns = {@JoinColumn( name = "created_xref_id" )}
    )
    public Collection<CrossReference> getCreatedReferences() {
        return createdReferences;
    }

    public void setCreatedReferences(Collection<CrossReference> createdReferences) {
        this.createdReferences = createdReferences;
    }

    public void setCreatedReferencesFromInteractor(Collection<InteractorXref> addedReferences){
        for (InteractorXref ref : addedReferences){
            CrossReference crossRef = new CrossReference(ref, getCreated());
            this.createdReferences.add(crossRef);
        }
    }
}
