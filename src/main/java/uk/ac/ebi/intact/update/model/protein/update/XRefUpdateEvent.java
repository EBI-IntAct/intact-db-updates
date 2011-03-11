package uk.ac.ebi.intact.update.model.protein.update;

import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.update.model.protein.update.protein.CrossReference;
import uk.ac.ebi.intact.update.model.protein.update.protein.IntactProtein;

import javax.persistence.JoinColumn;
import javax.persistence.JoinTable;
import javax.persistence.MappedSuperclass;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;
import java.util.Date;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>29-Oct-2010</pre>
 */
@MappedSuperclass
public abstract class XRefUpdateEvent extends ProteinEvent{

    private Collection<CrossReference> deletedReferences;
    private Collection<CrossReference> createdReferences;

    public XRefUpdateEvent(){
        super();
        deletedReferences = new ArrayList<CrossReference>();
        createdReferences = new ArrayList<CrossReference>();
    }

    public XRefUpdateEvent(Collection<InteractorXref> deletedRefs, Collection<InteractorXref> addedRefs, IntactProtein intactProtein, EventName name, Date created, int index){
        super(name, created, index);
        setDeletedReferencesFromInteractor(deletedRefs, intactProtein);
        setCreatedReferencesFromInteractor(addedRefs);
    }

    @OneToMany
    @JoinTable(
            name = "ia_event2deletedxref",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
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
            name = "ia_event2createdxref",
            joinColumns = {@JoinColumn( name = "protein_event_id" )},
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
