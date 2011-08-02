package uk.ac.ebi.intact.update.model.protein.update.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.InteractorXref;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.model.Xref;
import uk.ac.ebi.intact.update.model.UpdateStatus;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;
import uk.ac.ebi.intact.update.model.protein.UpdatedCrossReference;

import javax.persistence.CascadeType;
import javax.persistence.DiscriminatorValue;
import javax.persistence.Entity;
import javax.persistence.OneToMany;
import java.util.ArrayList;
import java.util.Collection;

/**
 * TODO comment this
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/07/11</pre>
 */
@Entity
@DiscriminatorValue("DeadProteinEvent")
public class DeadProteinEvent extends PersistentProteinEvent {

    private Collection<UpdatedCrossReference> deletedReferences = new ArrayList<UpdatedCrossReference>();

    public DeadProteinEvent() {
        super();
    }

    public DeadProteinEvent(ProteinUpdateProcess process, Protein protein) {
        super(process, ProteinEventName.dead_protein, protein);
    }

    public DeadProteinEvent(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, ProteinEventName.dead_protein, protein, uniprotAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public DeadProteinEvent(ProteinUpdateProcess process, String proteinAc) {
        super(process, ProteinEventName.dead_protein, proteinAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public DeadProteinEvent(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, ProteinEventName.dead_protein, proteinAc, uniprotAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedCrossReference> getDeletedReferences() {
        return this.deletedReferences;
    }

    public void addDeletedReferencesFromXref(Collection<InteractorXref> updatedRef){
        for (Xref ref : updatedRef){

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, UpdateStatus.deleted);
            if (this.deletedReferences.add(reference)){
                reference.setParent(this);
            }
        }
    }

    public void setDeletedReferences(Collection<UpdatedCrossReference> updatedReferences) {
        if (updatedReferences != null){
            this.deletedReferences = updatedReferences;
        }
    }

    public boolean addDeletedXRef(UpdatedCrossReference xref){
        if (this.deletedReferences.add(xref)){
            xref.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeDeletedXRef(UpdatedCrossReference xref){
        if (this.deletedReferences.remove(xref)){
            xref.setParent(null);
            return true;
        }

        return false;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final DeadProteinEvent event = (DeadProteinEvent) o;

        return CollectionUtils.isEqualCollection(deletedReferences, event.getDeletedReferences());
    }
}
