package uk.ac.ebi.intact.update.model.protein.events;

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
 * Event for dead proteins in uniprot
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/07/11</pre>
 */
@Entity
@DiscriminatorValue("dead_protein")
public class DeadProteinEvent extends PersistentProteinEvent {

    /**
     * The collection of cross references which have been deleted while processing the dead protein
     */
    private Collection<UpdatedCrossReference> deletedXrefs = new ArrayList<UpdatedCrossReference>();

    public DeadProteinEvent() {
        super();
    }

    public DeadProteinEvent(ProteinUpdateProcess process, Protein protein) {
        super(process, protein);
    }

    public DeadProteinEvent(ProteinUpdateProcess process, Protein protein, String uniprotAc) {
        super(process, protein, uniprotAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public DeadProteinEvent(ProteinUpdateProcess process, String proteinAc) {
        super(process, proteinAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    public DeadProteinEvent(ProteinUpdateProcess process, String proteinAc, String uniprotAc) {
        super(process, proteinAc, uniprotAc);    //To change body of overridden methods use File | Settings | File Templates.
    }

    @OneToMany(mappedBy = "parent", orphanRemoval = true, cascade = {CascadeType.PERSIST, CascadeType.REMOVE, CascadeType.MERGE, CascadeType.REFRESH} )
    public Collection<UpdatedCrossReference> getDeletedXrefs() {
        return this.deletedXrefs;
    }

    public void addDeletedReferencesFromXref(Collection<InteractorXref> updatedRef){
        for (Xref ref : updatedRef){

            UpdatedCrossReference reference = new UpdatedCrossReference(ref, UpdateStatus.deleted);
            if (this.deletedXrefs.add(reference)){
                reference.setParent(this);
            }
        }
    }

    public void setDeletedXrefs(Collection<UpdatedCrossReference> updatedReferences) {
        if (updatedReferences != null){
            this.deletedXrefs = updatedReferences;
        }
    }

    public boolean addDeletedXRef(UpdatedCrossReference xref){
        if (this.deletedXrefs.add(xref)){
            xref.setParent(this);
            return true;
        }

        return false;
    }

    public boolean removeDeletedXRef(UpdatedCrossReference xref){
        if (this.deletedXrefs.remove(xref)){
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

        return CollectionUtils.isEqualCollection(deletedXrefs, event.getDeletedXrefs());
    }
}
