package uk.ac.ebi.intact.update.model.protein.events;

import org.apache.commons.collections.CollectionUtils;
import uk.ac.ebi.intact.model.Protein;
import uk.ac.ebi.intact.update.model.protein.ProteinUpdateProcess;

import javax.persistence.*;
import java.util.ArrayList;
import java.util.Collection;

/**
 * Event for deleted components
 *
 * @author Marine Dumousseau (marine@ebi.ac.uk)
 * @version $Id$
 * @since <pre>27/06/11</pre>
 */
@Entity
@DiscriminatorValue("deleted_component")
public class DeletedComponentEvent extends PersistentProteinEvent {

    /**
     * collection of intact ac of components which have been deleted
     */
    private Collection<String> deletedComponents  = new ArrayList<String>();

    public DeletedComponentEvent(){
        super();
    }

    public DeletedComponentEvent(ProteinUpdateProcess updateProcess, Protein protein, String uniprot){
        super(updateProcess, protein, uniprot);
    }

    @ElementCollection
    @CollectionTable(name="ia_deleted_comp", joinColumns=@JoinColumn(name="event_id"))
    @Column(name="component_ac")
    public Collection<String> getDeletedComponents() {
        return deletedComponents;
    }

    public void setDeletedComponents(Collection<String> deletedComponents) {
        if (deletedComponents != null){
            this.deletedComponents = deletedComponents;
        }
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

        return code;
    }

    @Override
    public boolean isIdenticalTo(Object o){

        if (!super.isIdenticalTo(o)){
            return false;
        }

        final DeletedComponentEvent event = ( DeletedComponentEvent ) o;

        return CollectionUtils.isEqualCollection(this.deletedComponents, event.getDeletedComponents());
    }

    @Override
    public String toString() {
        StringBuffer buffer = new StringBuffer();

        buffer.append(super.toString() + "\n");

        buffer.append("Deleted component event : [proteinAc = " + super.getProteinAc() + ", Number deleted components = " + this.deletedComponents.size());

        return buffer.toString();
    }
}
